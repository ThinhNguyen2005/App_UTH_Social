package com.example.uth_socials.ui.screen.chat



import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import com.example.uth_socials.ui.component.logo.ChatListTopBar
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
    val isLoading by viewModel.isListLoading.collectAsState()




    Scaffold(
        topBar = {
            ChatListTopBar(
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        when {
            isLoading  -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            chats.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(70.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Chưa có đoạn chat nào",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(contentPadding = padding) {
                    items(chats) { chat ->
                        ListItem(
                            leadingContent = {
                                AsyncImage(
                                    model = chat.avatarUrl.ifEmpty {
                                        "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da"
                                    },
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
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
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
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
                                val chatId = viewModel.getChatId(chat.userId)
                                if (chatId != null) onChatSelected(chatId)
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}