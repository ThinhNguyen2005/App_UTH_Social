package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.chat.Message
import com.example.uth_socials.data.repository.ChatRepository
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.ui.screen.chat.ChatSummary
import kotlinx.coroutines.launch


import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
    
    private val _showBanDialog = MutableStateFlow(false)
    val showBanDialog = _showBanDialog.asStateFlow()

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
                        val participants =
                            doc.get("participants") as? List<*> ?: return@mapNotNull null
                        val otherUserId =
                            participants.firstOrNull { it != currentUserId }?.toString()
                                ?: return@mapNotNull null


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
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            // Check ban status trước khi gửi tin nhắn
            val userRepository = UserRepository()
            val user = userRepository.getUser(senderId)
            if (user?.isBanned == true) {
                _showBanDialog.value = true
                return@launch
            }
            
            try {
                val parts = chatId.split("_")
                val targetUserId = parts.firstOrNull { it != senderId } ?: return@launch

                // 1. Chuẩn bị Refs và Data
                val chatRef = db.collection("chats").document(chatId)
                val messageRef = chatRef.collection("messages").document() // Ref cho tin nhắn mới

                val messageData = hashMapOf(
                    "senderId" to senderId,
                    "text" to trimmed,
                    "timestamp" to Timestamp.now(),
                    "seen" to false
                )

                // Dữ liệu cho document chat (cả tạo mới và cập nhật)
                val chatUpdateData = mapOf(
                    "participants" to listOf(senderId, targetUserId), // Sẽ được merge khi tạo mới
                    "lastMessage" to trimmed,
                    "lastSenderId" to senderId,
                    "timestamp" to Timestamp.now()
                )

                // 2. Chạy Batch (Atomic)
                db.runBatch { batch ->

                    // Thao tác 1: Set document chat.
                    // SetOptions.merge() sẽ:
                    // - TẠO MỚI (nếu chưa có) với "participants", "lastMessage",... -> dùng 'allow create'
                    // - CẬP NHẬT (nếu đã có) "lastMessage",... -> dùng 'allow update'
                    batch.set(chatRef, chatUpdateData, SetOptions.merge())

                    // Thao tác 2: Tạo tin nhắn mới
                    batch.set(messageRef, messageData)

                }.await() // Gửi cả 2 thao tác lên server cùng lúc

                Log.d("ChatDebug", "✅ Message sent successfully (atomic batch).")

            } catch (e: Exception) {
                // Lỗi này giờ có thể là do rule 'create' hoặc 'update' của bạn
                Log.e("ChatDebug", "❌ Failed to send message with batch", e)
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
    
    fun onDismissBanDialog() {
        _showBanDialog.value = false
    }



}