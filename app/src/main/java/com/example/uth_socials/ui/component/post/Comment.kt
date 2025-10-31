package com.example.uth_socials.ui.component.post

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.uth_socials.R
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.ui.component.common.formatTimeAgo
import com.example.uth_socials.ui.viewmodel.CommentPostState

@Composable
fun CommentSheetContent(
    comments: List<Comment>,
    isLoading: Boolean,
    onAddComment: (String) -> Unit,
    onLikeComment: (String) -> Unit,
    onUserProfileClick: (String) -> Unit,
    commentPostState: CommentPostState,
    currentUserAvatarUrl: String?

) {
    var commentText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun handleReply(username: String) {
        commentText = "@$username "
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
        Text("Bình luận", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (comments.isEmpty()) {
                Text("Chưa có bình luận nào.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(comments, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            onUserClick = onUserProfileClick,
                            onLikeClick = onLikeComment,
                            onReplyClick = { handleReply(comment.username) }
                        )
                    }
                }
            }
        }

//        Row(
//            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            val isPosting = commentPostState == CommentPostState.POSTING
//            OutlinedTextField(
//                value = commentText,
//                onValueChange = { commentText = it },
//                modifier = Modifier.weight(1f).focusRequester(focusRequester),
//                placeholder = { Text("Viết bình luận...") },
//                enabled = !isPosting
//            )
//            IconButton(
//                onClick = {
//                    onAddComment(commentText)
//                    commentText = ""
//                },
//                enabled = commentText.isNotBlank() && !isPosting
//            ) {
//                if (isPosting) {
//                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
//                } else {
//                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi bình luận")
//                }
//            }
//        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Avatar người dùng (thêm cảm giác cá nhân hóa)
            // TODO: Thay thế bằng URL avatar của người dùng đang đăng nhập
            AsyncImage(
                model = currentUserAvatarUrl,
                contentDescription = "My Avatar",
                placeholder = painterResource(id = R.drawable.ic_user_placeholder), // Thêm placeholder
                error = painterResource(id = R.drawable.ic_user_placeholder), // Thêm ảnh lỗi
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Vùng nhập liệu tùy chỉnh thay cho OutlinedTextField
            val isPosting = commentPostState == CommentPostState.POSTING
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp) // Bo tròn như viên thuốc
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp), // Padding cho text bên trong
                contentAlignment = Alignment.CenterStart
            ) {
                // Placeholder chỉ hiển thị khi text rỗng
                if (commentText.isBlank()) {
                    Text(
                        text = "Thêm bình luận...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // BasicTextField không có trang trí, cho phép chúng ta toàn quyền kiểm soát
                BasicTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface // Đảm bảo màu chữ đúng với theme
                    ),
                    enabled = !isPosting
                )
            }

            // 3. Nút gửi
            IconButton(
                onClick = {
                    onAddComment(commentText)
                    commentText = ""
                },
                enabled = commentText.isNotBlank() && !isPosting
            ) {
                if (isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi bình luận",
                        tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onUserClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onReplyClick: () -> Unit
) {
    val timeAgo = formatTimeAgo(comment.timestamp)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.userAvatarUrl,
            contentDescription = "User avatar",
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable { onUserClick(comment.userId) },
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onReplyClick)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = comment.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onUserClick(comment.userId) }
                    )
                    Text(
                        text = comment.text,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp, start = 12.dp)
            ) {
                Text(text = timeAgo, fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Trả lời", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                if (comment.likes > 0) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${comment.likes} lượt thích",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                }
            }
        }

        val scale by animateFloatAsState(
            targetValue = if (comment.isLiked) 1.1f else 1.0f,
            animationSpec = tween(durationMillis = 200),
            label = "likeScale"
        )
        IconButton(onClick = { onLikeClick(comment.id) }) {
            Icon(
                imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like comment",
                tint = if (comment.isLiked) Color.Red else Color.Gray,
                modifier = Modifier.size(20.dp).scale(scale)
            )
        }
    }
}

