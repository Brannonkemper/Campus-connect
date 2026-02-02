package com.example.campusconnect.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AuthState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val role: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    // ✅ Secret admin code (change to your own)
    private val SECRET_ADMIN_CODE = "CC-ADMIN-2026"

    /**
     * Register a user.
     * - Everyone becomes "student" unless they provide correct adminCode.
     */
    fun register(name: String, email: String, password: String, adminCode: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = AuthState(error = "Please fill all fields")
            return
        }

        val role = if (adminCode.trim() == SECRET_ADMIN_CODE) "admin" else "student"
        _state.value = AuthState(loading = true)

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    _state.value = AuthState(loading = false, error = "Registration failed (no user id)")
                    return@addOnSuccessListener
                }

                // ✅ 1) NAVIGATE IMMEDIATELY (so UI never hangs on Creating...)
                _state.value = AuthState(success = true, role = role)

                // ✅ 2) Save profile in Firestore (do not block navigation)
                val userDoc = hashMapOf(
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "role" to role,
                    "createdAt" to Timestamp.now()
                )

                db.collection("users").document(uid).set(userDoc)
                    .addOnFailureListener { e ->
                        // optional: show message later; do NOT undo navigation
                        _state.value = AuthState(
                            success = true,
                            role = role,
                            error = "Profile save failed: ${e.message ?: "unknown error"}"
                        )
                    }
            }
            .addOnFailureListener { e ->
                _state.value = AuthState(
                    loading = false,
                    error = e.message ?: "Registration failed"
                )
            }
    }


    /**
     * Login and fetch role from Firestore.
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState(error = "Enter email and password")
            return
        }

        _state.value = AuthState(loading = true)

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    _state.value = AuthState(error = "Login failed (no user id).")
                    return@addOnSuccessListener
                }

                fetchRole(uid)
            }
            .addOnFailureListener { e ->
                _state.value = AuthState(error = e.message ?: "Login failed.")
            }
    }

    /**
     * Send password reset email.
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _state.value = AuthState(error = "Enter your email to reset password")
            return
        }

        _state.value = AuthState(loading = true)

        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                // Using error as a general message display (you can rename later to "message")
                _state.value = AuthState(error = "Password reset link sent. Check your email.")
            }
            .addOnFailureListener { e ->
                _state.value = AuthState(error = e.message ?: "Failed to send reset email.")
            }
    }

    private fun fetchRole(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "student"
                _state.value = AuthState(success = true, role = role)
            }
            .addOnFailureListener { e ->
                _state.value = AuthState(error = e.message ?: "Failed to fetch user role.")
            }
    }

    fun resetState() {
        _state.value = AuthState()
    }

    fun logout() {
        auth.signOut()
        _state.value = AuthState()
    }

    fun tryRestoreSession() {
        val uid = auth.currentUser?.uid ?: return
        _state.value = AuthState(loading = true)
        fetchRole(uid)
    }
}
