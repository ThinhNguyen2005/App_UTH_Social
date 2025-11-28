package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.chat.Message
import com.example.uth_socials.data.repository.ChatRepository
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.ui.screen.chat.ChatSummary
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val db = FirebaseFirestore.getInstance()

    val currentUserId = userRepository.getCurrentUserId()

    private val _showBanDialog = MutableStateFlow(false)
    val showBanDialog = _showBanDialog.asStateFlow()

    // Thông tin người đang chat cùng
    private val _otherUserName = MutableStateFlow("")
    val otherUserName = _otherUserName.asStateFlow()

    private val _otherUserAvatar = MutableStateFlow("")
    val otherUserAvatar = _otherUserAvatar.asStateFlow()

    private val _isListLoading = MutableStateFlow(true)
    val isListLoading = _isListLoading.asStateFlow()


    val chats: StateFlow<List<ChatSummary>> = flow {
        if (currentUserId != null) {
            // Kết nối Flow từ Repository
            emitAll(chatRepository.getChatsFlow(currentUserId))
        } else {
            _isListLoading.value = false
            emit(emptyList())
        }
    }.map { rawChats ->
        rawChats.map { chat ->
            try {
                // 1. Lấy thông tin user
                val user = userRepository.getUser(chat.userId)
                val name = user?.username ?: "Người dùng"
                val avatar = user?.avatarUrl ?: ""

                // 2. Format tin nhắn tại đây
                val formattedMsg = if (chat.lastSenderId == currentUserId) {
                    "Bạn: ${chat.lastMessage}"
                } else {
                    "$name: ${chat.lastMessage}"
                }

                // 3. Trả về ChatSummary hoàn chỉnh
                chat.copy(
                    userName = name,
                    avatarUrl = avatar,
                    lastMessage = formattedMsg //
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi khi load thông tin user cho chat ${chat.id}", e)
                chat
            }
        }.sortedByDescending { it.timestamp }
    }
        .onEach {
            _isListLoading.value = false
        }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )




    // --- PHẦN 2: CHI TIẾT CHAT (CHAT DETAIL) ---

    // Biến lưu chatId đang xem (Thay đổi khi vào màn hình chat)
    private val _currentChatId = MutableStateFlow("")

    // Tự động lấy tin nhắn dựa trên _currentChatId
    val messages: StateFlow<List<Message>> = _currentChatId
        .flatMapLatest { chatId ->
            if (chatId.isBlank()) {
                flowOf(emptyList())
            } else {
                chatRepository.getMessagesFlow(chatId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )



    // Hàm được gọi khi vào màn hình ChatScreen
    fun enterChatRoom(chatId: String) {
        _currentChatId.value = chatId
        loadOtherUserInfo(chatId)
    }

    private fun loadOtherUserInfo(chatId: String) {
        val myId = currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parts = chatId.split("_")
                val otherId = if (parts[0] == myId) parts[1] else parts[0]
                val user = userRepository.getUser(otherId)
                _otherUserName.value = user?.username ?: "Người dùng"
                _otherUserAvatar.value = user?.avatarUrl ?: ""
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi load thông tin người chat: $chatId", e)
                _otherUserName.value = "Người dùng"
                _otherUserAvatar.value = ""
            }
        }
    }

    // --- CÁC HÀM TƯƠNG TÁC ---

    fun sendMessage(chatId: String, currentUserId: String?, text: String) {
        val senderId = currentUserId ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            // Logic check ban ở đây...

            // Gửi tin (Batch write)
            try {
                val parts = chatId.split("_")
                val targetUserId = if (parts[0] == senderId) parts[1] else parts[0]

                val chatRef = db.collection("chats").document(chatId)
                val messageRef = chatRef.collection("messages").document()

                val messageData = hashMapOf(
                    "senderId" to senderId,
                    "text" to trimmed,
                    "timestamp" to Timestamp.now(),
                    "seen" to false
                )
                val chatUpdate = mapOf(
                    "participants" to listOf(senderId, targetUserId),
                    "lastMessage" to trimmed,
                    "lastSenderId" to senderId,
                    "timestamp" to Timestamp.now()
                )

                db.runBatch { batch ->
                    batch.set(chatRef, chatUpdate, SetOptions.merge())
                    batch.set(messageRef, messageData)
                }.await()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Lỗi guiwr tin nhắn", e)
            }
        }
    }

    // Hàm helper để UI gọi khi click vào item
    fun getChatId(otherUserId: String): String? {
        return currentUserId?.let { chatRepository.buildChatId(it, otherUserId) }
    }

    fun onDismissBanDialog() { _showBanDialog.value = false }
}