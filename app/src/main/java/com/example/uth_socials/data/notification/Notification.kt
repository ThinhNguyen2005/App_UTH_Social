package com.example.uth_socials.data.notification

import android.icu.text.CaseMap
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class Notification(
    val id: String = "",

    val category: String = "",
    val userId : String = "",
    val username : String = "",
    val avatarUrl: String = "",
    val receiverId: String = "",

    val title : String = "",
    val body : String = "",

    val isRead : Boolean = false,

    @ServerTimestamp
    val timestamp: Timestamp? = null
)