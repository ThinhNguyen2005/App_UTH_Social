package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.chat.Message
import com.example.uth_socials.ui.screen.chat.ChatSummary
import kotlinx.coroutines.launch


import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chats = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    fun listenToMessages(chatId: String) {
        listenerRegistration?.remove() // tránh leak listener cũ

        listenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val msgList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }

                viewModelScope.launch {
                    _messages.value = msgList
                }
            }
    }
    fun listenToChatList(currentUserId: String) {
        _isLoading.value = true

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    val chatList = snapshot.documents.mapNotNull { doc ->
                        val lastMsg = doc.getString("lastMessage") ?: ""
                        val lastSenderId = doc.getString("lastSenderId") ?: ""
                        val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                        val participants = doc.get("participants") as? List<*> ?: return@mapNotNull null
                        val otherUserId = participants.firstOrNull { it != currentUserId }?.toString() ?: return@mapNotNull null


                        val userDoc = db.collection("users").document(otherUserId).get().await()

                        val userName = userDoc.getString("username") ?: "Người dùng"
                        val avatar = userDoc.getString("avatarUrl") ?: ""

                        ChatSummary(
                            id = doc.id,
                            userId = otherUserId,
                            userName = userName,
                            lastMessage = if (lastSenderId == currentUserId) "Bạn: $lastMsg" else "$userName: $lastMsg",
                            lastSenderId = lastSenderId,
                            avatarUrl = avatar,
                            timestamp = timestamp
                        )
                    }
                    _chats.value = chatList.sortedByDescending { it.timestamp }
                    _isLoading.value = false
                }

            }
    }
    fun sendMessage(chatId: String, senderId: String, text: String) {
        Log.d("ChatDebug", "▶️ sendMessage called: chatId=$chatId, sender=$senderId, text=$text")

        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("chats").document(chatId)
        val message = hashMapOf(
            "senderId" to senderId,
            "text" to text,
            "timestamp" to Timestamp.now(),
            "seen" to false
        )

        viewModelScope.launch {
            try {
                // 🟢 Thêm message vào subcollection
                chatRef.collection("messages").add(message).await()

                // 🟢 Cập nhật thông tin chat (last message, last sender, timestamp)
                chatRef.update(
                    mapOf(
                        "lastMessage" to text,
                        "lastSenderId" to senderId,
                        "timestamp" to Timestamp.now()
                    )
                ).await()

                Log.d("ChatDebug", "✅ Message sent successfully.")
            } catch (e: Exception) {
                Log.e("ChatDebug", "❌ Failed to send message", e)
            }
        }
    }



    override fun onCleared(){
        listenerRegistration?.remove()
        super.onCleared()

    }
    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

    suspend fun getOrCreateChatId(targetUserId: String): String? {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val chatId = getChatId(currentUserId, targetUserId)
        val chatRef = db.collection("chats").document(chatId)

        val snapshot = chatRef.get().await()
        if (!snapshot.exists()) {
            val newChat = hashMapOf(
                "participants" to listOf(currentUserId, targetUserId),
                "lastMessage" to "",
                "timestamp" to Timestamp.now()
            )
            chatRef.set(newChat).await()
        }
        return chatId
    }

}