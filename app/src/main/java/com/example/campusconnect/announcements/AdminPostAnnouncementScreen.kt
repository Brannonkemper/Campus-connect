package com.example.campusconnect.announcements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusconnect.ui.components.SoftBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostAnnouncementScreen(
    onBack: () -> Unit,
    vm: AnnouncementViewModel
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }

    val ui by vm.ui.collectAsState()
    val announcements by vm.announcements.collectAsState()

    LaunchedEffect(Unit) {
        vm.listenAnnouncements()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(                title = { Text("Post Announcement") },
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
                .padding(padding)
        ) {
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
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Share a campus update",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Post, edit, or remove announcements instantly.",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val currentId = editingId
                            if (currentId == null) {
                                vm.postAnnouncement(title, message) {
                                    title = ""
                                    message = ""
                                    onBack()
                                }
                            } else {
                                vm.updateAnnouncement(currentId, title, message) {
                                    title = ""
                                    message = ""
                                    editingId = null
                                }
                            }
                        },
                        enabled = !ui.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isEditing = editingId != null
                        val label = if (isEditing) "Update" else "Post"
                        val loadingLabel = if (isEditing) "Updating..." else "Posting..."
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (ui.loading) loadingLabel else label)
                    }

                    if (editingId != null) {
                        TextButton(
                            onClick = {
                                title = ""
                                message = ""
                                editingId = null
                            }
                        ) {
                            Text("Cancel Edit")
                        }
                    }

                    ui.error?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = vm::clearError) { Text("Dismiss") }
                    }
                }
            }

            HorizontalDivider()

            if (announcements.isEmpty() && !ui.loading) {
                Text("No announcements yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    announcements.forEach { a ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(a.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    a.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            title = a.title
                                            message = a.message
                                            editingId = a.id
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit")
                                    }
                                    OutlinedButton(
                                        onClick = { vm.deleteAnnouncement(a.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Delete")
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









