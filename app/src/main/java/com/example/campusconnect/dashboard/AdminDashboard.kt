package com.example.campusconnect.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusconnect.ui.components.SoftBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onOpenPostAnnouncement: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenClubs: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(                title = { Text("Admin Dashboard") },                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    Column {
                        Text(
                            text = "Command Center",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Shape campus activity and keep operations smooth.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DashboardCard(
                        title = "Manage Students",
                        description = "View and manage student accounts",
                        icon = Icons.Default.ManageAccounts
                    )

                    DashboardCard(
                        title = "Post Announcements",
                        description = "Create and publish campus-wide announcements",
                        icon = Icons.Default.Campaign,
                        onClick = onOpenPostAnnouncement
                    )

                    DashboardCard(
                        title = "Manage Events",
                        description = "Create, edit, and delete campus events",
                        icon = Icons.Default.Event,
                        onClick = onOpenEvents
                    )

                    DashboardCard(
                        title = "Manage Clubs",
                        description = "Create clubs and post club announcements",
                        icon = Icons.Default.Groups,
                        onClick = onOpenClubs
                    )

                    DashboardCard(
                        title = "Reports & Analytics",
                        description = "View system usage and activity reports",
                        icon = Icons.Default.Analytics
                    )

                    DashboardCard(
                        title = "System Settings",
                        description = "Configure application preferences",
                        icon = Icons.Default.Settings
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Text("Logout")
                }
            }
        }
    }
}









