package com.example.campusconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect.announcements.AdminPostAnnouncementScreen
import com.example.campusconnect.announcements.AnnouncementViewModel
import com.example.campusconnect.announcements.StudentAnnouncementsScreen
import com.example.campusconnect.ai.StudentAiChatScreen
import com.example.campusconnect.ai.AiChatViewModel
import com.example.campusconnect.auth.AuthViewModel
import com.example.campusconnect.auth.LoginScreen
import com.example.campusconnect.auth.RegisterScreen
import com.example.campusconnect.clubs.AdminClubsScreen
import com.example.campusconnect.clubs.ClubAnnouncementsScreen
import com.example.campusconnect.clubs.ClubViewModel
import com.example.campusconnect.clubs.StudentClubsScreen
import com.example.campusconnect.dashboard.AdminDashboard
import com.example.campusconnect.dashboard.AdminReportsAnalyticsScreen
import com.example.campusconnect.dashboard.StudentDashboard
import com.example.campusconnect.events.AdminEventsScreen
import com.example.campusconnect.events.EventViewModel
import com.example.campusconnect.events.StudentEventsScreen
import com.example.campusconnect.ui.theme.CampusConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CampusConnectTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf("login") }
    var selectedClubId by remember { mutableStateOf("") }
    var selectedClubName by remember { mutableStateOf("") }
    val authViewModel: AuthViewModel = viewModel()

    val announcementVm: AnnouncementViewModel = viewModel()
    val eventVm: EventViewModel = viewModel()
    val clubVm: ClubViewModel = viewModel()
    val aiChatVm: AiChatViewModel = viewModel()

    LaunchedEffect(Unit) {
        authViewModel.tryRestoreSession()
    }

    when (screen) {
        "login" -> LoginScreen(
            onGoRegister = { screen = "register" },
            onLoginSuccess = { role ->
                screen = if (role == "admin") "admin" else "student"
            },
            vm = authViewModel
        )

        "register" -> RegisterScreen(
            onGoLogin = { screen = "login" },
            onRegisterSuccess = { role ->
                screen = if (role == "admin") "admin" else "student"
            },
            vm = authViewModel
        )

        "student" -> StudentDashboard(
            onOpenAnnouncements = { screen = "student_announcements" },
            onOpenEvents = { screen = "student_events" },
            onOpenClubs = { screen = "student_clubs" },
            onOpenAiChat = { screen = "student_ai_chat" },
            onLogout = {
                authViewModel.logout()
                screen = "login"
            },
            eventVm = eventVm,
            clubVm = clubVm
        )

        "admin" -> AdminDashboard(
            onOpenPostAnnouncement = { screen = "admin_post_announcement" },
            onOpenEvents = { screen = "admin_events" },
            onOpenClubs = { screen = "admin_clubs" },
            onOpenReports = { screen = "admin_reports" },
            onLogout = {
                authViewModel.logout()
                screen = "login"
            }
        )

        "admin_reports" -> AdminReportsAnalyticsScreen(
            onBack = { screen = "admin" },
            vm = clubVm,
            eventVm = eventVm
        )

        "admin_post_announcement" -> AdminPostAnnouncementScreen(
            onBack = { screen = "admin" },
            vm = announcementVm
        )

        "student_announcements" -> StudentAnnouncementsScreen(
            onBack = { screen = "student" },
            vm = announcementVm
        )

        "student_events" -> StudentEventsScreen(
            onBack = { screen = "student" },
            vm = eventVm
        )

        "admin_events" -> AdminEventsScreen(
            onBack = { screen = "admin" },
            vm = eventVm
        )

        "admin_clubs" -> AdminClubsScreen(
            onBack = { screen = "admin" },
            onOpenClubAnnouncements = { clubId, clubName ->
                selectedClubId = clubId
                selectedClubName = clubName
                screen = "admin_club_announcements"
            },
            vm = clubVm
        )

        "student_clubs" -> StudentClubsScreen(
            onBack = { screen = "student" },
            onOpenClubAnnouncements = { clubId, clubName ->
                selectedClubId = clubId
                selectedClubName = clubName
                screen = "student_club_announcements"
            },
            vm = clubVm
        )

        "student_ai_chat" -> StudentAiChatScreen(
            onBack = { screen = "student" },
            announcementVm = announcementVm,
            eventVm = eventVm,
            clubVm = clubVm,
            aiChatVm = aiChatVm
        )

        "admin_club_announcements" -> ClubAnnouncementsScreen(
            clubId = selectedClubId,
            clubName = selectedClubName,
            canPost = true,
            isMember = true,
            onBack = { screen = "admin_clubs" },
            vm = clubVm
        )

        "student_club_announcements" -> ClubAnnouncementsScreen(
            clubId = selectedClubId,
            clubName = selectedClubName,
            canPost = false,
            isMember = clubVm.myClubIds.collectAsState().value.contains(selectedClubId),
            onBack = { screen = "student_clubs" },
            vm = clubVm
        )
    }
}
