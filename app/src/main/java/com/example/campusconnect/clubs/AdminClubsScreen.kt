package com.example.campusconnect.clubs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusconnect.ui.components.SoftBackground
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClubsScreen(
    onBack: () -> Unit,
    onOpenClubAnnouncements: (clubId: String, clubName: String) -> Unit,
    vm: ClubViewModel
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var expandedMembersForId by remember { mutableStateOf<String?>(null) }

    val ui by vm.ui.collectAsState()
    val clubs by vm.clubs.collectAsState()
    val members by vm.members.collectAsState()

    LaunchedEffect(Unit) {
        vm.listenClubs()
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(                title = { Text("Clubs Studio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        SoftBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(top=48.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Build campus communities",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Create clubs, manage members, and post updates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Text(
                    text = "Create a club",
                    style = MaterialTheme.typography.titleMedium
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val imageModel = imageUri ?: existingImageUrl.takeIf { it.isNotBlank() }
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Club image preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            )
                        }

                        Button(
                            onClick = { pickImage.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (imageModel == null) "Add Club Image" else "Change Image")
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Club Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                localError = null
                                val currentId = editingId
                                val pickedImage = imageUri

                                fun clearForm() {
                                    name = ""
                                    description = ""
                                    imageUri = null
                                    existingImageUrl = ""
                                }

                                fun saveClub(finalUrl: String) {
                                    if (currentId == null) {
                                        vm.createClub(name, description, finalUrl) {
                                            clearForm()
                                        }
                                    } else {
                                        vm.updateClub(currentId, name, description, finalUrl) {
                                            clearForm()
                                            editingId = null
                                        }
                                    }
                                }

                                if (pickedImage == null) {
                                    val url = if (currentId == null) "" else existingImageUrl
                                    saveClub(url)
                                } else {
                                    uploading = true
                                    val storagePath = if (currentId == null) {
                                        "clubs/${System.currentTimeMillis()}_${pickedImage.lastPathSegment ?: "cover"}.jpg"
                                    } else {
                                        "clubs/$currentId/cover_${System.currentTimeMillis()}.jpg"
                                    }
                                    val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
                                    storageRef.putFile(pickedImage)
                                        .addOnSuccessListener {
                                            storageRef.downloadUrl
                                                .addOnSuccessListener { url ->
                                                    uploading = false
                                                    saveClub(url.toString())
                                                }
                                                .addOnFailureListener { e ->
                                                    uploading = false
                                                    localError = e.message
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            uploading = false
                                            localError = e.message
                                        }
                                }
                            },
                            enabled = !ui.loading && !uploading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isEditing = editingId != null
                            val label = if (isEditing) "Update Club" else "Publish Club"
                            Text(
                                when {
                                    uploading -> "Uploading..."
                                    ui.loading -> if (isEditing) "Updating..." else "Publishing..."
                                    else -> label
                                }
                            )
                        }

                        if (editingId != null) {
                            TextButton(
                                onClick = {
                                    name = ""
                                    description = ""
                                    imageUri = null
                                    existingImageUrl = ""
                                    editingId = null
                                }
                            ) {
                                Text("Cancel Edit")
                            }
                        }

                        localError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }

                        ui.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = vm::clearError) { Text("Dismiss") }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Existing clubs",
                    style = MaterialTheme.typography.titleMedium
                )

                if (clubs.isEmpty() && !ui.loading) {
                    Text("No clubs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        clubs.forEach { club ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (club.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = club.imageUrl,
                                            contentDescription = "Club cover",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                        )
                                    }
                                    Text(club.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        club.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                name = club.name
                                                description = club.description
                                                imageUri = null
                                                existingImageUrl = club.imageUrl
                                                editingId = club.id
                                            }
                                        ) {
                                            Text("Edit")
                                        }
                                        OutlinedButton(onClick = { vm.deleteClub(club.id) }) {
                                            Text("Delete")
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    expandedMembersForId =
                                                        if (expandedMembersForId == club.id) null else club.id
                                                    if (expandedMembersForId == club.id) {
                                                        vm.listenMembers(club.id)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ListAlt,
                                                    contentDescription = null
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Members")
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    onOpenClubAnnouncements(club.id, club.name)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = null
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Announcements")
                                            }
                                        }
                                    }

                                    if (expandedMembersForId == club.id) {
                                        if (members.isEmpty()) {
                                            Text(
                                                "No members yet.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                members.forEach { member ->
                                                    Text(
                                                        "${member.name} - ${member.email}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}







