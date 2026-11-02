package com.digitaldost

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.digitaldost.overlay.FloatingAssistantService
import com.digitaldost.ui.ChatScreen
import com.digitaldost.ui.ChatViewModel
import com.digitaldost.ui.DigitalDostTheme
import com.digitaldost.ui.PrivacyScreen

class MainActivity : ComponentActivity() {

    private val vm: ChatViewModel by viewModels()
    private var tick by mutableIntStateOf(0)           // bumped on resume to refresh permission status
    private var rationale by mutableStateOf<Rationale?>(null)
    private var micCallback: (() -> Unit)? = null

    private class Rationale(val title: String, val body: String, val onOk: () -> Unit, val onCancel: () -> Unit = {})

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            tick++
            if (hasMic()) micCallback?.invoke()
            micCallback = null
        }

    private fun hasMic() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun canOverlay() = Settings.canDrawOverlays(this)

    private fun requestMic(onGranted: () -> Unit) {
        if (hasMic()) { onGranted(); return }
        micCallback = onGranted
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        permLauncher.launch(perms.toTypedArray())
    }

    private fun ask(title: String, body: String, onCancel: () -> Unit = {}, onOk: () -> Unit) {
        rationale = Rationale(title, body, onOk, onCancel)
    }

    private fun onMicClick() {
        if (hasMic()) vm.toggleMic()
        else ask(
            "Microphone",
            "Digital Dost uses the microphone only while you tap the mic button and speak. It never listens in the background."
        ) { requestMic { vm.toggleMic() } }
    }

    private fun onFloatingToggle(on: Boolean) {
        vm.updateFloating(on)
        if (on) ensureFloating() else stopService(Intent(this, FloatingAssistantService::class.java))
    }

    private fun ensureFloating() {
        if (!hasMic()) {
            ask(
                "Microphone and notifications",
                "The floating assistant needs the microphone (used only when you tap the mic) and a notification so you can see it is running.",
                onCancel = { vm.updateFloating(false) }
            ) { requestMic { ensureFloating() } }
            return
        }
        if (!canOverlay()) {
            ask(
                "Display over other apps",
                "To show the floating button above other apps, Android needs you to allow \"Display over other apps\" for Digital Dost. You will be taken to a settings page.",
                onCancel = { vm.updateFloating(false) }
            ) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, FloatingAssistantService::class.java))
    }

    override fun onResume() {
        super.onResume()
        tick++
        vm.refreshFromPrefs()
        // Returning from the overlay-permission screen: finish enabling the floating assistant
        if (vm.floatingOn && hasMic() && canOverlay()) ensureFloating()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigitalDostTheme {
                var tab by remember { mutableIntStateOf(0) }
                val currentTick = tick
                val mic = remember(currentTick) { hasMic() }
                val overlay = remember(currentTick) { canOverlay() }

                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                        Box(Modifier.weight(1f)) {
                            if (tab == 0) ChatScreen(vm, onMicClick = ::onMicClick)
                            else PrivacyScreen(vm, mic, overlay, onFloatingToggle = ::onFloatingToggle)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 12.dp,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        ) {
                            NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                                NavigationBarItem(
                                    selected = tab == 0,
                                    onClick = { tab = 0 },
                                    icon = { Text("◉", fontSize = 20.sp) },
                                    label = { Text("Chat") }
                                )
                                NavigationBarItem(
                                    selected = tab == 1,
                                    onClick = { tab = 1 },
                                    icon = { Text("⌁", fontSize = 22.sp) },
                                    label = { Text("Privacy") }
                                )
                            }
                        }
                    }
                }

                rationale?.let { r ->
                    AlertDialog(
                        onDismissRequest = { rationale = null; r.onCancel() },
                        title = { Text(r.title) },
                        text = { Text(r.body, fontSize = 18.sp) },
                        confirmButton = { Button(onClick = { rationale = null; r.onOk() }) { Text("Continue") } },
                        dismissButton = { TextButton(onClick = { rationale = null; r.onCancel() }) { Text("Not now") } }
                    )
                }
            }
        }
    }
}
