package com.example.uth_socials.data.chat

import com.google.firebase.Timestamp


data class Message(
    val id: String ="",
    val text: String = "",
    val senderId: String = "",
    val timestamp : Timestamp? = null,
    val seen: Boolean=false
)