package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.notification.Notification
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.Post
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map


class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = Firebase.storage
    private val auth = FirebaseAuth.getInstance()

    private val notificationsCollection = db.collection("notifications")

    fun listenNotificationsChanged(): Flow<List<Notification>> = callbackFlow {

        val snapshot = notificationsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)

        // Lắng nghe thay đổi thời gian thực
        val listener = snapshot.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("NotificationRepository", "Firestore Error", error)
                return@addSnapshotListener
            }

            Log.d(
                "NotificationRepository",
                "fromCache=${snapshot?.metadata?.isFromCache} size=${snapshot?.documents?.size}"
            )
            if (snapshot != null) {
                // Emit raw documents to be processed on background thread
                trySend(snapshot.documents)
            }
        }

        // Khi Flow bị hủy (ví dụ: ViewModel bị destroy), gỡ listener
        awaitClose { listener.remove() }
    }.map { documents ->
        documents.mapNotNull { doc ->
            doc.toObject(Notification::class.java)
        }
    }.flowOn(Dispatchers.IO)


    suspend fun markAsRead(id: String) : Boolean{
        return try {
            notificationsCollection.document(id).update("isRead", true).await()
            true
        }catch (e : Exception) {
            Log.e("NotificationRepository", "Error updating notification", e)
            false
        }
    }

    suspend fun getNotifications() : List<Notification>{
        return try {
            val snapshot = notificationsCollection.orderBy("timestamp", Query.Direction.DESCENDING).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)
            }
        }catch (e : Exception) {
            Log.e("NotificationRepository", "Error getting notification", e)
            emptyList<Notification>()
        }
    }

    suspend fun getNotReadNotification() : List<Notification> {
        return try {
            val snapshot = notificationsCollection.whereEqualTo("isRead", false).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)
            }
        }catch (e : Exception) {
            Log.e("NotificationRepository", "Error getting notification", e)
            emptyList<Notification>()
        }
    }

    suspend fun deleteNotification(id: String) : Boolean {
        return  try {
            notificationsCollection.document(id).delete().await()
            true
        }catch (e : Exception){
            Log.e("NotificationRepository", "Error deleting notification", e)
            false
        }
    }

}

