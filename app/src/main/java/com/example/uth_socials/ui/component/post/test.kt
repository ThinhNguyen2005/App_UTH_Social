package com.example.uth_socials.ui.component.post

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Một Composable đại diện cho ô nhập bình luận, không có avatar.
 * Giao diện gồm một ô text field bo tròn và một nút gửi.
 *
 * @param commentText Trạng thái text hiện tại của ô nhập.
 * @param onCommentChange Callback được gọi khi text thay đổi.
 * @param onSendClick Callback được gọi khi nhấn nút gửi.
 * @param isPosting Cờ để cho biết liệu bình luận có đang trong quá trình gửi hay không.
 *                  Khi true, ô nhập và nút gửi sẽ bị vô hiệu hóa.
 */
@Composable
fun CommentInputField(
    commentText: String,
    onCommentChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isPosting: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Bạn có thể điều chỉnh padding nếu cần
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ô nhập bình luận và nút gửi được gom vào một Box
        Box(
            modifier = Modifier
                .weight(1f) // Chiếm toàn bộ chiều rộng có sẵn
                .clip(RoundedCornerShape(24.dp)) // Bo tròn nhiều hơn cho mềm mại
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp), // Thêm padding bên trái cho text
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TextField cơ bản để tùy chỉnh hoàn toàn
                BasicTextField(
                    value = commentText,
                    onValueChange = onCommentChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp), // Tăng padding dọc cho cao hơn
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    singleLine = true,
                    enabled = !isPosting,
                    decorationBox = { innerTextField ->
                        // Hiển thị placeholder khi text rỗng
                        if (commentText.isEmpty()) {
                            Text(
                                text = "Viết bình luận...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )

                // Nút gửi
                IconButton(
                    onClick = onSendClick,
                    enabled = commentText.isNotBlank() && !isPosting
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = "Gửi bình luận",
                        tint = if (commentText.isNotBlank() && !isPosting)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ===================================
// HÀM PREVIEW ĐỂ XEM TRƯỚC
// ===================================
@Preview(showBackground = true)
@Composable
private fun CommentInputFieldPreview_Empty() {
    MaterialTheme {
        CommentInputField(
            commentText = "",
            onCommentChange = {},
            onSendClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CommentInputFieldPreview_Filled() {
    MaterialTheme {
        CommentInputField(
            commentText = "Bình luận này rất hay!",
            onCommentChange = {},
            onSendClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CommentInputFieldPreview_Posting() {
    MaterialTheme {
        CommentInputField(
            commentText = "Đang gửi đi...",
            onCommentChange = {},
            onSendClick = {},
            isPosting = true
        )
    }
}