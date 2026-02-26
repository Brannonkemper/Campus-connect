package com.example.campusconnect.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusconnect.clubs.ClubAnalytics
import com.example.campusconnect.clubs.ClubViewModel
import com.example.campusconnect.events.EventViewModel
import com.example.campusconnect.ui.components.SoftBackground
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsAnalyticsScreen(
    onBack: () -> Unit,
    vm: ClubViewModel,
    eventVm: EventViewModel
) {
    val ui by vm.ui.collectAsState()
    val analytics by vm.clubAnalytics.collectAsState()
    val events by eventVm.events.collectAsState()

    LaunchedEffect(Unit) {
        vm.listenClubAnalytics()
        eventVm.listenEvents()
    }

    val totalMembers = analytics.sumOf { it.memberCount }
    val totalAnnouncements = analytics.sumOf { it.announcementCount }
    val totalEventRegistrations = events.sumOf { it.registrationCount }
    val topByMembers = analytics.maxByOrNull { it.memberCount }
    val topByAnnouncements = analytics.maxByOrNull { it.announcementCount }
    val topByEventRegistrations = events.maxByOrNull { it.registrationCount }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reports & Analytics") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Club Performance Overview",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Track membership, participation, and content activity across all clubs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    SectionHeader("Summary")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Total Clubs",
                            value = analytics.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Total Members",
                            value = totalMembers.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Total Posts",
                            value = totalAnnouncements.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Total Events",
                            value = events.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Event Registrations",
                            value = totalEventRegistrations.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    SectionHeader("Highlights")
                    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Top Clubs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Most members: ${topByMembers?.clubName ?: "-"} (${topByMembers?.memberCount ?: 0})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Most announcements: ${topByAnnouncements?.clubName ?: "-"} (${topByAnnouncements?.announcementCount ?: 0})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Most registered event: ${topByEventRegistrations?.title ?: "-"} (${topByEventRegistrations?.registrationCount ?: 0})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                item {
                    SectionHeader("Club Breakdown")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                if (ui.loading && analytics.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (!ui.loading && analytics.isEmpty()) {
                    item {
                        Text(
                            text = "No club analytics yet. Create clubs and member activity to generate reports.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(analytics, key = { it.clubId }) { item ->
                    ClubAnalyticsCard(
                        analytics = item,
                        totalMembers = totalMembers
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader("Event Registrations")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                if (events.isEmpty()) {
                    item {
                        Text(
                            text = "No events yet. Create events to see registration analytics.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(events, key = { it.id }) { event ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = event.title.ifBlank { "Untitled Event" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${event.date} at ${event.venue}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Registered students: ${event.registrationCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClubAnalyticsCard(
    analytics: ClubAnalytics,
    totalMembers: Int
) {
    val participationPercent = if (totalMembers == 0) {
        0f
    } else {
        analytics.memberCount.toFloat() / totalMembers.toFloat()
    }

    val postsPerTenMembers = if (analytics.memberCount == 0) {
        0f
    } else {
        analytics.announcementCount.toFloat() / analytics.memberCount.toFloat() * 10f
    }

    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = analytics.clubName.ifBlank { "Unnamed Club" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${analytics.memberCount} members - ${analytics.announcementCount} announcements",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "New members (7d): ${analytics.recentJoins7Days}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Participation share: ${"%.1f".format(Locale.US, participationPercent * 100f)}%",
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { participationPercent },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Engagement: ${"%.1f".format(Locale.US, postsPerTenMembers)} posts / 10 members",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Last announcement: ${formatTimestamp(analytics.lastAnnouncementAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Latest member joined: ${formatTimestamp(analytics.lastMemberJoinAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "No data"
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(timestamp.toDate())
}
