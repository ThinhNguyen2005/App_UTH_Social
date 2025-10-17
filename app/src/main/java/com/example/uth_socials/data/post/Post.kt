package com.example.uth_socials.data.post

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    // Dùng @get:JvmField và @ServerTimestamp để Firestore tự động điền thời gian
    // khi tạo document mới.
    @ServerTimestamp
    val timestamp: Timestamp? = null,

    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val textContent: String = "",
    val imageUrls: List<String> = emptyList(),

    // Các trường dùng để lọc và tương tác
    val category: String = "",
    val likes: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val saveCount: Int = 0,


    // --- SỬA LỖI Ở ĐÂY ---
    // Thêm trường này để lưu danh sách UID của những người đã thích.
    val likedBy: List<String> = emptyList(),

    // Trường này chỉ dùng cho UI, không lưu trên Firestore.
    // Dùng @get:JvmField @Transient để Firestore bỏ qua.
//    @get:JvmField
//    @Transient
    val isLiked: Boolean = false,
    val isSaved: Boolean? = false
) {
    // Thêm một constructor rỗng mà Firestore yêu cầu để có thể toObject()
    constructor() : this(null, "", "", "", "", "", emptyList(), "", 0, 0, 0,0,emptyList(), false)
}