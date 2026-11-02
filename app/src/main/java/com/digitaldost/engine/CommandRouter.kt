package com.digitaldost.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.MediaStore
import android.provider.Settings
import com.digitaldost.data.AppDatabase
import com.digitaldost.data.ChatMessage
import com.digitaldost.data.Lang
import com.digitaldost.data.Prefs
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Reply(val text: String, val launched: Boolean = false)

/**
 * Takes raw user text, saves it, decides what to do (rule-based first, LLM only for open questions),
 * performs the Android action through standard intents and returns the reply text.
 * Shared by the main app and the floating overlay.
 */
class CommandRouter(private val ctx: Context, private val prefs: Prefs) {

    private val dao = AppDatabase.get(ctx).chatDao()
    private val ai = OnlineAi(prefs)
    private var awaitingWhatsApp = false

    private fun t(en: String, ur: String, ro: String) = when (prefs.lang) {
        Lang.EN -> en
        Lang.UR -> ur
        Lang.RO -> ro
    }

    suspend fun handle(input: String): Reply {
        val text = input.trim()
        if (text.isEmpty()) return Reply("")
        val history = dao.recent(8).reversed()
        dao.insert(ChatMessage(role = "user", text = text))
        val reply = try {
            route(text, history)
        } catch (e: Exception) {
            Reply(t("Sorry, something went wrong. Please try again.",
                "معذرت، کچھ غلط ہو گیا۔ دوبارہ کوشش کریں۔",
                "Maazrat, kuch ghalat ho gaya. Dobara koshish karein."))
        }
        dao.insert(ChatMessage(role = "assistant", text = reply.text))
        return reply
    }

    private suspend fun route(text: String, history: List<ChatMessage>): Reply {
        if (awaitingWhatsApp) {
            awaitingWhatsApp = false
            return if (CommandParser.isCancel(text))
                Reply(t("Okay, cancelled.", "ٹھیک ہے، منسوخ کر دیا۔", "Theek hai, cancel kar diya."))
            else shareToWhatsApp(text)
        }
        return when (val c = CommandParser.parse(text)) {
            is Command.OpenApp -> openApp(c.app)
            is Command.SearchYouTube -> searchYouTube(c.query)
            is Command.SearchGoogle -> searchGoogle(c.query)
            is Command.SearchMaps -> searchMaps(c.query)
            is Command.OpenSettings -> openSettings(c.page)
            Command.StartWhatsAppMessage -> {
                if (!installed("com.whatsapp") && !installed("com.whatsapp.w4b")) notInstalled("WhatsApp")
                else {
                    awaitingWhatsApp = true
                    Reply(t("What would you like to say?", "آپ کیا پیغام بھیجنا چاہتے ہیں؟", "Aap kya message bhejna chahte hain?"))
                }
            }
            is Command.Offline -> offline(c.kind)
            is Command.ScamCheck -> scamCheck(c.text, history)
            is Command.Unsupported -> Reply(t(
                "I don't know how to open \"${c.name}\" yet. I can open Google, WhatsApp, YouTube, Maps, Gmail, Camera, Gallery and Settings.",
                "میں ابھی \"${c.name}\" کھولنا نہیں جانتا۔ میں گوگل، واٹس ایپ، یوٹیوب، میپس، جی میل، کیمرہ، گیلری اور سیٹنگز کھول سکتا ہوں۔",
                "Main abhi \"${c.name}\" kholna nahi jaanta. Main Google, WhatsApp, YouTube, Maps, Gmail, Camera, Gallery aur Settings khol sakta hoon."))
            is Command.Ask -> askAi(c.text, history)
        }
    }

    // ---------- launching ----------

    private fun launch(i: Intent): Boolean = try {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i); true
    } catch (e: Exception) { false }

    private fun installed(pkg: String) = ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    private fun launchPackage(vararg pkgs: String): Boolean {
        for (p in pkgs) {
            val i = ctx.packageManager.getLaunchIntentForPackage(p)
            if (i != null && launch(i)) return true
        }
        return false
    }

    private fun opening(name: String) =
        Reply(t("Opening $name.", "$name کھول رہا ہوں۔", "$name khol raha hoon."), launched = true)

    private fun notInstalled(name: String) = Reply(t(
        "$name is not installed on your device.",
        "$name آپ کے فون میں انسٹال نہیں ہے۔",
        "$name aap ke phone mein install nahi hai."))

    private fun openApp(app: AppTarget): Reply {
        val ok = when (app) {
            AppTarget.GOOGLE -> launchPackage("com.google.android.googlequicksearchbox") ||
                launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
            AppTarget.WHATSAPP -> launchPackage("com.whatsapp", "com.whatsapp.w4b")
            AppTarget.YOUTUBE -> launchPackage("com.google.android.youtube")
            AppTarget.MAPS -> launchPackage("com.google.android.apps.maps")
            AppTarget.GMAIL -> launchPackage("com.google.android.gm")
            AppTarget.CAMERA -> launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            AppTarget.GALLERY -> launch(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY))
            AppTarget.PHONE -> launch(Intent(Intent.ACTION_DIAL))
            AppTarget.BROWSER -> launch(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER))
        }
        return if (ok) opening(app.label) else notInstalled(app.label)
    }

    private fun searching(where: String, q: String) = Reply(t(
        "Searching $where for \"$q\".",
        "$where پر \"$q\" تلاش کر رہا ہوں۔",
        "$where par \"$q\" dhoond raha hoon."), launched = true)

    private fun searchYouTube(q: String): Reply {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(q)}"))
        if (installed("com.google.android.youtube")) i.setPackage("com.google.android.youtube")
        return if (launch(i)) searching("YouTube", q) else notInstalled("YouTube")
    }

    private fun searchGoogle(q: String): Reply {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}"))
        return if (launch(i)) searching("Google", q) else notInstalled("Browser")
    }

    private fun searchMaps(q: String): Reply {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(q)}"))
        if (installed("com.google.android.apps.maps")) i.setPackage("com.google.android.apps.maps")
        return if (launch(i)) searching("Maps", q) else notInstalled("Google Maps")
    }

    private fun openSettings(page: SettingsPage): Reply {
        val action = when (page) {
            SettingsPage.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SettingsPage.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SettingsPage.SOUND -> Settings.ACTION_SOUND_SETTINGS
            SettingsPage.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SettingsPage.BATTERY_SAVER -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            SettingsPage.MAIN -> Settings.ACTION_SETTINGS
        }
        val ok = launch(Intent(action))
        return if (ok) Reply(t(
            "Opening ${page.label} settings. You can change it there.",
            "${page.label} کی سیٹنگز کھول رہا ہوں۔ وہاں آپ اسے بدل سکتے ہیں۔",
            "${page.label} ki settings khol raha hoon. Wahan aap ise badal sakte hain."), launched = true)
        else Reply(t("I couldn't open that screen. Please open Settings manually.",
            "میں وہ اسکرین نہیں کھول سکا۔ براہ کرم سیٹنگز خود کھولیں۔",
            "Main woh screen nahi khol saka. Meherbani karke Settings khud kholein."))
    }

    private fun shareToWhatsApp(message: String): Reply {
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            if (!installed(pkg)) continue
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                setPackage(pkg)
            }
            // The user chooses the contact and taps Send themselves.
            if (launch(i)) return Reply(t(
                "Your message is ready in WhatsApp. Choose a contact and tap send yourself.",
                "آپ کا پیغام واٹس ایپ میں تیار ہے۔ رابطہ منتخب کریں اور خود بھیجیں۔",
                "Aap ka message WhatsApp mein tayyar hai. Contact chunein aur khud send dabayein."), launched = true)
        }
        return notInstalled("WhatsApp")
    }

    // ---------- offline basics ----------

    private fun offline(kind: OfflineKind): Reply = Reply(when (kind) {
        OfflineKind.TIME -> {
            val s = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
            t("The time is $s.", "اس وقت $s ہوئے ہیں۔", "Abhi $s hue hain.")
        }
        OfflineKind.DATE -> {
            val s = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
            t("Today is $s.", "آج $s ہے۔", "Aaj $s hai.")
        }
        OfflineKind.BATTERY -> {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val p = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            t("Your battery is at $p%.", "آپ کی بیٹری $p% ہے۔", "Aap ki battery $p% hai.")
        }
        OfflineKind.GREETING -> t(
            "Hello! What would you like to do? For example: open WhatsApp.",
            "السلام علیکم! آپ کیا کرنا چاہتے ہیں؟ مثلاً: واٹس ایپ کھولو۔",
            "Assalam o Alaikum! Aap kya karna chahte hain? Misaal: WhatsApp kholo.")
        OfflineKind.HELP -> t(
            "You can say:\n• Open WhatsApp / Google / YouTube\n• Search YouTube for cooking videos\n• Turn on Wi-Fi\n• Send a WhatsApp message\n• What time is it?\n• Is this message a scam? (paste it)",
            "آپ کہہ سکتے ہیں:\n• واٹس ایپ کھولو\n• یوٹیوب پر کھانے کی ویڈیوز تلاش کرو\n• وائی فائی آن کرنا ہے\n• واٹس ایپ پیغام بھیجنا ہے\n• وقت کیا ہوا ہے؟\n• کیا یہ پیغام فراڈ ہے؟ (پیغام پیسٹ کریں)",
            "Aap keh sakte hain:\n• WhatsApp kholo\n• YouTube par cooking videos dhundo\n• Wifi on karna hai\n• WhatsApp message bhejna hai\n• Abhi time kya hua hai?\n• Kya ye message scam hai? (message paste karein)")
    })

    // ---------- AI / safety ----------

    private suspend fun scamCheck(text: String, history: List<ChatMessage>): Reply {
        val local = ScamHeuristics.explain(text)
        if (!prefs.onlineMode || prefs.apiKey.isBlank())
            return Reply(local + "\n\n" + t(
                "(Turn on Online AI Mode in Privacy settings for a fuller explanation.)",
                "(مزید وضاحت کے لیے پرائیویسی سیٹنگز میں آن لائن اے آئی موڈ آن کریں۔)",
                "(Zyada wazahat ke liye Privacy settings mein Online AI Mode on karein.)"))
        val extra = "The user pasted a message they find suspicious. Explain in simple words why it may be a scam or what to check. Do not call it safe."
        return ai.ask(history, text, extra).fold(
            { Reply(local + "\n\n" + it) },
            { Reply(local) })
    }

    private suspend fun askAi(text: String, history: List<ChatMessage>): Reply {
        if (!prefs.onlineMode) return Reply(t(
            "I'm in Local Privacy Mode, so I can only do basic commands like opening apps. Turn on Online AI Mode in Privacy settings to ask open questions.",
            "میں لوکل پرائیویسی موڈ میں ہوں، اس لیے صرف بنیادی کمانڈز جیسے ایپس کھولنا کر سکتا ہوں۔ سوالات کے لیے پرائیویسی سیٹنگز میں آن لائن اے آئی موڈ آن کریں۔",
            "Main Local Privacy Mode mein hoon, is liye sirf basic commands (jaise apps kholna) kar sakta hoon. Sawalat ke liye Privacy settings mein Online AI Mode on karein."))
        if (prefs.apiKey.isBlank()) return Reply(t(
            "Online AI Mode is on, but no API key is saved yet. Add it in Privacy settings.",
            "آن لائن اے آئی موڈ آن ہے، لیکن ابھی API کی محفوظ نہیں ہے۔ پرائیویسی سیٹنگز میں شامل کریں۔",
            "Online AI Mode on hai, lekin abhi API key save nahi hai. Privacy settings mein add karein."))
        return ai.ask(history, text).fold(
            { Reply(it) },
            { Reply(t(
                "I couldn't reach the AI service. Please check your internet and key, or try a basic command.",
                "میں اے آئی سروس تک نہیں پہنچ سکا۔ انٹرنیٹ اور کی چیک کریں، یا بنیادی کمانڈ آزمائیں۔",
                "Main AI service tak nahi pahunch saka. Internet aur key check karein, ya basic command try karein.")) })
    }
}
