

package com.example.campusconnect.announcements

import com.google.firebase.Timestamp

data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Timestamp? = null,
    val createdBy: String = ""
)
