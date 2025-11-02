package com.example.uth_socials.data.user

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class User(
    var username: String = "",
    var avatarUrl: String = "",
    var followers: List<String> = emptyList(),
    var following: List<String> = emptyList(),
    var hiddenPosts: List<String> = emptyList(),
    var blockedUsers: List<String> = emptyList(),
    var bio: String = "",
    var userId: String? = null,
    var id: String = ""
)