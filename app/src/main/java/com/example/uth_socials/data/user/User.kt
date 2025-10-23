package com.example.uth_socials.data.user

data class User(
    val username: String = "",
    val avatarUrl: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val bio: String = "",
    val id: String = ""
)