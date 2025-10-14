package com.example.uth_socials.data.post

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val textContent: String = "",
    val imageUrls: List<String> = emptyList(),
    val likes: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false
)