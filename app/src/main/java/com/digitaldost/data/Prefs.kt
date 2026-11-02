package com.digitaldost.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class Lang(val label: String, val promptName: String) {
    EN("English", "English"),
    UR("اردو", "Urdu (written in Urdu script)"),
    RO("Roman Urdu", "Roman Urdu (Urdu written in English letters)")
}

/** All settings live on-device. The API key is stored in EncryptedSharedPreferences (Android Keystore). */
class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("dd_prefs", Context.MODE_PRIVATE)

    private val secure: SharedPreferences? = try {
        val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx, "dd_secure", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) { null }

    var lang: Lang
        get() = runCatching { Lang.valueOf(sp.getString("lang", Lang.EN.name)!!) }.getOrDefault(Lang.EN)
        set(v) = sp.edit().putString("lang", v.name).apply()

    /** false = Local Privacy Mode (default), true = Online AI Mode */
    var onlineMode: Boolean
        get() = sp.getBoolean("online", false)
        set(v) = sp.edit().putBoolean("online", v).apply()

    var speakReplies: Boolean
        get() = sp.getBoolean("speak", true)
        set(v) = sp.edit().putBoolean("speak", v).apply()

    var floatingEnabled: Boolean
        get() = sp.getBoolean("floating", false)
        set(v) = sp.edit().putBoolean("floating", v).apply()

    /** "en-US" or "ur-PK" – language the speech recogniser listens for */
    var voiceLocale: String
        get() = sp.getString("voice_locale", "en-US")!!
        set(v) = sp.edit().putString("voice_locale", v).apply()

    var apiKey: String
        get() = secure?.getString("api_key", "") ?: ""
        set(v) { secure?.edit()?.putString("api_key", v.trim())?.apply() }
}
