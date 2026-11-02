const state = {
  online: false,
  speak: false,
  listening: false
};

const chatScroll = document.querySelector("#chatScroll");
const messages = document.querySelector("#messages");
const welcome = document.querySelector("#welcome");
const input = document.querySelector("#messageInput");
const sendButton = document.querySelector("#sendButton");
const micButton = document.querySelector("#micButton");
const micLabel = document.querySelector("#micLabel");
const voiceStatus = document.querySelector("#voiceStatus");
const deleteDialog = document.querySelector("#deleteDialog");
let conversationHistory = [];

function formatBubbleText(bubble, text) {
  bubble.textContent = text;
  if (/[\u0600-\u06FF]/.test(text)) {
    bubble.dir = "rtl";
    bubble.lang = "ur";
  } else {
    bubble.dir = "ltr";
  }
}

const responses = [
  [/open whatsapp/i, "I can open WhatsApp for you. In a full phone integration this would launch the app; this browser demo keeps everything local."],
  [/youtube|video/i, "I can search YouTube for videos about your topic. This web demo will not open external apps or collect your search history."],
  [/wifi|wi-fi/i, "To turn on Wi-Fi, open your device's quick settings and tap the Wi-Fi tile."],
  [/time/i, "The current time is " + new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) + "."],
  [/hello|hi|salam/i, "Assalam-o-Alaikum! How can I help you today?"]
];

function showView(viewName) {
  document.querySelectorAll(".view").forEach(view => view.classList.remove("active"));
  document.querySelectorAll(".nav-item, .brand").forEach(item => item.classList.remove("active"));
  document.querySelector("#" + viewName + "View")?.classList.add("active");
  document.querySelectorAll('[data-view="' + viewName + '"]').forEach(item => item.classList.add("active"));
}

function addMessage(text, role = "user") {
  welcome.hidden = true;
  const bubble = document.createElement("div");
  bubble.className = "bubble " + (role === "assistant" ? "assistant" : "user");
  formatBubbleText(bubble, text);
  messages.appendChild(bubble);
  conversationHistory.push({ role, content: text });
  localStorage.setItem("digitalDostHistory", JSON.stringify(conversationHistory.slice(-20)));
  chatScroll.scrollTop = chatScroll.scrollHeight;
}

function getResponse(text) {
  const match = responses.find(([pattern]) => pattern.test(text));
  return match ? match[1] : "I understand you said: “" + text + "”. I can help with apps, settings, reminders, searches, and everyday questions.";
}

async function sendMessage(text) {
  const cleanText = text.trim();
  if (!cleanText) return;
  addMessage(cleanText, "user");
  input.value = "";
  sendButton.disabled = true;

  let answer;
  if (state.online) {
    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ messages: conversationHistory.slice(-20) })
      });
      const data = await response.json().catch(() => ({
        success: false,
        error: "The server returned an invalid response."
      }));
      if (!response.ok || !data.success) throw new Error(data.error || "Request failed.");
      answer = data.response;
    } catch (error) {
      answer = error.message || "Unable to reach the AI service. Please try again.";
    }
  } else {
    await new Promise(resolve => window.setTimeout(resolve, 350));
    answer = getResponse(cleanText);
  }

  addMessage(answer, "assistant");
  sendButton.disabled = false;
  if (state.speak && "speechSynthesis" in window) {
    speechSynthesis.speak(new SpeechSynthesisUtterance(answer));
  }
}

document.querySelectorAll("[data-view]").forEach(button => {
  button.addEventListener("click", () => showView(button.dataset.view));
});

document.querySelectorAll("[data-lang]").forEach(button => {
  button.addEventListener("click", () => {
    document.querySelectorAll("[data-lang]").forEach(item => item.classList.remove("active"));
    button.classList.add("active");
  });
});

document.querySelectorAll("[data-voice]").forEach(button => {
  button.addEventListener("click", () => {
    document.querySelectorAll("[data-voice]").forEach(item => item.classList.remove("active"));
    button.classList.add("active");
  });
});

document.querySelectorAll("[data-command]").forEach(button => {
  button.addEventListener("click", () => sendMessage(button.dataset.command));
});

sendButton.addEventListener("click", () => sendMessage(input.value));
input.addEventListener("keydown", event => {
  if (event.key === "Enter") sendMessage(input.value);
});

input.addEventListener("input", () => {
  sendButton.disabled = !input.value.trim();
});

micButton.addEventListener("click", () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    voiceStatus.textContent = "Voice recognition is not supported in this browser.";
    return;
  }

  state.listening = !state.listening;
  micButton.classList.toggle("listening", state.listening);
  micLabel.textContent = state.listening ? "Listening… tap to stop" : "Tap to speak";

  if (!state.listening) return;
  const recognition = new SpeechRecognition();
  recognition.lang = document.querySelector("[data-voice].active")?.dataset.voice === "اردو" ? "ur-PK" : "en-US";
  recognition.interimResults = true;
  recognition.onresult = event => {
    const result = Array.from(event.results).map(item => item[0].transcript).join("");
    voiceStatus.textContent = result;
  };
  recognition.onend = () => {
    const text = voiceStatus.textContent.trim();
    state.listening = false;
    micButton.classList.remove("listening");
    micLabel.textContent = "Tap to speak";
    voiceStatus.textContent = "";
    if (text) sendMessage(text);
  };
  recognition.onerror = () => {
    state.listening = false;
    micButton.classList.remove("listening");
    micLabel.textContent = "Tap to speak";
    voiceStatus.textContent = "Could not access the microphone.";
  };
  recognition.start();
});

document.querySelector("#onlineToggle").addEventListener("change", event => {
  state.online = event.target.checked;
  document.querySelector("#currentMode").textContent = state.online ? "Online AI Mode" : "Local Privacy Mode";
  document.querySelector("#modeDescription").textContent = state.online
    ? "Open questions are shown as an external-service demo in this interface."
    : "No cloud AI. Your conversations stay on this device.";
  document.querySelector("#modeLabel").textContent = state.online ? "Online AI Mode" : "Local & private";
  document.querySelector("#statusText").textContent = state.online ? "ONLINE" : "SAFE";
});

document.querySelector("#speakToggle").addEventListener("change", event => {
  state.speak = event.target.checked;
});

document.querySelector("#deleteButton").addEventListener("click", () => deleteDialog.showModal());
document.querySelector("#cancelDelete").addEventListener("click", () => deleteDialog.close());
document.querySelector("#confirmDelete").addEventListener("click", () => {
  messages.replaceChildren();
  conversationHistory = [];
  localStorage.removeItem("digitalDostHistory");
  welcome.hidden = false;
  deleteDialog.close();
});

async function refreshApiStatus() {
  try {
    const response = await fetch("/api/health");
    const data = await response.json();
    if (!data.success) {
      document.querySelector("#modeDescription").textContent =
        "Local mode is available. Add a new Groq API key to .env to enable online chat.";
      document.querySelector("#statusText").textContent = "SETUP";
    }
  } catch {
    document.querySelector("#modeDescription").textContent =
      "Start Digital Dost with npm start to enable this interface.";
    document.querySelector("#statusText").textContent = "OFFLINE";
  }
}

try {
  const savedHistory = JSON.parse(localStorage.getItem("digitalDostHistory") || "[]");
  if (Array.isArray(savedHistory)) {
    conversationHistory = savedHistory.slice(-20);
    conversationHistory.forEach(item => {
      if (typeof item.content === "string" && (item.role === "user" || item.role === "assistant")) {
        welcome.hidden = true;
        const bubble = document.createElement("div");
        bubble.className = "bubble " + (item.role === "assistant" ? "assistant" : "user");
        formatBubbleText(bubble, item.content);
        messages.appendChild(bubble);
      }
    });
  }
} catch {
  localStorage.removeItem("digitalDostHistory");
}

sendButton.disabled = true;
refreshApiStatus();
