package com.example.campusconnect.events

import com.google.firebase.Timestamp

data class Event(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val venue: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
)
