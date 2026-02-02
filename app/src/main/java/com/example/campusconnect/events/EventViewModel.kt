package com.example.campusconnect.events

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class EventUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class EventViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _ui = MutableStateFlow(EventUiState())
    val ui: StateFlow<EventUiState> = _ui

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private var startedListening = false

    fun listenEvents() {
        if (startedListening) return
        startedListening = true

        _ui.value = EventUiState(loading = true)

        db.collection("events")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _ui.value = EventUiState(loading = false, error = e.message)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { doc ->
                    Event(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        date = doc.getString("date") ?: "",
                        venue = doc.getString("venue") ?: "",
                        description = doc.getString("description") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                } ?: emptyList()

                _events.value = list
                _ui.value = EventUiState(loading = false)
            }
    }

    fun createEvent(
        title: String,
        date: String,
        venue: String,
        description: String,
        imageUrl: String,
        onDone: () -> Unit
    ) {
        if (title.isBlank() || date.isBlank() || venue.isBlank() || description.isBlank()) {
            _ui.value = EventUiState(error = "All fields are required")
            return
        }

        val uid = auth.currentUser?.uid ?: run {
            _ui.value = EventUiState(error = "Not logged in")
            return
        }

        _ui.value = EventUiState(loading = true)

        val data = hashMapOf(
            "title" to title.trim(),
            "date" to date.trim(),
            "venue" to venue.trim(),
            "description" to description.trim(),
            "imageUrl" to imageUrl.trim(),
            "createdAt" to Timestamp.now(),
            "createdBy" to uid
        )

        db.collection("events")
            .add(data)
            .addOnSuccessListener {
                _ui.value = EventUiState(loading = false)
                onDone()
            }
            .addOnFailureListener { e ->
                _ui.value = EventUiState(loading = false, error = e.message)
            }
    }

    fun updateEvent(
        id: String,
        title: String,
        date: String,
        venue: String,
        description: String,
        imageUrl: String,
        onDone: () -> Unit
    ) {
        if (title.isBlank() || date.isBlank() || venue.isBlank() || description.isBlank()) {
            _ui.value = EventUiState(error = "All fields are required")
            return
        }

        _ui.value = EventUiState(loading = true)

        val data = mapOf<String, Any>(
            "title" to title.trim(),
            "date" to date.trim(),
            "venue" to venue.trim(),
            "description" to description.trim(),
            "imageUrl" to imageUrl.trim()
        )

        db.collection("events")
            .document(id)
            .update(data)
            .addOnSuccessListener {
                _ui.value = EventUiState(loading = false)
                onDone()
            }
            .addOnFailureListener { e ->
                _ui.value = EventUiState(loading = false, error = e.message)
            }
    }

    fun deleteEvent(id: String) {
        _ui.value = EventUiState(loading = true)

        db.collection("events")
            .document(id)
            .delete()
            .addOnSuccessListener {
                _ui.value = EventUiState(loading = false)
            }
            .addOnFailureListener { e ->
                _ui.value = EventUiState(loading = false, error = e.message)
            }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}
