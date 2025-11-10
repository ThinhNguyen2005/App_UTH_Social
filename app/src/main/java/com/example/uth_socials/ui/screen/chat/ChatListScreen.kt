package com.example.uth_socials.ui.screen.chat



import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.data.util.DateUtils
import com.example.uth_socials.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class ChatSummary(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val avatarUrl: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: ChatViewModel = viewModel()
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(currentUserId) {
        currentUserId?.let { viewModel.listenToChatList(it) }
    }

    Scaffold(

    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(chats) { chat ->
                    ListItem(
                        leadingContent = {
                            AsyncImage(
                                model = chat.avatarUrl.ifEmpty {
                                    "https://cdn-icons-png.flaticon.com/512/149/149071.png"
                                },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        },
                        headlineContent = { Text(chat.userName) },
                        supportingContent = {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = chat.lastMessage,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, // ✅ cắt bằng "..."
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .weight(1f) // ✅ chiếm phần còn lại
                                        .padding(end = 8.dp), // tránh dính vào giờ
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DateUtils.formatTimeHeader(chat.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            val chatId = viewModel.getChatId(currentUserId ?: "", chat.userId)
                            onChatSelected(chatId)
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
