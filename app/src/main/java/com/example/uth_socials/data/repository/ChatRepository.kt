package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.chat.Message
import com.example.uth_socials.ui.screen.chat.ChatSummary
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    fun getChatsFlow(userId: String): Flow<List<ChatSummary>> = callbackFlow {
        val listener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Map dữ liệu thô từ Firestore
                    val chats = snapshot.documents.map { doc ->
                        val lastMsg = doc.getString("lastMessage") ?: ""
                        val lastSenderId = doc.getString("lastSenderId") ?: ""
                        val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                        val participants = doc.get("participants") as? List<*> ?: emptyList<Any>()
                        val otherUserId = participants.firstOrNull { it != userId }?.toString() ?: ""

                        // Lưu ý: Tạm thời để name/avatar rỗng, ViewModel sẽ điền sau (để tránh gọi async trong listener)
                        ChatSummary(
                            id = doc.id,
                            userId = otherUserId,
                            userName = "Đang tải...",
                            lastMessage = lastMsg,
                            lastSenderId = lastSenderId,
                            avatarUrl = "",
                            timestamp = timestamp
                        )
                    }
                    trySend(chats)
                }
            }
        awaitClose { listener.remove() } // Tự động hủy listener
    }

    // 2. Flow cho Tin nhắn chi tiết
    fun getMessagesFlow(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() } // Tự động hủy listener
    }
    suspend fun getExistingChatId(targetUserId: String): String? {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val chatId = if (currentUserId < targetUserId)
            "${currentUserId}_${targetUserId}"
        else
            "${targetUserId}_${currentUserId}"

        val snapshot = db.collection("chats").document(chatId).get().await()
        return if (snapshot.exists()) chatId else null
    }

    fun buildChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
}
