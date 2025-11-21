package com.example.uth_socials.ui.screen.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.ui.component.logo.ChatBottomBar
import com.example.uth_socials.ui.component.logo.ChatTopAppBar
import com.example.uth_socials.ui.component.common.BannedUserDialog
import com.example.uth_socials.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatId: String, onBack: () -> Unit = {}) {
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val currentUserId = viewModel.currentUserId

    val otherUserName by viewModel.otherUserName.collectAsState()
    val otherUserAvatar by viewModel.otherUserAvatar.collectAsState()


    LaunchedEffect(chatId) {
        viewModel.enterChatRoom(chatId)
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
                    if (msg.isNotEmpty() && currentUserId != null) {
                        viewModel.sendMessage(chatId,currentUserId, msg)
                        text = ""
                    }
                }
            )
        },
        // 👇 KHÔNG cho Scaffold tự cộng thêm bất kỳ inset nào
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background
    )  { innerPadding ->
        // Danh sách tin nhắn: chỉ nhận padding từ Scaffold (để chừa TopAppBar)
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }

        MessageList(
            messages = messages,
            currentUserId = currentUserId,
            otherUserAvatar = otherUserAvatar,
            listState = listState,
            modifier = Modifier.padding(innerPadding) // Áp dụng padding từ Scaffold
        )
        
        // Ban dialog
        val showBanDialog by viewModel.showBanDialog.collectAsState()
        BannedUserDialog(
            isVisible = showBanDialog,
            banReason = null,
            onDismiss = { viewModel.onDismissBanDialog() },
            onLogout = {
                FirebaseAuth.getInstance().signOut()
                viewModel.onDismissBanDialog()
                onBack()
            }
        )
    }
}
