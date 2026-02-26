package com.example.campusconnect.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun StudentEventsScreen(
    onBack: () -> Unit,
    vm: EventViewModel
) {
    val ui by vm.ui.collectAsState()
    val events by vm.events.collectAsState()
    val registeredEventIds by vm.registeredEventIds.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.clearError()
        vm.listenEvents()
        vm.listenMyEventRegistrations()
    }

    val filteredEvents = remember(events, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            events
        } else {
            events.filter { it.title.contains(query, ignoreCase = true) }
        }
    }

    val headerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(                title = { Text("Campus Events") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBrush, RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(
                                text = "Find something new this week",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Explore upcoming talks, meetups, and club activities.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search event name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        }
                    )
                }

                if (ui.loading) {
                    item { CircularProgressIndicator() }
                }

                ui.error?.let {
                    item { Text(it, color = MaterialTheme.colorScheme.error) }
                }

                if (filteredEvents.isEmpty() && !ui.loading) {
                    item {
                        Text(
                            if (searchQuery.isBlank()) "No events yet." else "No events found for \"$searchQuery\"."
                        )
                    }
                } else {
                    items(filteredEvents.size) { index ->
                        val event = filteredEvents[index]
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (event.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = event.imageUrl,
                                        contentDescription = "Event poster",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                    )
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
                                val isRegistered = registeredEventIds.contains(event.id)
                                val displayRegistrations = event.registrationCount + if (isRegistered) 1 else 0
                                Text(
                                    text = "Registrations: $displayRegistrations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isRegistered) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {},
                                            enabled = false,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Registered")
                                        }
                                        OutlinedButton(
                                            onClick = { vm.cancelEventRegistration(event.id) },
                                            enabled = !ui.loading,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Cancel")
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { vm.registerForEvent(event.id) },
                                        enabled = !ui.loading,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Register")
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





