# Digital Dost — Your Personal AI. Your Mobile. Your Privacy.

Build4Better · CodeSphere 3.0 · NED University — Android MVP (P0 + parts of P1)

## What works in this build
| Feature | Status |
|---|---|
| Floating "DD" bubble over other apps (draggable, tap to open panel, minimise, close) | ✅ |
| Voice input (tap-to-speak, never always-listening) + spoken replies (TTS) | ✅ |
| Text chat with local Room history, long-press to copy | ✅ |
| Rule-based commands in English, Urdu script and Roman Urdu | ✅ |
| Open Google / WhatsApp / YouTube / Maps / Gmail / Camera / Gallery / Phone / Browser | ✅ |
| YouTube / Google / Maps search ("opens results for X") | ✅ |
| Android Settings shortcuts (Wi-Fi, Bluetooth, sound, display, battery saver, main) | ✅ |
| WhatsApp message flow (asks what to say, opens WhatsApp with text; **user picks contact and taps Send**) | ✅ |
| Offline commands: time, date, battery, help, greeting, scam-signal check | ✅ |
| Local Privacy Mode (default) vs Online AI Mode (optional, API key in encrypted prefs) | ✅ |
| Privacy dashboard, permission rationale dialogs, delete all chats | ✅ |
| Screen understanding, reminders, Accessibility Service | ❌ not in this build |

## Setup
1. Open the `DigitalDost` folder in **Android Studio (Koala or newer)**. Let Gradle sync (it will generate the Gradle wrapper jar).
2. Use a real phone (Android 8.0+). Overlay + speech are unreliable on emulators.
3. Run. Tap **Privacy → Floating assistant** and grant microphone + "Display over other apps".
4. Optional: Privacy → Online AI Mode → paste an Anthropic API key. Model is set in `engine/OnlineAi.kt` (`MODEL`).
5. Unit tests: `./gradlew test` (parser table in `app/src/test/.../ParserCases.kt`).

## Architecture
```
MainActivity (Compose)  ─┐
FloatingAssistantService ─┼─> CommandRouter ─> CommandParser (pure Kotlin, deterministic)
(overlay, classic Views)  │        │              └─ Android Intents (apps, settings, WhatsApp share)
VoiceController (STT+TTS)─┘        ├─> Offline answers (time/date/battery/help/scam signals)
                                   ├─> OnlineAi (only if Online AI Mode + key)
                                   └─> Room (messages) · Prefs / EncryptedSharedPreferences
```
Design rule: **device actions never depend on an LLM.** The LLM is used only for open questions and richer scam explanations.

## Measured test table
`CommandParser` on the JVM: **32 / 32** command phrases (English, Urdu script, Roman Urdu) mapped to the expected action.
Voice recognition accuracy and app-launch success on real devices are **not yet measured** — fill these in after testing on your demo phone:

| Test | Result |
|---|---|
| 20 spoken commands, English | _/20 |
| 20 spoken commands, Urdu | _/20 |
| App launch (Google, WhatsApp, YouTube) × 10 | _/30 |

## Known limitations (be upfront with judges)
- Chat history is stored in Room inside the app's private sandbox (backup disabled) but the **database is not yet encrypted** (SQLCipher is the next step). Only the API key is encrypted.
- Roman Urdu is understood when **typed**. Spoken Roman Urdu depends on Android's recogniser; Urdu script (اردو option) or English works better.
- Speech recognition may use Google's servers unless an offline language pack is installed. Local Privacy Mode requests offline recognition but cannot force it.
- Urdu TTS voice may be missing; the app falls back to an English voice.
- YouTube/Google/Maps searches open the results page; the app cannot tap buttons inside other apps (no Accessibility Service).
- WhatsApp: the message is prefilled; the user chooses the contact and sends. Behaviour differs slightly between WhatsApp versions.
- Online AI Mode embeds a user-entered API key on the device — fine for a prototype; use a backend proxy in production.
- Scam signals are simple keyword rules in English; they never declare a message safe.
- Replies from the rule engine are in English/Urdu/Roman Urdu; the app's own menu labels are English only.
- Overlay behaviour varies by manufacturer (Xiaomi/Oppo/Vivo may need "autostart" or "pop-up" permissions).
- Android 15 target; tested logic: parser only. UI/overlay need on-device testing.

## AI tools & third-party disclosure
- Code drafted with the help of Claude (Anthropic).
- Libraries: AndroidX, Jetpack Compose + Material 3, Room, androidx.security-crypto, Kotlin coroutines, JUnit.
- Optional runtime service: Anthropic Messages API (only in Online AI Mode).

## 2-minute demo script
1. (0:00) Open any app → floating DD bubble visible. "Always accessible, never always listening."
2. (0:15) Tap bubble → mic → "WhatsApp kholo" → WhatsApp opens, panel steps aside.
3. (0:40) Tap bubble → type "YouTube par C programming ka lecture dekhna hai" → YouTube results.
4. (1:00) Main app: switch to Roman Urdu, ask "Wifi on karna hai" → Wi-Fi settings.
5. (1:15) Send WhatsApp message flow — show the user taps Send.
6. (1:30) Chat history (saved locally) → Privacy dashboard: Local Privacy Mode, permissions, delete.
7. (1:50) Paste a fake bank SMS → scam signals.
