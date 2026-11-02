package com.digitaldost.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitaldost.data.ChatMessage
import com.digitaldost.data.Lang

@Composable
fun ChatScreen(vm: ChatViewModel, onMicClick: () -> Unit) {
    val messages by vm.messages.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun sendTyped() {
        val text = input.trim()
        if (text.isEmpty() || vm.busy) return
        vm.send(text)
        input = ""
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("DD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Digital Dost", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (vm.online) "Online AI Mode" else "Local & private",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (vm.online) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        if (vm.online) "  ONLINE  " else "  SAFE  ",
                        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (vm.online) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Lang.entries.forEach { l ->
                if (vm.lang == l) Button(onClick = { vm.selectLang(l) }, modifier = Modifier.weight(1f)) { Text(l.label) }
                else OutlinedButton(onClick = { vm.selectLang(l) }, modifier = Modifier.weight(1f)) { Text(l.label) }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            Text("How can I help?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text("Talk, type, or choose a quick action. Your everyday tasks stay on your phone.", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(18.dp))
                            SuggestionChip("Open WhatsApp") { vm.send("open whatsapp") }
                            Spacer(Modifier.height(8.dp))
                            SuggestionChip("Find a YouTube video") { vm.send("youtube search videos") }
                            Spacer(Modifier.height(8.dp))
                            SuggestionChip("Turn on Wi-Fi") { vm.send("wifi on") }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    items(messages, key = { it.id }) { MessageBubble(it) }
                }
            }
        }

        if (vm.busy) Text("Working…", fontSize = 16.sp)
        if (vm.listening) Text(vm.partial.ifBlank { "Listening…" }, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        if (vm.voiceMessage.isNotBlank()) Text(vm.voiceMessage, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)

        Surface(
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                if (vm.listening) Text(vm.partial.ifBlank { "Listening…" }, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 6.dp))
                if (vm.voiceMessage.isNotBlank()) Text(vm.voiceMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type here…", fontSize = 18.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendTyped() }),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )
                    Button(
                        onClick = { sendTyped() },
                        enabled = input.trim().isNotEmpty() && !vm.busy,
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("↑", fontSize = 24.sp) }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onMicClick,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (vm.listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (vm.listening) "■  Stop listening" else "◎  Tap to speak", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage) {
    val mine = m.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (mine) 20.dp else 5.dp, bottomEnd = if (mine) 5.dp else 20.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            // SelectionContainer: long-press to select and copy a response
            SelectionContainer {
                Text(m.text, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 16.sp, color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
