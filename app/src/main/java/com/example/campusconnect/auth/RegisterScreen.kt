package com.example.campusconnect.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect.ui.components.SoftBackground

@Composable
fun RegisterScreen(
    onGoLogin: () -> Unit,
    onRegisterSuccess: (role: String) -> Unit,
    vm: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var adminCode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val state by vm.state.collectAsState()

    LaunchedEffect(state.success, state.role) {
        if (state.success && state.role != null) {
            onRegisterSuccess(state.role!!)
            vm.resetState()
        }
    }

    SoftBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row {
                    Icon(
                        imageVector = Icons.Default.AppRegistration,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Create your profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Join the campus network in minutes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Hide" else "Show")
                            }
                        }
                    )
                    Text(
                        text = "Use at least 8 characters with uppercase, lowercase, number, and special character.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = adminCode,
                        onValueChange = { adminCode = it },
                        label = { Text("Admin Code (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { vm.register(name, email, password, adminCode) },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.loading) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Creating...")
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Register")
                        }
                    }

                    TextButton(
                        onClick = onGoLogin,
                        enabled = !state.loading
                    ) {
                        Text("Already have an account? Login")
                    }

                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}


