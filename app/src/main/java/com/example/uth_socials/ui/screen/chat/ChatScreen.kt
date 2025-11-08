package com.example.uth_socials.ui.screen.chat


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.ui.component.logo.ChatBottomBar
import com.example.uth_socials.ui.component.logo.ChatTopAppBar
import com.example.uth_socials.ui.viewmodel.ChatViewModel
import com.example.uth_socials.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatId: String, onBack: () -> Unit = {}) {
    val vm: ChatViewModel = viewModel()
    val messages by vm.messages.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var otherUserName by remember { mutableStateOf("") }
    var otherUserAvatar by remember { mutableStateOf("") }

    LaunchedEffect(chatId) {
        val parts = chatId.split("_")
        val other = if (parts[0] == currentUserId) parts[1] else parts[0]
        val doc = FirebaseFirestore.getInstance().collection("users").document(other).get().await()
        otherUserName = doc.getString("username") ?: "Người dùng"
        otherUserAvatar = doc.getString("avatarUrl") ?: ""
        vm.listenToMessages(chatId)
    }

    Scaffold(
        topBar = {
            ChatTopAppBar(
                userName = otherUserName,
                avatarUrl = otherUserAvatar,
                onBackClick = onBack
            )
        },
        bottomBar = {
            var text by remember { mutableStateOf("") }
            ChatBottomBar(
                text = text,
                onTextChange = { text = it },
                onSend = {
                    val msg = text.trim()
                    if (msg.isNotEmpty()) {
                        val sender = FirebaseAuth.getInstance().currentUser?.uid ?: return@ChatBottomBar
                        vm.sendMessage(chatId, sender, msg)
                        text = ""
                    }
                }
            )
        },
        // 👇 KHÔNG cho Scaffold tự cộng thêm bất kỳ inset nào
        contentWindowInsets = WindowInsets(0),
        containerColor = Color(0xFF0F1B2A)
    )  { innerPadding ->
        // Danh sách tin nhắn: chỉ nhận padding từ Scaffold (để chừa TopAppBar)
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            itemsIndexed(messages) { idx, msg ->
                val prev = messages.getOrNull(idx - 1)
                val currTime = msg.timestamp?.toDate()?.time ?: 0L
                val prevTime = prev?.timestamp?.toDate()?.time ?: 0L
                val showHeader = prev == null || DateUtils.shouldShowTimeHeader(prevTime, currTime)
                if (showHeader) TimeHeader(currTime)

                Message(
                    message = msg,
                    currentUserId = currentUserId,
                    otherUserAvatar = otherUserAvatar
                )
            }
        }
    }
}
