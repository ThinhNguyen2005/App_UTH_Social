package com.example.uth_socials.ui.screen.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
    val reversedMessages = remember(messages) { messages.reversed() }
    var text by remember { mutableStateOf("") }

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
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // Sử dụng Column để stack MessageList và ChatBottomBar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MessageList chiếm toàn bộ không gian còn lại
            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(0)
                }
            }

            MessageList(
                messages = reversedMessages,
                currentUserId = currentUserId,
                otherUserAvatar = otherUserAvatar,
                listState = listState,
                modifier = Modifier
                    .weight(1f) // Chiếm hết không gian còn lại
            )
            
            // ChatBottomBar ở dưới cùng, sẽ nâng lên theo bàn phím
            ChatBottomBar(
                text = text,
                onTextChange = { text = it },
                onSend = {
                    val msg = text.trim()
                    if (msg.isNotEmpty() && currentUserId != null) {
                        viewModel.sendMessage(chatId, currentUserId, msg)
                        text = ""
                    }
                },
                modifier = Modifier.imePadding() // Nâng lên theo bàn phím
            )
        }
        
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
