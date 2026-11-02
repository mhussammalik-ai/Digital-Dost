package com.digitaldost

import com.digitaldost.engine.*

object ParserCases {
    const val SCAM_TEXT = "ye message scam lag raha hai: your account is blocked click http://bit.ly/x"

    val cases: List<Pair<String, Command>> = listOf(
        "Open WhatsApp" to Command.OpenApp(AppTarget.WHATSAPP),
        "google kholo" to Command.OpenApp(AppTarget.GOOGLE),
        "Google khol do" to Command.OpenApp(AppTarget.GOOGLE),
        "youtube chalao" to Command.OpenApp(AppTarget.YOUTUBE),
        "یوٹیوب کھولو" to Command.OpenApp(AppTarget.YOUTUBE),
        "واٹس ایپ کھولیں" to Command.OpenApp(AppTarget.WHATSAPP),
        "گوگل کھولو" to Command.OpenApp(AppTarget.GOOGLE),
        "open maps" to Command.OpenApp(AppTarget.MAPS),
        "Open gmail" to Command.OpenApp(AppTarget.GMAIL),
        "camera kholo" to Command.OpenApp(AppTarget.CAMERA),
        "Mujhe YouTube par C programming ka lecture dekhna hai" to Command.SearchYouTube("c programming lecture"),
        "youtube pe imran khan interview search karo" to Command.SearchYouTube("imran khan interview"),
        "google par weather in karachi search karo" to Command.SearchGoogle("weather karachi"),
        "maps par saddar karachi dhundo" to Command.SearchMaps("saddar karachi"),
        "wifi kholo" to Command.OpenSettings(SettingsPage.WIFI),
        "I want to enable Wi-Fi" to Command.OpenSettings(SettingsPage.WIFI),
        "how do I turn on wifi" to Command.OpenSettings(SettingsPage.WIFI),
        "bluetooth on karo" to Command.OpenSettings(SettingsPage.BLUETOOTH),
        "open settings" to Command.OpenSettings(SettingsPage.MAIN),
        "volume kam karo" to Command.OpenSettings(SettingsPage.SOUND),
        "battery settings kholo" to Command.OpenSettings(SettingsPage.BATTERY_SAVER),
        "send whatsapp message" to Command.StartWhatsAppMessage,
        "whatsapp par message bhejna hai" to Command.StartWhatsAppMessage,
        "what time is it" to Command.Offline(OfflineKind.TIME),
        "battery kitni hai" to Command.Offline(OfflineKind.BATTERY),
        "hello" to Command.Offline(OfflineKind.GREETING),
        "salam" to Command.Offline(OfflineKind.GREETING),
        "help" to Command.Offline(OfflineKind.HELP),
        SCAM_TEXT to Command.ScamCheck(SCAM_TEXT),
        "open facebook" to Command.Unsupported("facebook"),
        "what is the capital of France" to Command.Ask("what is the capital of France"),
        "why is the sky blue" to Command.Ask("why is the sky blue"),
    )
}
