package com.example.campusconnect.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusconnect.ui.components.SoftBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    vm: UserProfileViewModel
) {
    val profile by vm.profile.collectAsState()
    val ui by vm.ui.collectAsState()

    var name by remember(profile.uid) { mutableStateOf("") }
    var phone by remember(profile.uid) { mutableStateOf("") }
    var department by remember(profile.uid) { mutableStateOf("") }
    var yearLevel by remember(profile.uid) { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var localImageUrl by remember(profile.uid) { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.listenMyProfile()
    }

    LaunchedEffect(profile) {
        name = profile.name
        phone = profile.phone
        department = profile.department
        yearLevel = profile.yearLevel
        localImageUrl = profile.profileImageUrl
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) imageUri = uri
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) {
        SoftBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Personalize your campus identity",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Your name and profile photo are used for event registration and club membership records.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                val imageModel = imageUri ?: localImageUrl.takeIf { it.isNotBlank() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .height(110.dp)
                            .fillMaxWidth(0.34f)
                            .clip(CircleShape),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (imageModel == null) {
                            BoxedProfilePlaceholder()
                        } else {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Profile image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Button(
                    onClick = { pickImage.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imageModel == null) "Add Profile Picture" else "Change Profile Picture")
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = profile.email,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department / Program") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = yearLevel,
                    onValueChange = { yearLevel = it },
                    label = { Text("Year Level") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        localError = null
                        val selectedImage = imageUri
                        if (selectedImage == null) {
                            vm.updateProfile(
                                name = name,
                                phone = phone,
                                department = department,
                                yearLevel = yearLevel,
                                profileImageUrl = localImageUrl
                            )
                            return@Button
                        }

                        uploading = true
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid.isNullOrBlank()) {
                            uploading = false
                            localError = "Not logged in. Please sign in again."
                            return@Button
                        }
                        val storage = FirebaseStorage.getInstance()
                        val stamp = System.currentTimeMillis()
                        val primaryPath = "users/$uid/profile_$stamp.jpg"
                        val fallbackPath = "events/profile_pictures/$uid/profile_$stamp.jpg"

                        fun saveDetailsWithImage(url: String) {
                            uploading = false
                            imageUri = null
                            localImageUrl = url
                            vm.updateProfile(
                                name = name,
                                phone = phone,
                                department = department,
                                yearLevel = yearLevel,
                                profileImageUrl = url
                            )
                        }

                        fun saveDetailsWithoutImage(message: String) {
                            uploading = false
                            localError = message
                            vm.updateProfile(
                                name = name,
                                phone = phone,
                                department = department,
                                yearLevel = yearLevel,
                                profileImageUrl = localImageUrl
                            )
                        }

                        fun uploadTo(path: String, allowFallback: Boolean) {
                            val ref = storage.reference.child(path)
                            ref.putFile(selectedImage)
                                .addOnSuccessListener {
                                    ref.downloadUrl
                                        .addOnSuccessListener { url ->
                                            saveDetailsWithImage(url.toString())
                                        }
                                        .addOnFailureListener { e ->
                                            val denied = (e as? StorageException)?.errorCode == StorageException.ERROR_NOT_AUTHORIZED
                                            if (allowFallback && denied) {
                                                uploadTo(fallbackPath, false)
                                            } else {
                                                saveDetailsWithoutImage(
                                                    e.message ?: "Profile image upload failed. Saved details without updating photo."
                                                )
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    val denied = (e as? StorageException)?.errorCode == StorageException.ERROR_NOT_AUTHORIZED
                                    if (allowFallback && denied) {
                                        uploadTo(fallbackPath, false)
                                    } else {
                                        saveDetailsWithoutImage(
                                            e.message ?: "Profile image upload failed. Saved details without updating photo."
                                        )
                                    }
                                }
                        }

                        uploadTo(primaryPath, true)
                    },
                    enabled = !ui.saving && !uploading && !ui.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when {
                        uploading -> CircularProgressIndicator(strokeWidth = 2.dp)
                        ui.saving -> Text("Saving...")
                        else -> Text("Save Profile")
                    }
                }

                ui.error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = vm::clearError) {
                        Text("Dismiss")
                    }
                }
                localError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
private fun BoxedProfilePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
