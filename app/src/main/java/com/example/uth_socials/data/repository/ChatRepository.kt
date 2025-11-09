package com.example.uth_socials.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getOrCreateChatId(targetUserId: String): String? {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val chatId = if (currentUserId < targetUserId)
            "${currentUserId}_${targetUserId}"
        else
            "${targetUserId}_${currentUserId}"

        val chatRef = db.collection("chats").document(chatId)
        val snapshot = chatRef.get().await()
        if (!snapshot.exists()) {
            val newChat = hashMapOf(
                "participants" to listOf(currentUserId, targetUserId),
                "lastMessage" to "",
                "lastSenderId" to "",
                "timestamp" to Timestamp.now()
            )
            chatRef.set(newChat).await()
        }
        return chatId
    }
}
