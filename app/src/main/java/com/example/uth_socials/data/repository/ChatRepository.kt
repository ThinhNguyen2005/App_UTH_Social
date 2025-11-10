package com.example.uth_socials.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getExistingChatId(targetUserId: String): String? {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val chatId = if (currentUserId < targetUserId)
            "${currentUserId}_${targetUserId}"
        else
            "${targetUserId}_${currentUserId}"

        val snapshot = db.collection("chats").document(chatId).get().await()
        return if (snapshot.exists()) chatId else null
    }

    suspend fun createNewChatIfNeeded(chatId: String, targetUserId: String, firstMessage: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatRef = db.collection("chats").document(chatId)
        val snapshot = chatRef.get().await()

        if (!snapshot.exists()) {
            val newChat = hashMapOf(
                "participants" to listOf(currentUserId, targetUserId),
                "lastMessage" to firstMessage,
                "lastSenderId" to currentUserId,
                "timestamp" to Timestamp.now()
            )
            chatRef.set(newChat).await()
            Log.d("ChatDebug", "🆕 Created new chat with first message: $firstMessage")
        }
    }

    fun buildChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
}
