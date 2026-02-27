package com.example.campusconnect.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val department: String = "",
    val yearLevel: String = "",
    val profileImageUrl: String = ""
)

data class UserProfileUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
)

class UserProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile

    private val _ui = MutableStateFlow(UserProfileUiState())
    val ui: StateFlow<UserProfileUiState> = _ui

    private var profileListener: ListenerRegistration? = null
    private var listeningUid: String? = null

    fun listenMyProfile() {
        val uid = auth.currentUser?.uid ?: return
        if (listeningUid == uid && profileListener != null) return

        profileListener?.remove()
        listeningUid = uid
        _ui.value = _ui.value.copy(loading = true, error = null)

        profileListener = db.collection("users")
            .document(uid)
            .addSnapshotListener { doc, e ->
                if (e != null) {
                    _ui.value = _ui.value.copy(loading = false, error = e.message)
                    return@addSnapshotListener
                }

                val currentEmail = auth.currentUser?.email.orEmpty()
                _profile.value = UserProfile(
                    uid = uid,
                    name = doc?.getString("name").orEmpty(),
                    email = doc?.getString("email").orEmpty().ifBlank { currentEmail },
                    phone = doc?.getString("phone").orEmpty(),
                    department = doc?.getString("department").orEmpty(),
                    yearLevel = doc?.getString("yearLevel").orEmpty(),
                    profileImageUrl = doc?.getString("profileImageUrl").orEmpty()
                )
                _ui.value = _ui.value.copy(loading = false, error = null)
            }
    }

    fun updateProfile(
        name: String,
        phone: String,
        department: String,
        yearLevel: String,
        profileImageUrl: String,
        onDone: () -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: run {
            _ui.value = _ui.value.copy(error = "Not logged in")
            return
        }
        val email = auth.currentUser?.email.orEmpty()
        val trimmedName = name.trim()

        if (trimmedName.isBlank()) {
            _ui.value = _ui.value.copy(error = "Name is required")
            return
        }

        _ui.value = _ui.value.copy(saving = true, error = null)

        val payload = mapOf(
            "name" to trimmedName,
            "email" to email,
            "phone" to phone.trim(),
            "department" to department.trim(),
            "yearLevel" to yearLevel.trim(),
            "profileImageUrl" to profileImageUrl.trim(),
            "updatedAt" to Timestamp.now()
        )

        db.collection("users")
            .document(uid)
            .set(payload, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                _ui.value = _ui.value.copy(saving = false)
                onDone()
            }
            .addOnFailureListener { e ->
                _ui.value = _ui.value.copy(saving = false, error = e.message)
            }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    override fun onCleared() {
        profileListener?.remove()
        super.onCleared()
    }
}
