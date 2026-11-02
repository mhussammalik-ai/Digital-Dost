package com.digitaldost.engine

/**
 * Deterministic parser for English, Urdu script and Roman Urdu.
 * Pure Kotlin (no Android imports) so it can be unit-tested on the JVM.
 * Used for all device actions so the demo never depends on an LLM response.
 */
enum class AppTarget(val label: String, val aliases: List<String>) {
    GOOGLE("Google", listOf("google", "gogle", "goggle", "google search", "گوگل")),
    WHATSAPP("WhatsApp", listOf("whatsapp", "whats app", "watsapp", "whatsap", "whatsup", "واٹس ایپ", "واٹس اپ", "واٹسایپ", "وٹس ایپ", "وٹس اپ", "وٹسایپ")),
    YOUTUBE("YouTube", listOf("youtube", "you tube", "yutub", "utub", "یوٹیوب")),
    MAPS("Google Maps", listOf("google maps", "maps", "map", "naqsha", "نقشہ", "میپس")),
    GMAIL("Gmail", listOf("gmail", "g mail", "email", "جی میل", "ای میل")),
    CAMERA("Camera", listOf("camera", "kemra", "کیمرہ", "کیمرا")),
    GALLERY("Gallery", listOf("gallery", "photos", "tasveerein", "گیلری", "تصاویر")),
    PHONE("Phone", listOf("dialer", "phone app", "ڈائلر", "فون ایپ")),
    BROWSER("Browser", listOf("browser", "chrome", "براؤزر"))
}

enum class SettingsPage(val label: String, val aliases: List<String>) {
    WIFI("Wi-Fi", listOf("wifi", "wi fi", "وائی فائی", "وائے فائی", "وائفائی")),
    BLUETOOTH("Bluetooth", listOf("bluetooth", "blue tooth", "بلوٹوتھ", "بلو ٹوتھ")),
    SOUND("Sound", listOf("volume", "sound", "awaz", "آواز", "والیوم")),
    DISPLAY("Display", listOf("brightness", "display", "roshni", "روشنی", "ڈسپلے")),
    BATTERY_SAVER("Battery saver", listOf("battery saver", "power saving", "بیٹری سیور")),
    MAIN("Settings", emptyList())
}

enum class OfflineKind { TIME, DATE, BATTERY, HELP, GREETING }

sealed interface Command {
    data class OpenApp(val app: AppTarget) : Command
    data class SearchYouTube(val query: String) : Command
    data class SearchGoogle(val query: String) : Command
    data class SearchMaps(val query: String) : Command
    data class OpenSettings(val page: SettingsPage) : Command
    data object StartWhatsAppMessage : Command
    data class Offline(val kind: OfflineKind) : Command
    data class ScamCheck(val text: String) : Command
    data class Unsupported(val name: String) : Command
    data class Ask(val text: String) : Command
}

object CommandParser {

    private val OPEN_WORDS = setOf(
        "open", "launch", "kholo", "khol", "kholein", "kholen", "kholna", "kholiye", "kholiyay",
        "chalao", "chalana", "chalain", "chalaen",
        "کھولو", "کھولیں", "کھولئے", "کھولیے", "کھول", "کھولنا", "چلاؤ", "چلائیں", "چلانا"
    )
    private val SEARCH_WORDS = setOf(
        "search", "find", "lookup", "play", "dhundo", "dhoondo", "dhundna", "dhoondna", "dhundein", "dhoondein",
        "talash", "talaash", "sunao", "lagao",
        "تلاش", "ڈھونڈو", "ڈھونڈیں", "ڈھونڈنا", "سرچ", "سناؤ", "سنائو", "لگاؤ"
    )
    private val FILLER = setOf(
        "par", "pe", "per", "pay", "mein", "me", "main", "on", "in", "for", "at", "ka", "ki", "ke", "ko", "se",
        "hai", "hain", "ho", "do", "de", "dena", "dijiye", "kijiye", "karo", "kro", "karna", "kar", "kr", "please", "pls", "plz",
        "mujhe", "mjhe", "mera", "meri", "mere", "ap", "aap", "the", "a", "an", "to", "and", "aur", "i", "want", "wanna", "need",
        "my", "let", "us", "can", "you", "could", "would", "like", "go", "watch", "show", "tell", "about", "zara", "jara", "ek",
        "dekhna", "dekhni", "dekho", "dekhein", "dekhen", "dekh", "help", "madad",
        "پر", "میں", "کا", "کی", "کے", "کو", "سے", "ہے", "ہیں", "دو", "کرو", "کریں", "مجھے", "مہربانی", "ذرا", "ایک",
        "دیکھنا", "دیکھو", "دیکھنی", "اور"
    )
    private val NOISE = OPEN_WORDS + SEARCH_WORDS + FILLER

    private val MESSAGE_WORDS = setOf(
        "message", "messages", "msg", "sms", "text", "send", "bhejo", "bhejna", "bhej", "bhejen", "bhejein", "likho", "likhna",
        "میسج", "پیغام", "بھیجو", "بھیجنا", "بھیج", "لکھو"
    )
    private val SETTINGS_WORDS = setOf("settings", "setting", "سیٹنگ", "سیٹنگز", "سیٹنگس")
    private val SCAM_WORDS = setOf(
        "scam", "scammer", "fraud", "phishing", "suspicious", "dhoka", "dhokha", "jaali",
        "فراڈ", "دھوکہ", "دھوکا", "مشکوک", "جعلی"
    )
    private val TIME_WORDS = setOf("time", "waqt", "samay", "baje", "bajay", "bajey", "وقت", "بجے")
    private val DATE_WORDS = setOf("date", "tareekh", "tarikh", "تاریخ")
    private val BATTERY_WORDS = setOf("battery", "charging", "بیٹری")
    private val HELP_WORDS = setOf("help", "madad", "madat", "مدد")
    private val GREET_WORDS = setOf("hello", "hi", "hey", "salam", "salaam", "assalam", "assalamualaikum", "aoa", "سلام", "السلام")
    private val CANCEL_WORDS = setOf("cancel", "nahi", "nahin", "no", "stop", "روکو", "نہیں", "کینسل")

    fun normalize(s: String): String = s.lowercase()
        .replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک').replace('ۃ', 'ہ')
        .replace(Regex("[^\\p{L}\\p{N}\\p{M}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun isCancel(raw: String): Boolean {
        val n = " ${normalize(raw)} "
        return n.trim().split(' ').any { it in CANCEL_WORDS } || n.contains(" rehne do ") || n.contains(" رہنے دو ")
    }

    private fun remainder(s: String): String =
        s.trim().split(' ').filter { it.isNotEmpty() && it !in NOISE }.joinToString(" ")

    private fun bestApp(n: String): Pair<AppTarget, String>? {
        var best: Pair<AppTarget, String>? = null
        for (app in AppTarget.entries) for (a in app.aliases) {
            if (n.contains(" $a ") && (best == null || a.length > best.second.length)) best = app to a
        }
        return best
    }

    fun parse(raw: String): Command {
        val text = raw.trim()
        val n = " ${normalize(text)} "
        val tokens = n.trim().split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return Command.Ask(text)

        // 1. Scam / suspicious message check
        if (tokens.any { it in SCAM_WORDS }) return Command.ScamCheck(text)

        // 2. Applications
        bestApp(n)?.let { (target, alias) ->
            val q = remainder(n.replace(" $alias ", " "))
            return when (target) {
                AppTarget.WHATSAPP ->
                    if (tokens.any { it in MESSAGE_WORDS }) Command.StartWhatsAppMessage else Command.OpenApp(target)
                AppTarget.YOUTUBE -> if (q.isNotEmpty()) Command.SearchYouTube(q) else Command.OpenApp(target)
                AppTarget.GOOGLE -> if (q.isNotEmpty()) Command.SearchGoogle(q) else Command.OpenApp(target)
                AppTarget.MAPS -> if (q.isNotEmpty()) Command.SearchMaps(q) else Command.OpenApp(target)
                else -> Command.OpenApp(target)
            }
        }

        // 3. Android settings
        for (p in SettingsPage.entries) if (p.aliases.any { n.contains(" $it ") }) return Command.OpenSettings(p)
        val hasSettingsWord = tokens.any { it in SETTINGS_WORDS }
        if (hasSettingsWord && tokens.any { it in BATTERY_WORDS }) return Command.OpenSettings(SettingsPage.BATTERY_SAVER)
        if (hasSettingsWord) return Command.OpenSettings(SettingsPage.MAIN)

        // 4. Offline basics
        val offline = when {
            tokens.any { it in TIME_WORDS } -> OfflineKind.TIME
            tokens.any { it in DATE_WORDS } -> OfflineKind.DATE
            tokens.any { it in BATTERY_WORDS } -> OfflineKind.BATTERY
            tokens.any { it in HELP_WORDS } || n.contains(" what can you do ") -> OfflineKind.HELP
            tokens.size <= 4 && tokens.any { it in GREET_WORDS } -> OfflineKind.GREETING
            else -> null
        }
        if (offline != null) return Command.Offline(offline)

        // 5. "open <something we don't know>"
        if (tokens.any { it in OPEN_WORDS }) {
            val name = remainder(n)
            if (name.isNotEmpty() && name.split(' ').size <= 3) return Command.Unsupported(name)
        }

        return Command.Ask(text)
    }
}

/** Offline, rule-based hints for the digital-safety assistant. Never claims a message is safe. */
object ScamHeuristics {
    fun signals(text: String): List<String> {
        val t = text.lowercase()
        val out = mutableListOf<String>()
        if (Regex("(https?://|www\\.|bit\\.ly|tinyurl|\\.xyz|\\.top)").containsMatchIn(t))
            out += "It contains a link. Do not tap links in unexpected messages."
        if (Regex("\\b(otp|pin|password|cvv|cnic|verification code)\\b").containsMatchIn(t))
            out += "It mentions an OTP, PIN, password or ID number. Real banks and companies never ask you to share these."
        if (Regex("\\b(urgent|immediately|blocked|suspended|expire[sd]?|last warning|final notice|within \\d+ (hours|minutes))\\b").containsMatchIn(t))
            out += "It tries to scare or rush you. Scammers create panic so you act without thinking."
        if (Regex("\\b(won|winner|prize|lucky draw|congratulations|free gift|reward)\\b").containsMatchIn(t))
            out += "It promises a prize or free money."
        if (Regex("\\b(send money|easypaisa|jazzcash|transfer|bank account|payment)\\b").containsMatchIn(t))
            out += "It asks about sending money or payments."
        return out
    }

    fun explain(text: String): String {
        val s = signals(text)
        val head = if (s.isEmpty())
            "I do not see obvious warning signs, but that does not mean the message is safe."
        else
            "Warning signs I found:\n" + s.joinToString("\n") { "• $it" }
        return head + "\n\nNever share your OTP, PIN or password with anyone. If unsure, call the company on the number printed on your card or official website. I cannot confirm that any message is completely safe."
    }
}
