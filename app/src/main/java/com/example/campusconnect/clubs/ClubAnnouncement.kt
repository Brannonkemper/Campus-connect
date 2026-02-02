package com.example.campusconnect.clubs

import com.google.firebase.Timestamp

data class ClubAnnouncement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
)
