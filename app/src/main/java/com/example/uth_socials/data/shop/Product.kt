package com.example.uth_socials.data.shop

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Product(
    val id: String? = null,
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null, //Link image in Firebase Storage
    val description: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val userAvatar: String? = null,

    @ServerTimestamp // Báo cho Firestore tự động gán thời gian tạo document vào biến này
    val createdAt: Date? = null,
    val favorite: Boolean = false
)
