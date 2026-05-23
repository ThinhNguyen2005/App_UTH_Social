package com.example.uth_socials.services

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.uth_socials.BuildConfig
import com.example.uth_socials.R
import com.example.uth_socials.data.notification.Notification
import com.example.uth_socials.ui.component.navigation.Screen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Cập nhật token vào Firestore
        Firebase.firestore.collection("users")
            .document(userId)
            .update("token", token)
            .addOnSuccessListener {
                if (BuildConfig.DEBUG) {
                    Log.d("FCM", "Token đã được lưu cho user (uid=***)")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Lỗi khi lưu token", e)
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (BuildConfig.DEBUG) {
            Log.d("FCM", "Thông báo nhận được: ${remoteMessage.data}")
        }


        remoteMessage.notification?.let {
            val title = it.title ?: "Thông báo mới"
            val body = it.body ?: ""

            // 🔹 Tạo notification
            val builder = NotificationCompat.Builder(this, "default_channel")
                .setSmallIcon(R.drawable.lg_uth) // Đảm bảo icon này tồn tại!
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            // 🔹 Hiển thị thông báo (mỗi notification có id riêng để không đè lên nhau)
            val manager = NotificationManagerCompat.from(this)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val notificationId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
                manager.notify(notificationId, builder.build())
            } else {
                Log.w("FCM", "Chưa có quyền POST_NOTIFICATIONS – không thể hiển thị thông báo.")
            }
        }

    }
}