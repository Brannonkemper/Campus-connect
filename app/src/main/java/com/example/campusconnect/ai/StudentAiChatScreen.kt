package com.example.campusconnect.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import com.example.campusconnect.BuildConfig
import com.example.campusconnect.announcements.Announcement
import com.example.campusconnect.announcements.AnnouncementViewModel
import com.example.campusconnect.clubs.Club
import com.example.campusconnect.clubs.ClubAnnouncementSummary
import com.example.campusconnect.clubs.ClubViewModel
import com.example.campusconnect.events.Event
import com.example.campusconnect.events.EventViewModel
import com.example.campusconnect.ui.components.SoftBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAiChatScreen(
    onBack: () -> Unit,
    announcementVm: AnnouncementViewModel,
    eventVm: EventViewModel,
    clubVm: ClubViewModel,
    aiChatVm: AiChatViewModel
) {
    val announcements by announcementVm.announcements.collectAsState()
    val events by eventVm.events.collectAsState()
    val clubs by clubVm.clubs.collectAsState()
    val clubAnnouncements by clubVm.recentClubAnnouncements.collectAsState()
    val chatMessages by aiChatVm.messages.collectAsState()
    val chatUi by aiChatVm.ui.collectAsState()

    LaunchedEffect(Unit) {
        announcementVm.listenAnnouncements()
        eventVm.listenEvents()
        clubVm.listenClubs()
        clubVm.listenMyClubs()
        clubVm.listenRecentClubAnnouncementsForMyClubs()
        aiChatVm.listenChat()
    }

    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(                title = { Text("Campus AI Assistant") },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { aiChatVm.clearChat() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Clear")
                    }
                },                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Ask CampusConnect...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                )

                TextButton(
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        if (input.isBlank() || chatUi.loading) return@TextButton
                        val apiKey = BuildConfig.GEMINI_API_KEY
                        Log.d("AiChat", "GEMINI_API_KEY length: ${apiKey.length}")
                        if (apiKey.isBlank()) {
                            Log.e("AiChat", "GEMINI_API_KEY is blank in BuildConfig")
                            aiChatVm.setError("Missing GEMINI_API_KEY in local.properties.")
                            return@TextButton
                        }
                        Log.d("AiChat", "GEMINI_API_KEY loaded, sending request.")
                        val userText = input.trim()
                        input = ""

                        val context = buildCampusContext(
                            announcements = announcements,
                            events = events,
                            clubs = clubs,
                            clubAnnouncements = clubAnnouncements
                        )

                        scope.launch {
                            aiChatVm.sendMessage(
                                userText = userText,
                                campusContext = context,
                                apiKey = apiKey
                            )
                        }
                    }
                ) {
                    Text("Send", fontWeight = FontWeight.SemiBold)
                }
            }
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
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    Text(
                        text = "Ask about clubs, upcoming events, or campus announcements.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages.size) { index ->
                        val message = chatMessages[index]
                        val bubbleColor = if (message.isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                        val textColor = if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.isUser) {
                                Arrangement.End
                            } else {
                                Arrangement.Start
                            }
                        ) {
                            Surface(
                                color = bubbleColor,
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = if (message.isUser) 0.dp else 2.dp
                            ) {
                                Text(
                                    text = message.content,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    if (chatUi.loading) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = "Thinking...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                chatUi.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun buildCampusContext(
    announcements: List<Announcement>,
    events: List<Event>,
    clubs: List<Club>,
    clubAnnouncements: List<ClubAnnouncementSummary>
): String {
    val clubNameById = clubs.associate { it.id to it.name }

    val announcementsText = announcements.take(5).joinToString("\n") { item ->
        "- ${item.title}: ${truncate(item.message)}"
    }.ifBlank { "- None" }

    val eventsText = events.take(5).joinToString("\n") { event ->
        "- ${event.title} on ${event.date} at ${event.venue}: ${truncate(event.description)}"
    }.ifBlank { "- None" }

    val clubsText = clubs.take(6).joinToString("\n") { club ->
        "- ${club.name}: ${truncate(club.description)}"
    }.ifBlank { "- None" }

    val clubAnnouncementsText = clubAnnouncements.take(6).joinToString("\n") { item ->
        val clubName = clubNameById[item.clubId].orEmpty().ifBlank { "Club ${item.clubId}" }
        "- ${clubName}: ${item.title} - ${truncate(item.message)}"
    }.ifBlank { "- None" }

    return """
Announcements:
$announcementsText

Events:
$eventsText

Clubs:
$clubsText

Club announcements:
$clubAnnouncementsText
""".trimIndent()
}

private fun truncate(text: String, max: Int = 180): String {
    val clean = text.trim()
    if (clean.length <= max) return clean
    return clean.take(max - 3) + "..."
}






