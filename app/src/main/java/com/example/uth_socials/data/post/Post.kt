package com.example.uth_socials.data.post

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Immutable
data class Post(
    @ServerTimestamp
    val timestamp: Timestamp? = null,

    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    var textContent: String = "",
    val imageUrls: List<String> = emptyList(),

    // Các trường dùng để lọc và tương tác
    val category: String? = "",
    val likes: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val saveCount: Int = 0,

    // Thêm trường này để lưu danh sách UID của những người đã thích.
    val likedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList(),

    @get:Exclude
    @PropertyName("isLiked")  // ✅ Map Firebase field 'isLiked' (or 'liked' if DB has it)
    val isLiked: Boolean = false,

    @get:Exclude
    @PropertyName("isSaved")  // ✅ Map Firebase field 'isSaved' (or 'saved' if DB has it)
    val isSaved: Boolean = false
) {
    constructor() : this(null, "", "", "", "", "", emptyList(), "", 0, 0, 0,0,emptyList(), emptyList(),false)
}