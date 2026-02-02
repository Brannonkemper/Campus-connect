package com.example.campusconnect.clubs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ClubUiState(
    val loading: Boolean = false,
    val error: String? = null
)

data class ClubAnnouncementSummary(
    val id: String = "",
    val clubId: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Timestamp? = null
)

class ClubViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _ui = MutableStateFlow(ClubUiState())
    val ui: StateFlow<ClubUiState> = _ui

    private val _clubs = MutableStateFlow<List<Club>>(emptyList())
    val clubs: StateFlow<List<Club>> = _clubs

    private val _members = MutableStateFlow<List<ClubMember>>(emptyList())
    val members: StateFlow<List<ClubMember>> = _members

    private val _myClubIds = MutableStateFlow<Set<String>>(emptySet())
    val myClubIds: StateFlow<Set<String>> = _myClubIds

    private val _clubAnnouncements = MutableStateFlow<List<ClubAnnouncement>>(emptyList())
    val clubAnnouncements: StateFlow<List<ClubAnnouncement>> = _clubAnnouncements

    private val _recentClubAnnouncements = MutableStateFlow<List<ClubAnnouncementSummary>>(emptyList())
    val recentClubAnnouncements: StateFlow<List<ClubAnnouncementSummary>> = _recentClubAnnouncements

    private var startedClubs = false
    private var membersClubId: String? = null
    private var announcementsClubId: String? = null
    private var startedMyClubs = false
    private var startedRecentAnnouncements = false
    private val recentAnnouncementListeners = mutableMapOf<String, ListenerRegistration>()

    fun listenClubs() {
        if (startedClubs) return
        startedClubs = true
        _ui.value = ClubUiState(loading = true)

        db.collection("clubs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _ui.value = ClubUiState(loading = false, error = e.message)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { doc ->
                    Club(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                } ?: emptyList()

                _clubs.value = list
                _ui.value = ClubUiState(loading = false)
            }
    }

    fun listenMyClubs() {
        val uid = auth.currentUser?.uid ?: return
        if (startedMyClubs) return
        startedMyClubs = true

        db.collection("users")
            .document(uid)
            .collection("clubs")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _ui.value = _ui.value.copy(error = e.message)
                    return@addSnapshotListener
                }

                val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                _myClubIds.value = ids
            }
    }

    fun listenMembers(clubId: String) {
        if (membersClubId == clubId) return
        membersClubId = clubId
        _ui.value = ClubUiState(loading = true)

        db.collection("clubs")
            .document(clubId)
            .collection("members")
            .orderBy("joinedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _ui.value = ClubUiState(loading = false, error = e.message)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { doc ->
                    ClubMember(
                        uid = doc.getString("uid") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        joinedAt = doc.getTimestamp("joinedAt")
                    )
                } ?: emptyList()

                _members.value = list
                _ui.value = ClubUiState(loading = false)
            }
    }

    fun listenClubAnnouncements(clubId: String) {
        if (announcementsClubId == clubId) return
        announcementsClubId = clubId
        _ui.value = ClubUiState(loading = true)

        db.collection("clubs")
            .document(clubId)
            .collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _ui.value = ClubUiState(loading = false, error = e.message)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.map { doc ->
                    ClubAnnouncement(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                } ?: emptyList()

                _clubAnnouncements.value = list
                _ui.value = ClubUiState(loading = false)
            }
    }

    fun listenRecentClubAnnouncements(limit: Long = 20) {
        if (startedRecentAnnouncements) return
        startedRecentAnnouncements = true

        db.collectionGroup("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _ui.value = _ui.value.copy(error = e.message)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.mapNotNull { doc ->
                    val clubId = doc.reference.parent.parent?.id ?: return@mapNotNull null
                    ClubAnnouncementSummary(
                        id = doc.id,
                        clubId = clubId,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()

                _recentClubAnnouncements.value = list
            }
    }

    fun listenRecentClubAnnouncementsForMyClubs(limitPerClub: Long = 5) {
        if (startedRecentAnnouncements) return
        startedRecentAnnouncements = true

        listenMyClubs()

        viewModelScope.launch {
            _myClubIds.collect { clubIds ->
                val toRemove = recentAnnouncementListeners.keys - clubIds
                toRemove.forEach { clubId ->
                    recentAnnouncementListeners.remove(clubId)?.remove()
                }

                clubIds.forEach { clubId ->
                    if (recentAnnouncementListeners.containsKey(clubId)) return@forEach

                    val registration = db.collection("clubs")
                        .document(clubId)
                        .collection("announcements")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limitPerClub)
                        .addSnapshotListener { snap, e ->
                            if (e != null) {
                                _ui.value = _ui.value.copy(error = e.message)
                                return@addSnapshotListener
                            }

                            val incoming = snap?.documents?.map { doc ->
                                ClubAnnouncementSummary(
                                    id = doc.id,
                                    clubId = clubId,
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    createdAt = doc.getTimestamp("createdAt")
                                )
                            } ?: emptyList()

                            val withoutClub = _recentClubAnnouncements.value.filter { it.clubId != clubId }
                            _recentClubAnnouncements.value = (withoutClub + incoming)
                                .sortedByDescending { it.createdAt }
                        }

                    recentAnnouncementListeners[clubId] = registration
                }
            }
        }
    }

    fun createClub(
        name: String,
        description: String,
        imageUrl: String,
        onDone: () -> Unit
    ) {
        if (name.isBlank() || description.isBlank()) {
            _ui.value = ClubUiState(error = "Name and description are required")
            return
        }

        val uid = auth.currentUser?.uid ?: run {
            _ui.value = ClubUiState(error = "Not logged in")
            return
        }

        _ui.value = ClubUiState(loading = true)

        val data = hashMapOf(
            "name" to name.trim(),
            "description" to description.trim(),
            "imageUrl" to imageUrl.trim(),
            "createdAt" to Timestamp.now(),
            "createdBy" to uid
        )

        db.collection("clubs")
            .add(data)
            .addOnSuccessListener {
                _ui.value = ClubUiState(loading = false)
                onDone()
            }
            .addOnFailureListener { e ->
                _ui.value = ClubUiState(loading = false, error = e.message)
            }
    }

    fun updateClub(
        id: String,
        name: String,
        description: String,
        imageUrl: String,
        onDone: () -> Unit
    ) {
        if (name.isBlank() || description.isBlank()) {
            _ui.value = ClubUiState(error = "Name and description are required")
            return
        }

        _ui.value = ClubUiState(loading = true)

        val data = mapOf<String, Any>(
            "name" to name.trim(),
            "description" to description.trim(),
            "imageUrl" to imageUrl.trim()
        )

        db.collection("clubs")
            .document(id)
            .update(data)
            .addOnSuccessListener {
                _ui.value = ClubUiState(loading = false)
                onDone()
            }
            .addOnFailureListener { e ->
                _ui.value = ClubUiState(loading = false, error = e.message)
            }
    }

    fun deleteClub(id: String) {
        _ui.value = ClubUiState(loading = true)

        db.collection("clubs")
            .document(id)
            .delete()
            .addOnSuccessListener {
                _ui.value = ClubUiState(loading = false)
            }
            .addOnFailureListener { e ->
                _ui.value = ClubUiState(loading = false, error = e.message)
            }
    }

    fun joinClub(clubId: String) {
        val uid = auth.currentUser?.uid ?: run {
            _ui.value = ClubUiState(error = "Not logged in")
            return
        }

        _ui.value = ClubUiState(loading = true)

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: ""
                val email = doc.getString("email") ?: ""
                val member = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "joinedAt" to Timestamp.now()
                )

                val memberRef = db.collection("clubs")
                    .document(clubId)
                    .collection("members")
                    .document(uid)

                val userClubRef = db.collection("users")
                    .document(uid)
                    .collection("clubs")
                    .document(clubId)

                memberRef.set(member)
                    .continueWithTask { userClubRef.set(mapOf("joinedAt" to Timestamp.now())) }
                    .addOnSuccessListener {
                        _ui.value = ClubUiState(loading = false)
                    }
                    .addOnFailureListener { e ->
                        _ui.value = ClubUiState(loading = false, error = e.message)
                    }
            }
            .addOnFailureListener { e ->
                _ui.value = ClubUiState(loading = false, error = e.message)
            }
    }

    fun leaveClub(clubId: String) {
        val uid = auth.currentUser?.uid ?: run {
            _ui.value = ClubUiState(error = "Not logged in")
            return
        }

        _ui.value = ClubUiState(loading = true)

        val memberRef = db.collection("clubs")
            .document(clubId)
            .collection("members")
            .document(uid)

        val userClubRef = db.collection("users")
            .document(uid)
            .collection("clubs")
            .document(clubId)

        memberRef.delete()
            .continueWithTask { userClubRef.delete() }
            .addOnSuccessListener {
                _ui.value = ClubUiState(loading = false)
            }
            .addOnFailureListener { e ->
                _ui.value = ClubUiState(loading = false, error = e.message)
            }
    }

    fun postClubAnnouncement(
        clubId: String,
        title: String,
        message: String,
        onDone: () -> Unit
    ) {
        if (title.isBlank() || message.isBlank()) {
            _ui.value = ClubUiState(error = "Title and message are required")
            return
        }

        val uid = auth.currentUser?.uid ?: run {
            _ui.value = ClubUiState(error = "Not logged in")
            return
        }

        _ui.value = ClubUiState(loading = true)

        val data = hashMapOf(
            "title" to title.trim(),
            "message" to message.trim(),
            "createdAt" to Timestamp.now(),
            "createdBy" to uid
        )

        db.collection("clubs")
            .document(clubId)
            .collection("announcements")
            .add(data)
            .addOnSuccessListener {
                _ui.value = ClubUiState(loading = false)
                onDone()
            }
            .addOnFailureListener { e ->
                _ui.value = ClubUiState(loading = false, error = e.message)
            }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}
