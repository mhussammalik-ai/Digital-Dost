package com.digitaldost.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.digitaldost.data.Lang
import com.digitaldost.data.Prefs
import com.digitaldost.engine.CommandRouter
import com.digitaldost.voice.VoiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Floating "DD" bubble drawn over other apps. Tap it to open a small panel (type or tap the mic).
 * Built with classic Views because Compose inside a Service needs extra lifecycle plumbing.
 */
class FloatingAssistantService : Service(), VoiceController.Listener {

    companion object {
        const val ACTION_STOP = "com.digitaldost.STOP_FLOATING"
        private const val CHANNEL = "dd_floating"
        private const val NOTIF_ID = 1
        private const val GREEN = 0xFF0B5D4B.toInt()
    }

    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private lateinit var router: CommandRouter
    private lateinit var voice: VoiceController
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var bubble: View? = null
    private var panel: View? = null
    private var replyView: TextView? = null
    private var inputView: EditText? = null
    private var micButton: Button? = null
    private var listening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        router = CommandRouter(this, prefs)
        voice = VoiceController(this, prefs, this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            prefs.floatingEnabled = false
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        showBubble()
        return START_STICKY
    }

    override fun onDestroy() {
        bubble?.let { runCatching { wm.removeView(it) } }
        panel?.let { runCatching { wm.removeView(it) } }
        bubble = null; panel = null
        voice.shutdown()
        scope.cancel()
        super.onDestroy()
    }

    // ---------- foreground notification ----------

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Digital Dost assistant", NotificationManager.IMPORTANCE_LOW)
        )
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingAssistantService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val action = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Turn off", stopPi
        ).build()
        val n = Notification.Builder(this, CHANNEL)
            .setContentTitle("Digital Dost is ready")
            .setContentText("Tap the floating button to talk. I only listen after you tap the mic.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .addAction(action)
            .setOngoing(true)
            .build()
        when {
            Build.VERSION.SDK_INT >= 34 -> startForeground(
                NOTIF_ID, n,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
            Build.VERSION.SDK_INT >= 30 -> startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else -> startForeground(NOTIF_ID, n)
        }
    }

    // ---------- bubble ----------

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun showBubble() {
        if (bubble != null) return
        val v = TextView(this).apply {
            text = "DD"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            contentDescription = "Digital Dost assistant. Tap to open."
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(GREEN)
                setStroke(dp(3), Color.WHITE)
            }
            elevation = dp(6).toFloat()
        }
        val lp = WindowManager.LayoutParams(
            dp(60), dp(60),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(12); y = dp(200) }
        v.setOnTouchListener(DragTouch(lp, v))
        wm.addView(v, lp)
        bubble = v
    }

    private inner class DragTouch(val lp: WindowManager.LayoutParams, val view: View) : View.OnTouchListener {
        private var startX = 0; private var startY = 0
        private var touchX = 0f; private var touchY = 0f
        private var moved = false
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y; touchX = e.rawX; touchY = e.rawY; moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt(); val dy = (e.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    if (moved) { lp.x = startX + dx; lp.y = startY + dy; wm.updateViewLayout(view, lp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) openPanel()
            }
            return true
        }
    }

    // ---------- panel ----------

    private fun hello() = when (prefs.lang) {
        Lang.EN -> "Hello! Type or tap Speak. For example: open WhatsApp."
        Lang.UR -> "السلام علیکم! لکھیں یا بولیں۔ مثلاً: واٹس ایپ کھولو۔"
        Lang.RO -> "Assalam o Alaikum! Likhein ya bolein. Misaal: WhatsApp kholo."
    }

    private fun smallButton(label: String, desc: String, onClick: () -> Unit) = Button(this).apply {
        text = label; textSize = 18f; minimumWidth = dp(52); contentDescription = desc
        setOnClickListener { onClick() }
    }

    private fun openPanel() {
        if (panel != null) return
        bubble?.visibility = View.GONE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(20).toFloat(); setStroke(dp(2), GREEN)
            }
            elevation = dp(8).toFloat()
        }

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = if (prefs.onlineMode) "Digital Dost · Online AI" else "Digital Dost · Local Privacy"
            textSize = 17f; setTextColor(GREEN); setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        header.addView(smallButton("—", "Minimise") { closePanel() })
        header.addView(smallButton("✕", "Close assistant") { prefs.floatingEnabled = false; stopSelf() })
        root.addView(header)

        val reply = TextView(this).apply {
            textSize = 18f; setTextColor(Color.BLACK); setPadding(0, dp(8), 0, dp(8)); text = hello()
        }
        replyView = reply
        root.addView(ScrollView(this).apply { addView(reply) }, LinearLayout.LayoutParams(MATCH_PARENT, dp(140)))

        val input = EditText(this).apply {
            hint = "Type here…"; textSize = 18f; setSingleLine(); imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) { submit(text.toString(), false); true } else false
            }
        }
        inputView = input
        root.addView(input, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val mic = Button(this).apply {
            text = "🎤 Speak"; textSize = 18f; minimumHeight = dp(56)
            setOnClickListener { toggleMic() }
        }
        micButton = mic
        val send = Button(this).apply {
            text = "Send"; textSize = 18f; minimumHeight = dp(56)
            setOnClickListener { submit(input.text.toString(), false) }
        }
        row.addView(mic, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(send, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        root.addView(row)

        val lp = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.94).toInt(), WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,   // focusable so the keyboard works
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(60)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        wm.addView(root, lp)
        panel = root
    }

    private fun closePanel() {
        if (listening) voice.stopListening()
        voice.stopSpeaking()
        listening = false
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null; replyView = null; inputView = null; micButton = null
        bubble?.visibility = View.VISIBLE
    }

    private fun toggleMic() {
        if (listening) voice.stopListening() else voice.startListening()
    }

    private fun submit(text: String, viaVoice: Boolean) {
        if (text.isBlank()) return
        inputView?.setText("")
        replyView?.text = "…"
        scope.launch {
            val r = router.handle(text)
            replyView?.text = r.text
            if (viaVoice) voice.speak(r.text)
            if (r.launched) closePanel()   // get out of the way once an app has been opened
        }
    }

    // ---------- VoiceController.Listener ----------

    override fun onListening() {
        listening = true; micButton?.text = "⏹ Stop"; replyView?.text = "Listening…"
    }
    override fun onPartial(text: String) { inputView?.setText(text) }
    override fun onFinal(text: String) {
        listening = false; micButton?.text = "🎤 Speak"; submit(text, true)
    }
    override fun onError(message: String) {
        listening = false; micButton?.text = "🎤 Speak"; replyView?.text = message
    }
}
