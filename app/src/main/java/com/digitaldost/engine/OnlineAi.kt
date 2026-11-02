package com.digitaldost.engine

import com.digitaldost.data.ChatMessage
import com.digitaldost.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional Online AI Mode. Only called when the user has switched it on AND entered an API key.
 * Text sent here leaves the device. For production, route through your own backend instead of
 * storing a key in the app.
 */
class OnlineAi(private val prefs: Prefs) {

    companion object {
        const val MODEL = "claude-haiku-5-5"
        private const val URL_MESSAGES = "https://api.anthropic.com/v1/messages"
    }

    private fun systemPrompt(extra: String?): String =
        "You are Digital Dost, a patient, friendly assistant for parents, grandparents and smartphone beginners in Pakistan. " +
        "Reply in ${prefs.lang.promptName}. Use short, simple sentences and no technical jargon. " +
        "Give step-by-step help when explaining phone settings. " +
        "Never ask for or accept passwords, PINs or OTPs. Never claim a message, link or website is completely safe. " +
        "Keep answers under 120 words." + (extra?.let { " $it" } ?: "")

    suspend fun ask(history: List<ChatMessage>, userText: String, extraSystem: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val key = prefs.apiKey
                require(key.isNotBlank()) { "No API key" }

                // Build an alternating user/assistant list that starts with "user"
                val clean = mutableListOf<Pair<String, String>>()
                for ((role, text) in history.map { it.role to it.text } + ("user" to userText)) {
                    if (clean.isEmpty() && role != "user") continue
                    if (clean.isNotEmpty() && clean.last().first == role)
                        clean[clean.lastIndex] = role to (clean.last().second + "\n" + text)
                    else clean += role to text
                }
                val msgs = JSONArray()
                clean.forEach { msgs.put(JSONObject().put("role", it.first).put("content", it.second)) }

                val body = JSONObject()
                    .put("model", MODEL)
                    .put("max_tokens", 400)
                    .put("system", systemPrompt(extraSystem))
                    .put("messages", msgs)

                val conn = (URL(URL_MESSAGES).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                    setRequestProperty("x-api-key", key)
                    setRequestProperty("anthropic-version", "2023-06-01")
                }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val raw = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) error("HTTP $code")
                JSONObject(raw).getJSONArray("content").getJSONObject(0).getString("text")
            }
        }
}
