package com.digitaldost.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitaldost.data.AppDatabase
import com.digitaldost.data.ChatMessage
import com.digitaldost.data.Lang
import com.digitaldost.data.Prefs
import com.digitaldost.engine.CommandRouter
import com.digitaldost.voice.VoiceController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app), VoiceController.Listener {

    private val prefs = Prefs(app)
    private val dao = AppDatabase.get(app).chatDao()
    private val router = CommandRouter(app, prefs)
    private val voice = VoiceController(app, prefs, this)

    val messages: StateFlow<List<ChatMessage>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var lang by mutableStateOf(prefs.lang); private set
    var online by mutableStateOf(prefs.onlineMode); private set
    var speakReplies by mutableStateOf(prefs.speakReplies); private set
    var voiceLocale by mutableStateOf(prefs.voiceLocale); private set
    var floatingOn by mutableStateOf(prefs.floatingEnabled); private set
    var hasApiKey by mutableStateOf(prefs.apiKey.isNotBlank()); private set

    var busy by mutableStateOf(false); private set
    var listening by mutableStateOf(false); private set
    var partial by mutableStateOf(""); private set
    var voiceMessage by mutableStateOf(""); private set

    /** Re-read prefs that the floating service may have changed (e.g. user closed the bubble). */
    fun refreshFromPrefs() { floatingOn = prefs.floatingEnabled }

    fun send(text: String, viaVoice: Boolean = false) {
        if (text.isBlank()) return
        voiceMessage = ""
        viewModelScope.launch {
            busy = true
            val r = router.handle(text)
            busy = false
            if (viaVoice) voice.speak(r.text)
        }
    }

    fun toggleMic() {
        if (listening) voice.stopListening() else { voiceMessage = ""; voice.startListening() }
    }

    fun stopSpeaking() = voice.stopSpeaking()

    fun selectLang(l: Lang) {
        prefs.lang = l; lang = l
        val loc = if (l == Lang.UR) "ur-PK" else "en-US"
        prefs.voiceLocale = loc; voiceLocale = loc
    }
    fun selectVoiceLocale(loc: String) { prefs.voiceLocale = loc; voiceLocale = loc }
    fun updateOnline(v: Boolean) { prefs.onlineMode = v; online = v }
    fun updateSpeakReplies(v: Boolean) { prefs.speakReplies = v; speakReplies = v }
    fun updateFloating(v: Boolean) { prefs.floatingEnabled = v; floatingOn = v }
    fun saveApiKey(key: String) { prefs.apiKey = key; hasApiKey = key.isNotBlank() }
    fun clearChat() { viewModelScope.launch { dao.clear() } }

    // VoiceController.Listener (called on main thread)
    override fun onListening() { listening = true }
    override fun onPartial(text: String) { partial = text }
    override fun onFinal(text: String) { listening = false; partial = ""; send(text, viaVoice = true) }
    override fun onError(message: String) { listening = false; partial = ""; voiceMessage = message }

    override fun onCleared() { voice.shutdown() }
}
