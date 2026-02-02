package com.example.campusconnect.announcements

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AnnouncementUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class AnnouncementViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _ui = MutableStateFlow(AnnouncementUiState())
    val ui: StateFlow<AnnouncementUiState> = _ui

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements

    private var startedListening = false

    fun listenAnnouncements() {
        if (startedListening) return
        startedListening = true

        _ui.value = AnnouncementUiState(loading = true)

        db.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _ui.value = AnnouncementUiState(loading = false, error = e.message)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { doc ->
                    Announcement(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                } ?: emptyList()

                _announcements.value = list
                _ui.value = AnnouncementUiState(loading = false)
            }
    }

    fun postAnnouncement(title: String, message: String, onDone: () -> Unit) {
        if (title.isBlank() || message.isBlank()) {
            _ui.value = AnnouncementUiState(error = "Title and message are required")
            return
        }

        val uid = auth.currentUser?.uid ?: run {
            _ui.value = AnnouncementUiState(error = "Not logged in")
            return
        }

        _ui.value = AnnouncementUiState(loading = true)

        val data = hashMapOf(
            "title" to title.trim(),
            "message" to message.trim(),
            "createdAt" to Timestamp.now(),
            "createdBy" to uid
        )

        db.collection("announcements")
            .add(data)
            .addOnSuccessListener {
                _ui.value = AnnouncementUiState(loading = false)
                onDone()
            }
            .addOnFailureListener { e ->
                _ui.value = AnnouncementUiState(loading = false, error = e.message)
            }
    }

    fun updateAnnouncement(id: String, title: String, message: String, onDone: () -> Unit) {
        if (title.isBlank() || message.isBlank()) {
            _ui.value = AnnouncementUiState(error = "Title and message are required")
            return
        }

        _ui.value = AnnouncementUiState(loading = true)

        val data = mapOf<String, Any>(
            "title" to title.trim(),
            "message" to message.trim()
        )

        db.collection("announcements")
            .document(id)
            .update(data)
            .addOnSuccessListener {
                _ui.value = AnnouncementUiState(loading = false)
                onDone()
            }
            .addOnFailureListener { e ->
                _ui.value = AnnouncementUiState(loading = false, error = e.message)
            }
    }

    fun deleteAnnouncement(id: String) {
        _ui.value = AnnouncementUiState(loading = true)

        db.collection("announcements")
            .document(id)
            .delete()
            .addOnSuccessListener {
                _ui.value = AnnouncementUiState(loading = false)
            }
            .addOnFailureListener { e ->
                _ui.value = AnnouncementUiState(loading = false, error = e.message)
            }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}
