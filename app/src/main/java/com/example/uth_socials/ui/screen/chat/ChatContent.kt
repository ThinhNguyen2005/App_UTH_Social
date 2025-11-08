package com.example.uth_socials.ui.screen.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.data.chat.Message
import com.example.uth_socials.ui.viewmodel.ChatViewModel
import com.example.uth_socials.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatContent(
    messages: List<Message>,
    onSend: (String) -> Unit,
    currentUserId: String?,
    otherUserAvatar: String,
    modifier: Modifier = Modifier
) {
    val viewModel: ChatViewModel = viewModel()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding() // chỉ xử lý phần "nút điều hướng", không ảnh hưởng keyboard
            .imePadding() // đẩy khi bàn phím mở
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            itemsIndexed(messages) { index, message ->
                val prevMessage = messages.getOrNull(index - 1)
                val currTime = message.timestamp?.toDate()?.time ?: 0L
                val prevTime = prevMessage?.timestamp?.toDate()?.time ?: 0L

                val showHeaderBefore =
                    prevMessage == null || DateUtils.shouldShowTimeHeader(prevTime, currTime)

                if (showHeaderBefore) {
                    TimeHeader(timestamp = currTime)
                }

                Message(
                    message = message,
                    currentUserId = currentUserId,
                    otherUserAvatar = otherUserAvatar
                )
            }
        }

        Divider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))

        // ✅ Thanh nhập tin nhắn ôm sát cạnh dưới
        Surface(
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                // ôm sát khi KHÔNG có bàn phím (dành chỗ cho nút điều hướng)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Nhập tin nhắn...") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSend(text.trim())
                            text = ""
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Gửi",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TimeHeader(timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = DateUtils.formatTimeHeader(timestamp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}