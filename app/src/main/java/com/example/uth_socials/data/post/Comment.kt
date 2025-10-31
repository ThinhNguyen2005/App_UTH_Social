package com.example.uth_socials.data.post


data class Comment(
    val id: String = "",
    val userId: String = "",
    val username: String = "Unknown User",
    val userAvatarUrl: String = "",
    val text: String = "",
    val timestamp: com.google.firebase.Timestamp? = null, // Phải là com.google.firebase.Timestamp
    val likedBy: List<String> = emptyList(),
    val likes: Int = 0,
    val isLiked: Boolean = false
)