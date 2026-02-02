package com.example.campusconnect.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiChatUiState(
    val loading: Boolean = false,
    val error: String? = null
)

data class AiChatMessage(
    val id: String = "",
    val role: String = "user",
    val content: String = "",
    val createdAt: Timestamp? = null
) {
    val isUser: Boolean
        get() = role == "user"
}

class AiChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _ui = MutableStateFlow(AiChatUiState())
    val ui: StateFlow<AiChatUiState> = _ui

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages

    private var startedListening = false

    fun listenChat() {
        val uid = auth.currentUser?.uid ?: return
        if (startedListening) return
        startedListening = true

        db.collection("users")
            .document(uid)
            .collection("ai_chats")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _ui.value = _ui.value.copy(error = e.message)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { doc ->
                    AiChatMessage(
                        id = doc.id,
                        role = doc.getString("role") ?: "user",
                        content = doc.getString("content") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()

                _messages.value = list
            }
    }

    fun sendMessage(
        userText: String,
        campusContext: String,
        apiKey: String
    ) {
        val uid = auth.currentUser?.uid ?: run {
            _ui.value = _ui.value.copy(error = "Not logged in")
            return
        }
        if (userText.isBlank()) return

        _ui.value = _ui.value.copy(loading = true, error = null)

        val userData = hashMapOf(
            "role" to "user",
            "content" to userText.trim(),
            "createdAt" to Timestamp.now()
        )

        val messagesRef = db.collection("users")
            .document(uid)
            .collection("ai_chats")

        messagesRef.add(userData)
            .addOnSuccessListener {
                viewModelScope.launch {
                    val reply = try {
                        fetchGeminiReply(
                            apiKey = apiKey,
                            messages = _messages.value + AiChatMessage(role = "user", content = userText),
                            campusContext = campusContext
                        )
                    } catch (e: Exception) {
                        "Sorry, something went wrong. Please try again."
                    }

                    val assistantData = hashMapOf(
                        "role" to "model",
                        "content" to reply,
                        "createdAt" to Timestamp.now()
                    )

                    messagesRef.add(assistantData)
                        .addOnSuccessListener {
                            _ui.value = _ui.value.copy(loading = false)
                        }
                        .addOnFailureListener { e ->
                            _ui.value = _ui.value.copy(
                                loading = false,
                                error = e.message
                            )
                        }
                }
            }
            .addOnFailureListener { e ->
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    fun setError(message: String) {
        _ui.value = _ui.value.copy(error = message)
    }

    fun clearChat() {
        val uid = auth.currentUser?.uid ?: run {
            _ui.value = _ui.value.copy(error = "Not logged in")
            return
        }

        _ui.value = _ui.value.copy(loading = true, error = null)

        val messagesRef = db.collection("users")
            .document(uid)
            .collection("ai_chats")

        messagesRef.get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        _ui.value = _ui.value.copy(loading = false)
                    }
                    .addOnFailureListener { e ->
                        _ui.value = _ui.value.copy(loading = false, error = e.message)
                    }
            }
            .addOnFailureListener { e ->
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
    }
}

private suspend fun fetchGeminiReply(
    apiKey: String,
    messages: List<AiChatMessage>,
    campusContext: String
): String = withContext(Dispatchers.IO) {
    val systemPrompt = """
You are the CampusConnect AI assistant. Use the campus data in <campus_data> to answer
questions about clubs, events, announcements, and student life. You can also answer
general questions beyond the campus data, but if the answer might be outdated or
is not in the data, say so and suggest checking the app listings.
<campus_data>
$campusContext
</campus_data>
""".trimIndent()

    val recentMessages = messages.takeLast(8)
    val contentsJson = JSONArray().apply {
        recentMessages.forEach { message ->
            val role = if (message.isUser) "user" else "model"
            put(
                JSONObject()
                    .put("role", role)
                    .put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", message.content))
                    )
            )
        }
    }

    val requestBody = JSONObject()
        .put(
            "systemInstruction",
            JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", systemPrompt))
            )
        )
        .put("contents", contentsJson)
        .put(
            "generationConfig",
            JSONObject()
                .put("temperature", 0.4)
                .put("maxOutputTokens", 512)
        )
        .toString()

    val url = URL(
        "https://generativelanguage.googleapis.com/v1beta/models/" +
            "gemini-3-flash-preview:generateContent?key=$apiKey"
    )
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15000
        readTimeout = 20000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
    }

    connection.outputStream.use { output ->
        output.write(requestBody.toByteArray())
    }

    val responseCode = connection.responseCode
    val responseText = (if (responseCode in 200..299) {
        connection.inputStream
    } else {
        connection.errorStream
    }).bufferedReader().use { it.readText() }

    if (responseCode !in 200..299) {
        Log.e("AiChat", "Gemini error: HTTP $responseCode")
        val errorMessage = runCatching {
            JSONObject(responseText).getJSONObject("error").getString("message")
        }.getOrNull()
        if (!errorMessage.isNullOrBlank()) {
            Log.e("AiChat", "Gemini error message: $errorMessage")
        } else {
            Log.e("AiChat", "Gemini error body: $responseText")
        }
        return@withContext "Sorry, I couldn't reach the AI service. ${errorMessage ?: "Please try again."}"
    }

    val root = JSONObject(responseText)
    val candidates = root.optJSONArray("candidates")
    if (candidates == null || candidates.length() == 0) {
        Log.e("AiChat", "Gemini returned no candidates.")
        return@withContext "Sorry, I didn't get a response. Please try again."
    }
    val content = candidates.getJSONObject(0).optJSONObject("content")
    val parts = content?.optJSONArray("parts")
    val text = parts?.optJSONObject(0)?.optString("text")
    if (text.isNullOrBlank()) {
        Log.e("AiChat", "Gemini returned empty text.")
        return@withContext "Sorry, I didn't get a response. Please try again."
    }
    text.trim()
}
