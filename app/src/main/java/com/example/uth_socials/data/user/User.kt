package com.example.uth_socials.data.user

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class User(
    var username: String = "",
    var avatarUrl: String? = null,
    var followers: List<String> = emptyList(),
    var following: List<String> = emptyList(),
    var hiddenPosts: List<String> = emptyList(),
    var blockedUsers: List<String> = emptyList(),
    var bio: String = "",
    var userId: String? = null,
    var id: String = "",
    var isBanned: Boolean = false,
    var bannedAt: com.google.firebase.Timestamp? = null,
    var bannedBy: String? = null,
    var banReason: String? = null,
    var violationCount: Int = 0,
    var warningCount: Int = 0
)