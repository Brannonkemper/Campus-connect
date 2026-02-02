package com.example.campusconnect.clubs

import com.google.firebase.Timestamp

data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
)
