package com.example.campusconnect.clubs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
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
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubAnnouncementsScreen(
    clubId: String,
    clubName: String,
    canPost: Boolean,
    isMember: Boolean,
    onBack: () -> Unit,
    vm: ClubViewModel
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val ui by vm.ui.collectAsState()
    val announcements by vm.clubAnnouncements.collectAsState()

    LaunchedEffect(clubId, isMember, canPost) {
        if (canPost || isMember) {
            vm.listenClubAnnouncements(clubId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(                title = { Text(clubName) },
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
                    .verticalScroll(rememberScrollState()),
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
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Club announcements",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Updates shared with club members.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                if (!canPost && !isMember) {
                    Text(
                        "Join this club to view announcements.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    return@Column
                }

                if (canPost) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
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
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    vm.postClubAnnouncement(clubId, title, message) {
                                        title = ""
                                        message = ""
                                    }
                                },
                                enabled = !ui.loading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (ui.loading) "Posting..." else "Post")
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

                ui.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                if (announcements.isEmpty() && !ui.loading) {
                    Text("No announcements yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        announcements.forEach { a ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(a.title, style = MaterialTheme.typography.titleMedium)
                                    formatTimestamp(a.createdAt)?.let { timeLabel ->
                                        Text(
                                            timeLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        a.message,
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

private fun formatTimestamp(ts: Timestamp?): String? {
    if (ts == null) return null
    val formatter = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault())
    return formatter.format(ts.toDate())
}








