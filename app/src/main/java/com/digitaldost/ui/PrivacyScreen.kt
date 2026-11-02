package com.digitaldost.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyScreen(
    vm: ChatViewModel,
    micGranted: Boolean,
    overlayGranted: Boolean,
    onFloatingToggle: (Boolean) -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Privacy & settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("You stay in control of your assistant and personal data.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("CURRENT MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(if (vm.online) "Online AI Mode" else "Local Privacy Mode", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (vm.online) "Open questions and message checks are sent to an external AI service. Opening apps still works on your phone."
                    else "No cloud AI. Chats are saved only on this phone. Opening apps, time, date, battery and settings work without internet.",
                    fontSize = 16.sp
                )
            }
        }

        SettingsCard {
            SwitchRow("Online AI Mode", "Lets Digital Dost answer open questions using an AI service. Your text leaves the phone.", vm.online) { vm.updateOnline(it) }
        }
        if (vm.online) {
            OutlinedTextField(
                value = keyInput, onValueChange = { keyInput = it },
                label = { Text(if (vm.hasApiKey) "API key saved (enter new to replace)" else "API key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { vm.saveApiKey(keyInput); keyInput = "" }, enabled = keyInput.isNotBlank()) { Text("Save key securely") }
        }

        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SwitchRow("Floating assistant", "Shows a button over other apps. It does not listen until you tap the mic.", vm.floatingOn, onFloatingToggle)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow("Speak replies after voice questions", "Digital Dost reads answers aloud.", vm.speakReplies) { vm.updateSpeakReplies(it) }
            }
        }

        Text("Voice recognition language", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.voiceLocale == "en-US") Button(onClick = { vm.selectVoiceLocale("en-US") }, Modifier.weight(1f)) { Text("English") }
            else OutlinedButton(onClick = { vm.selectVoiceLocale("en-US") }, Modifier.weight(1f)) { Text("English") }
            if (vm.voiceLocale == "ur-PK") Button(onClick = { vm.selectVoiceLocale("ur-PK") }, Modifier.weight(1f)) { Text("اردو") }
            else OutlinedButton(onClick = { vm.selectVoiceLocale("ur-PK") }, Modifier.weight(1f)) { Text("اردو") }
        }
        Text(
            "Tip: for Roman Urdu, type it, or speak Urdu with the اردو option. Android's speech service may use the internet unless an offline language pack is installed.",
            fontSize = 14.sp
        )

        Text("Permissions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        SettingsCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusRow("Microphone", if (micGranted) "Allowed" else "Not allowed", micGranted)
                StatusRow("Display over other apps", if (overlayGranted) "Allowed" else "Not allowed", overlayGranted)
                StatusRow("Screen capture", "Not used", null)
                StatusRow("Accessibility service", "Not used", null)
                StatusRow("Contacts", "You choose contacts in WhatsApp", null)
            }
        }

        HorizontalDivider()
        Button(
            onClick = { confirmDelete = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Delete all conversations", fontSize = 18.sp) }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete all conversations?") },
        text = { Text("This cannot be undone.", fontSize = 18.sp) },
        confirmButton = { Button(onClick = { vm.clearChat(); confirmDelete = false }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 14.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatusRow(label: String, value: String, allowed: Boolean?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (allowed != null) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = if (allowed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) {
                Text(if (allowed) "  On  " else "  Off  ", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 1.dp) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}
