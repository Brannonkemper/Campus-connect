package com.example.campusconnect.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Announcement
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusconnect.clubs.Club
import com.example.campusconnect.clubs.ClubViewModel
import com.example.campusconnect.events.Event
import com.example.campusconnect.events.EventViewModel
import com.example.campusconnect.ui.components.SoftBackground
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    onOpenAnnouncements: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenClubs: () -> Unit,
    onOpenAiChat: () -> Unit,
    onLogout: () -> Unit,
    eventVm: EventViewModel,
    clubVm: ClubViewModel
) {
    var selectedDestination by rememberSaveable { mutableStateOf(StudentDestination.ANNOUNCEMENTS) }

    val events by eventVm.events.collectAsState()
    val clubs by clubVm.clubs.collectAsState()
    val myClubIds by clubVm.myClubIds.collectAsState()
    val registeredEventIds by eventVm.registeredEventIds.collectAsState()
    val nowMillis = System.currentTimeMillis()

    LaunchedEffect(Unit) {
        eventVm.listenEvents()
        eventVm.listenMyEventRegistrations()
        clubVm.listenClubs()
        clubVm.listenMyClubs()
    }

    val joinedClubs = clubs
        .filter { myClubIds.contains(it.id) }
        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }

    val registeredEvents = events
        .filter { registeredEventIds.contains(it.id) }
        .sortedBy { eventDateMillis(it) }

    val upcomingEvents = events
        .map { it to eventDateMillis(it) }
        .filter { (_, whenMs) -> whenMs == Long.MAX_VALUE || whenMs >= nowMillis }
        .sortedBy { it.second }
        .map { it.first }
        .take(3)

    val topClubs = clubs
        .sortedWith(
            compareByDescending<Club> { it.createdAt?.toDate()?.time ?: 0L }
                .thenBy { it.name.lowercase() }
        )
        .take(5)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Student Dashboard") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Logout")
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
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "Campus Pulse",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "See your joined clubs and registered events at a glance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Search campus updates",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                ) {
                    val hasPersonalizedData = registeredEvents.isNotEmpty() || joinedClubs.isNotEmpty()

                    if (!hasPersonalizedData) {
                        item {
                            Text(
                                text = "You have not joined a club or registered for an event yet. Register for an event or join a club to personalize this dashboard.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            Text(
                                text = "Upcoming Events",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (upcomingEvents.isEmpty()) {
                            item {
                                Text(
                                    text = "No upcoming events available.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            item {
                                EventCarousel(events = upcomingEvents, onClick = onOpenEvents)
                            }
                        }

                        item {
                            Text(
                                text = "Top Clubs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (topClubs.isEmpty()) {
                            item {
                                Text(
                                    text = "No clubs available right now.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            item {
                                ClubCarousel(clubs = topClubs, onClick = onOpenClubs)
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "My Registered Events",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (registeredEvents.isEmpty()) {
                            item {
                                Text(
                                    text = "No event registrations yet. Register for one from the upcoming list.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (upcomingEvents.isNotEmpty()) {
                                item {
                                    EventCarousel(events = upcomingEvents, onClick = onOpenEvents)
                                }
                            }
                        } else {
                            item {
                                EventCarousel(events = registeredEvents, onClick = onOpenEvents)
                            }
                        }

                        item {
                            Text(
                                text = "My Joined Clubs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (joinedClubs.isEmpty()) {
                            item {
                                Text(
                                    text = "No joined clubs yet. Join one from the top clubs list.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (topClubs.isNotEmpty()) {
                                item {
                                    ClubCarousel(clubs = topClubs, onClick = onOpenClubs)
                                }
                            }
                        } else {
                            item {
                                ClubCarousel(clubs = joinedClubs, onClick = onOpenClubs)
                            }
                        }
                    }
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    NavigationBar(containerColor = Color.Transparent) {
                        StudentDestination.values().forEach { destination ->
                            NavigationBarItem(
                                selected = selectedDestination == destination,
                                onClick = {
                                    selectedDestination = destination
                                    when (destination) {
                                        StudentDestination.ANNOUNCEMENTS -> onOpenAnnouncements()
                                        StudentDestination.CLUBS -> onOpenClubs()
                                        StudentDestination.EVENTS -> onOpenEvents()
                                        StudentDestination.AI_CHAT -> onOpenAiChat()
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCarousel(
    events: List<Event>,
    onClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(events, key = { it.id }) { event ->
            ElevatedCard(
                modifier = Modifier
                    .width(260.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column {
                    if (event.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = event.imageUrl,
                            contentDescription = event.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = event.title.ifBlank { "Untitled Event" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${event.date} - ${event.venue}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubCarousel(
    clubs: List<Club>,
    onClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(clubs, key = { it.id }) { club ->
            ElevatedCard(
                modifier = Modifier
                    .width(260.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column {
                    if (club.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = club.imageUrl,
                            contentDescription = club.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = club.name.ifBlank { "Unnamed Club" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = club.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private enum class StudentDestination(
    val label: String,
    val icon: ImageVector
) {
    ANNOUNCEMENTS("Announcements", Icons.Default.Announcement),
    CLUBS("Clubs", Icons.Default.Groups),
    EVENTS("Events", Icons.Default.Event),
    AI_CHAT("AI Chat", Icons.Default.SmartToy)
}

private fun eventDateMillis(event: Event): Long {
    val raw = event.date.trim()
    if (raw.isBlank()) return event.createdAt?.toDate()?.time ?: Long.MAX_VALUE

    val formats = listOf(
        "MMM d, h:mm a",
        "MMM d, yyyy h:mm a",
        "MMM d, yyyy, h:mm a",
        "MMM d h:mm a",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd"
    )

    formats.forEach { pattern ->
        val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = true }
        try {
            val parsed = parser.parse(raw) ?: return@forEach
            return adjustYearIfMissing(parsed, pattern)
        } catch (_: ParseException) {
        }
    }

    return event.createdAt?.toDate()?.time ?: Long.MAX_VALUE
}

private fun adjustYearIfMissing(date: Date, pattern: String): Long {
    if (!pattern.contains("yyyy")) return date.time
    return date.time
}
