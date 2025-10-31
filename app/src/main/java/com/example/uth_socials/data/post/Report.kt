package com.example.uth_socials.data.post

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Report(
    val id: String = "",
    val postId: String = "",
    val reportedBy: String = "",
    val reason: String = "",
    val description: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val status: String = "pending" // "pending", "reviewed", "resolved"
) {
    constructor() : this("", "", "", "", "", null, "pending")
}



