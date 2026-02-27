package com.example.campusconnect.clubs

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentClubsScreen(
    onBack: () -> Unit,
    onOpenClubAnnouncements: (clubId: String, clubName: String) -> Unit,
    vm: ClubViewModel
) {
    val ui by vm.ui.collectAsState()
    val clubs by vm.clubs.collectAsState()
    val myClubIds by vm.myClubIds.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.clearError()
        vm.listenClubs()
        vm.listenMyClubs()
    }

    val filteredClubs = remember(clubs, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            clubs
        } else {
            clubs.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.primary
                                    )
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Find your people",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (ui.loading) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }

                ui.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search club name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                )

                if (filteredClubs.isEmpty() && !ui.loading) {
                    Text(
                        if (searchQuery.isBlank()) "No clubs yet." else "No clubs found for \"$searchQuery\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(filteredClubs, key = { it.id }) { club ->
                            val isMember = myClubIds.contains(club.id)
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
                                        if (isMember) {
                                            OutlinedButton(onClick = { vm.leaveClub(club.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonRemove,
                                                    contentDescription = null
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Leave")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    onOpenClubAnnouncements(club.id, club.name)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = null
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Announcements")
                                            }
                                        } else {
                                            OutlinedButton(onClick = { vm.joinClub(club.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.GroupAdd,
                                                    contentDescription = null
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Join")
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


