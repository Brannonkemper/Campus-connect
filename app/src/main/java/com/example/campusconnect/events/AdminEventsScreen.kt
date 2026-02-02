package com.example.campusconnect.events

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import com.example.campusconnect.ui.components.SoftBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventsScreen(
    onBack: () -> Unit,
    vm: EventViewModel
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }

    val ui by vm.ui.collectAsState()
    val events by vm.events.collectAsState()

    LaunchedEffect(Unit) {
        vm.listenEvents()
    }

    val headerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(                title = { Text("Event Studio") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
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
                        .background(headerBrush, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                Column {
                    Text(
                        text = "Create events that stand out",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Design, publish, and refine campus activities in one place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val imageModel = imageUri ?: existingImageUrl.takeIf { it.isNotBlank() }
                if (imageModel != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Event poster preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                Button(
                    onClick = { pickImage.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imageModel == null) "Add Event Image" else "Change Image")
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date and Time") },
                    singleLine = true,
                    placeholder = { Text("e.g. Oct 21, 4:00 PM") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue") },
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
                            title = ""
                            date = ""
                            venue = ""
                            description = ""
                            imageUri = null
                            existingImageUrl = ""
                        }

                        fun saveEvent(finalImageUrl: String) {
                            if (currentId == null) {
                                vm.createEvent(title, date, venue, description, finalImageUrl) {
                                    clearForm()
                                }
                            } else {
                                vm.updateEvent(currentId, title, date, venue, description, finalImageUrl) {
                                    clearForm()
                                    editingId = null
                                }
                            }
                        }

                        if (pickedImage == null) {
                            val url = if (currentId == null) "" else existingImageUrl
                            saveEvent(url)
                        } else {
                            uploading = true
                            val storagePath = if (currentId == null) {
                                "events/${System.currentTimeMillis()}_${pickedImage.lastPathSegment ?: "poster"}.jpg"
                            } else {
                                "events/$currentId/poster_${System.currentTimeMillis()}.jpg"
                            }
                            val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
                            storageRef.putFile(pickedImage)
                                .addOnSuccessListener {
                                    storageRef.downloadUrl
                                        .addOnSuccessListener { url ->
                                            uploading = false
                                            saveEvent(url.toString())
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
                    val label = if (isEditing) "Update Event" else "Publish Event"
                    val loadingLabel = if (isEditing) "Updating..." else "Publishing..."
                    Text(
                        when {
                            uploading -> "Uploading..."
                            ui.loading -> loadingLabel
                            else -> label
                        }
                    )
                }

                if (editingId != null) {
                    TextButton(
                        onClick = {
                            title = ""
                            date = ""
                            venue = ""
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

            HorizontalDivider()

            if (events.isEmpty() && !ui.loading) {
                Text("No events yet.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    events.forEach { event ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (event.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = event.imageUrl,
                                        contentDescription = "Event poster",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${event.date} at ${event.venue}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            title = event.title
                                            date = event.date
                                            venue = event.venue
                                            description = event.description
                                            imageUri = null
                                            existingImageUrl = event.imageUrl
                                            editingId = event.id
                                        }
                                    ) {
                                        Text("Edit")
                                    }
                                    TextButton(onClick = { vm.deleteEvent(event.id) }) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }

                if (ui.loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}









