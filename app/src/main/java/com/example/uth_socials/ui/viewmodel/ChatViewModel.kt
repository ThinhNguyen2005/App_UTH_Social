package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.uth_socials.data.chat.Message
import com.example.uth_socials.ui.screen.chat.ChatSummary
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
        val message = hashMapOf(
            "senderId" to senderId,
            "text" to text,
            "lastSenderId" to senderId,
            "timestamp" to Timestamp.now(),
            "seen" to false
        )

        val chatRef = db.collection("chats").document(chatId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(chatRef)
            if (!snapshot.exists()) {
                // 🔹 Nếu chat chưa tồn tại, tạo mới
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@runTransaction null
                val parts = chatId.split("_")
                if (parts.size != 2) return@runTransaction null

                // ✅ Xác định người nhận chắc chắn
                val receiverId = if (parts[0] == senderId) parts[1] else parts[0]
                val participants = listOf(currentUserId, receiverId)

                transaction.set(
                    chatRef,
                    hashMapOf(
                        "participants" to participants,
                        "lastMessage" to text,
                        "timestamp" to Timestamp.now()
                    )
                )
            } else {
                // 🔹 Nếu chat đã tồn tại, chỉ cập nhật lastMessage
                transaction.update(
                    chatRef,
                    mapOf(
                        "lastMessage" to text,
                        "lastSenderId" to senderId,
                        "timestamp" to Timestamp.now()
                    )
                )
            }

            // 🔹 Thêm tin nhắn con vào subcollection messages
            val messageRef = chatRef.collection("messages").document()
            transaction.set(messageRef, message)
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



}