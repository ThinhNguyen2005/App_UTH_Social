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
    val status: String = "pending", // "pending", "reviewed", "resolved", "dismissed"
    // Admin action fields
    val reviewedBy: String? = null, // Admin user ID who reviewed
    val reviewedAt: Timestamp? = null,
    val adminAction: AdminAction? = null, // Action taken by admin
    val adminNotes: String? = null // Admin's review notes
) {
    constructor() : this("", "", "", "", "", null, "pending", null, null, null, null)
}

enum class AdminAction {
    NONE,           // No action taken
    DISMISS,        // Dismiss report as invalid
    WARN_USER,      // Send warning to user
    DELETE_POST,    // Delete the reported post
    BAN_USER,       // Ban the user who posted
    BAN_REPORTER    // Ban the user who made invalid reports
}

data class AdminReport(
    val report: Report,
    val post: Post? = null,           // Post being reported
    val reporter: User? = null,       // User who made the report
    val reportedUser: User? = null    // User who owns the reported post
)

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val isBanned: Boolean = false,
    val bannedAt: Timestamp? = null,
    val bannedBy: String? = null,
    val banReason: String? = null,
    val violationCount: Int = 0, // Number of times posts were deleted
    val warningCount: Int = 0   // Number of warnings issued
)