package com.example.uth_socials.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {

    // Hiển thị giờ gửi khi nhấn vào bong bóng
    fun formatTime(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val date = timestamp.toDate()
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    // Quyết định khi nào hiển thị header mới
    fun shouldShowTimeHeader(prevTime: Long, currTime: Long): Boolean {
        if (prevTime == 0L) return true
        val diff = currTime - prevTime
        val fiveMinutes = 5 * 60 * 1000
        val prevDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(prevTime))
        val currDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(currTime))
        return diff > fiveMinutes || prevDay != currDay
    }

    // Hiển thị nội dung header theo logic Messenger
    fun formatTimeHeader(timestamp: Long): String {
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val diffDays = TimeUnit.MILLISECONDS.toDays(now.timeInMillis - msgCal.timeInMillis)

        return when {
            diffDays == 0L -> {
                // Cùng ngày
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(msgCal.time)
            }
            diffDays < 7 -> {
                // Trong 7 ngày
                SimpleDateFormat("EEEE, HH:mm", Locale.getDefault()).format(msgCal.time)
            }
            else -> {
                // Hơn 1 tuần
                SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).format(msgCal.time)
            }
        }
    }
}
