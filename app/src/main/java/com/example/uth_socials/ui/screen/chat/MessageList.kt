package com.example.uth_socials.ui.screen.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uth_socials.data.chat.Message
import com.example.uth_socials.data.util.DateUtils


@Composable
fun MessageList(
    messages: List<Message>,
    currentUserId: String?,
    otherUserAvatar: String,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        reverseLayout = false // Hoặc true tùy logic của bạn
    ) {
        itemsIndexed(messages) { idx, msg ->
            val prev = messages.getOrNull(idx - 1)
            val currTime = msg.timestamp?.toDate()?.time ?: 0L
            val prevTime = prev?.timestamp?.toDate()?.time ?: 0L

            val showHeader = prev == null || DateUtils.shouldShowTimeHeader(prevTime, currTime)

            if (showHeader) {
                TimeHeader(timestamp = currTime)
            }

            Message(
                message = msg,
                currentUserId = currentUserId,
                otherUserAvatar = otherUserAvatar
            )
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