package com.example.uth_socials.data.post


import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val parentname: String = "",
    val parentId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val likedBy: List<String> = emptyList(),
    val likes: Int = 0,
    val liked: Boolean = false
)