package com.example.uth_socials.ui.screen.chat



import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.uth_socials.data.chat.Message
import com.example.uth_socials.data.util.DateUtils


@Composable
fun Message(
    message: Message,
    currentUserId: String?,
    otherUserAvatar: String
) {
    val isMe = message.senderId == currentUserId
    var showTime by remember { mutableStateOf(false) }

    // Xác định màu sắc và hình dạng bong bóng chat
    val bubbleColor = if (isMe) Color(0xFFC9E4FF) else Color.White
    val bubbleShape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp), // Thêm padding ngang
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar của người gửi (chỉ hiển thị nếu không phải là bạn)
        if (!isMe) {
            AsyncImage(
                model = otherUserAvatar.ifEmpty {
                    "https://cdn-icons-png.flaticon.com/512/149/149071.png"
                },
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .align(Alignment.Bottom), // Căn avatar ở dưới cùng
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Nội dung tin nhắn
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Surface(
                color = bubbleColor,
                shape = bubbleShape,
                // Thêm elevation để có hiệu ứng đổ bóng nhẹ cho tin nhắn màu trắng
                shadowElevation = if (!isMe) 1.dp else 0.dp
            ) {
                Text(
                    text = message.text,
                    color = Color.Black, // Chữ màu đen cho dễ đọc
                    modifier = Modifier
                        .clickable { showTime = !showTime }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            // Hiển thị thời gian khi người dùng nhấn vào tin nhắn
            if (showTime) {
                Text(
                    text = DateUtils.formatTime(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}