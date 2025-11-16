package com.example.uth_socials.data.notification

import android.icu.text.CaseMap
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class Notification(
    val id: String = "",

    var category: String = "",
    var userId : String = "",
    var username : String = "",
    var avatarUrl: String = "",

    var title : String = "",
    var body : String = "",

    var isRead : Boolean = false,

    @ServerTimestamp
    val timestamp: Timestamp? = null,
    //val timestampMillis: Long = System.currentTimeMillis()
)