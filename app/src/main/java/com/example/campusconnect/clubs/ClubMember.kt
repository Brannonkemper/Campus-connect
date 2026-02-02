package com.example.campusconnect.clubs

import com.google.firebase.Timestamp

data class ClubMember(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val joinedAt: Timestamp? = null
)
