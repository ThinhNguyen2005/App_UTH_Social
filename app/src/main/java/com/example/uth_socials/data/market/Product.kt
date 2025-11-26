package com.example.uth_socials.data.market

import com.google.firebase.firestore.ServerTimestamp
import java.sql.Timestamp

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    val imageUrls: List<String> = emptyList(),
    val description: String = "",
    val type: String = "", // "Sách", "Đồ điện tử", etc.
    val userId: String = "",

    // Interaction data
    val likedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList(),
    val saves: Long = 0,
    val shares: Long = 0,

    // Timestamp
    @ServerTimestamp
    val timestamp: Timestamp? = null,

    val campus: String? = null,
    val userName: String? = null,
    val userAvatar: String? = null
)