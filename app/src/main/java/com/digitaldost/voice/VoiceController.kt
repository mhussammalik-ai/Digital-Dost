package com.digitaldost.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.digitaldost.data.Lang
import com.digitaldost.data.Prefs
import java.util.Locale

/**
 * Speech-to-text + text-to-speech. Listening only starts after the user taps the mic
 * (always accessible != always listening).
 */
class VoiceController(
    private val ctx: Context,
    private val prefs: Prefs,
    private val listener: Listener
) {
    interface Listener {
        fun onListening()
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
    }

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(ctx) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            listener.onError("Voice recognition is not available on this phone. You can type instead.")
            return
        }
        stopSpeaking()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = listener.onListening()
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        ?.let { listener.onPartial(it) }
                }
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (text.isNullOrBlank()) listener.onError(describe(SpeechRecognizer.ERROR_NO_MATCH))
                    else listener.onFinal(text)
                }
                override fun onError(error: Int) = listener.onError(describe(error))
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, prefs.voiceLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // In Local Privacy Mode ask for on-device recognition (works only if the offline language pack is installed)
            if (!prefs.onlineMode) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() { recognizer?.stopListening() }

    fun speak(text: String) {
        if (!prefs.speakReplies || !ttsReady || text.isBlank()) return
        val engine = tts ?: return
        val wanted = if (prefs.lang == Lang.UR) Locale("ur", "PK") else Locale("en", "IN")
        val r = engine.setLanguage(wanted)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) engine.setLanguage(Locale.US)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dd_reply")
    }

    fun stopSpeaking() { tts?.stop() }

    fun shutdown() {
        recognizer?.destroy(); recognizer = null
        tts?.stop(); tts?.shutdown(); tts = null
    }

    private fun describe(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "I didn't catch that. Tap the mic and try again, or type your request."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission is needed. Please allow it in your phone's app settings."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Voice recognition needs internet or an offline language pack. You can type instead."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice is busy. Please try again in a moment."
        else -> "Voice recognition failed. You can type your request instead."
    }
}
