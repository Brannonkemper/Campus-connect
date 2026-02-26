package com.example.campusconnect.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.regex.Pattern

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

    // Secret admin code (change to your own)
    private val SECRET_ADMIN_CODE = "CC-ADMIN-2026"
    private val emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    /**
     * Register a user.
     * - Everyone becomes "student" unless they provide correct adminCode.
     */
    fun register(name: String, email: String, password: String, adminCode: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
            _state.value = AuthState(error = "Please fill all required fields in a valid way.")
            return
        }

        if (!isValidEmail(trimmedEmail)) {
            _state.value = AuthState(error = "Enter a valid email address.")
            return
        }

        val passwordValidationError = validateStrongPassword(password)
        if (passwordValidationError != null) {
            _state.value = AuthState(error = passwordValidationError)
            return
        }

        val role = if (adminCode.trim() == SECRET_ADMIN_CODE) "admin" else "student"
        _state.value = AuthState(loading = true)

        auth.createUserWithEmailAndPassword(trimmedEmail, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    _state.value = AuthState(loading = false, error = "Registration failed (no user id)")
                    return@addOnSuccessListener
                }

                _state.value = AuthState(success = true, role = role)

                val userDoc = hashMapOf(
                    "name" to trimmedName,
                    "email" to trimmedEmail,
                    "role" to role,
                    "createdAt" to Timestamp.now()
                )

                db.collection("users").document(uid).set(userDoc)
                    .addOnFailureListener { e ->
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
                    error = e.message ?: "Registration failed."
                )
            }
    }

    /**
     * Login and fetch role from Firestore.
     */
    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || password.isBlank()) {
            _state.value = AuthState(error = "Please fill all required fields in a valid way.")
            return
        }

        if (!isValidEmail(trimmedEmail)) {
            _state.value = AuthState(error = "Enter a valid email address.")
            return
        }

        _state.value = AuthState(loading = true)

        auth.signInWithEmailAndPassword(trimmedEmail, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    _state.value = AuthState(error = "Login failed (no user id).")
                    return@addOnSuccessListener
                }

                fetchRole(uid)
            }
            .addOnFailureListener { e ->
                val message = when {
                    e is FirebaseAuthInvalidCredentialsException && e.errorCode == "ERROR_WRONG_PASSWORD" -> "Incorrect password."
                    e is FirebaseAuthInvalidCredentialsException && e.errorCode == "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
                    e is FirebaseAuthInvalidUserException -> "No account found for this email."
                    else -> e.message ?: "Login failed."
                }
                _state.value = AuthState(error = message)
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

    private fun isValidEmail(email: String): Boolean {
        return emailPattern.matcher(email).matches()
    }

    private fun validateStrongPassword(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters."
        if (!password.any { it.isUpperCase() }) return "Password must include at least one uppercase letter."
        if (!password.any { it.isLowerCase() }) return "Password must include at least one lowercase letter."
        if (!password.any { it.isDigit() }) return "Password must include at least one number."
        if (password.none { !it.isLetterOrDigit() }) return "Password must include at least one special character."
        return null
    }
}
