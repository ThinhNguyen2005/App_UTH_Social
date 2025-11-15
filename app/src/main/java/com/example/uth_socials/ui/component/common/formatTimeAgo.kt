package com.example.uth_socials.ui.component.common

import com.google.firebase.Timestamp

fun formatTimeAgo(timestamp: Timestamp?): String {
    // 1. Kiểm tra nếu timestamp là null thì trả về một chuỗi mặc định
    if (timestamp == null) {
        return "Vừa xong" // hoặc "Không rõ"
    }
    val millis = timestamp.toDate().time
    val now = System.currentTimeMillis()
    val seconds = (now - millis) / 1000
    return when {
        seconds < 60 -> "Vừa xong"
        seconds < 3600 -> "${seconds / 60} phút trước"
        seconds < 86400 -> "${seconds / 3600} giờ trước"
        else -> "${seconds / 86400} ngày trước"
    }
}
