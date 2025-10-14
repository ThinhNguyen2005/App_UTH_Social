package com.example.uth_socials.ui.component.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.uth_socials.data.post.Post

val Post.likeCount: Int
    get() = this.likes


@Composable
fun PostCard(
    post: Post,
    onLikeClicked: (String) -> Unit,
    onCommentClicked: (String) -> Unit,
    onSaveClicked: (String) -> Unit,
    onShareClicked: (String) -> Unit,
    onUserProfileClicked: (String) -> Unit
) {
    Card(
        modifier = Modifier.padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            PostHeader(post, onUserProfileClicked)
            Spacer(modifier = Modifier.height(8.dp))
            ExpandableText(text = post.textContent, modifier = Modifier.fillMaxWidth())
            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                PostMedia(imageUrls = post.imageUrls)
            }
            Spacer(modifier = Modifier.height(12.dp))
            PostActions(post, onLikeClicked, onCommentClicked, onSaveClicked, onShareClicked)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp), // khoảng cách trên/dưới
                thickness = 1.dp,            // độ dày
                color = Color.LightGray     // màu của line
            )

        }
    }
}

@Composable
fun PostHeader(post: Post, onUserProfileClicked: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserProfileClicked(post.userId) } // Cho phép nhấn vào cả hàng
    ) {
        AsyncImage(
            model = post.userAvatarUrl,
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = post.username, fontWeight = FontWeight.Bold)
            Text(text = "10 ngày trước", fontSize = 12.sp, color = Color.Gray) // Sẽ làm phần tính toán thời gian sau
        }
        IconButton(onClick = { /* TODO: Mở menu */ }) {
            Icon(Icons.Default.MoreHoriz, contentDescription = "More options")
        }
        // ... Thêm IconButton cho dấu "..." ở đây nếu muốn
    }
}


@Composable
fun ExpandableText(text: String, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxLines = if (isExpanded) 100 else 2 // Hiển thị 2 dòng, khi mở rộng thì 100 dòng

    Column(modifier = modifier.clickable { isExpanded = !isExpanded }) {
        Text(
            text = text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis // Thêm dấu "..." nếu bị cắt
        )
    }
}

// Đây là phần nâng cấp chính
@Composable
fun PostMedia(imageUrls: List<String>) {
    val imageCount = imageUrls.size

    // Sử dụng Box để dễ dàng xếp chồng các ảnh
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Dùng tỷ lệ 1:1 cho lưới ảnh vuông
            .clip(RoundedCornerShape(12.dp))
    ) {
        when (imageCount) {
            1 -> {
                // Trường hợp 1 ảnh: hiển thị đầy đủ
                PostImage(url = imageUrls[0], modifier = Modifier.fillMaxSize())
            }
            2 -> {
                // Trường hợp 2 ảnh: chia đôi theo chiều ngang
                Row(modifier = Modifier.fillMaxSize()) {
                    PostImage(url = imageUrls[0], modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(2.dp))
                    PostImage(url = imageUrls[1], modifier = Modifier.weight(1f))
                }
            }
            3 -> {
                // Trường hợp 3 ảnh: 1 ảnh lớn bên trái, 2 ảnh nhỏ bên phải
                Row(modifier = Modifier.fillMaxSize()) {
                    PostImage(url = imageUrls[0], modifier = Modifier.weight(2f)) // Chiếm 2/3
                    Spacer(modifier = Modifier.width(2.dp))
                    Column(modifier = Modifier.weight(1f)) { // Chiếm 1/3
                        PostImage(url = imageUrls[1], modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(2.dp))
                        PostImage(url = imageUrls[2], modifier = Modifier.weight(1f))
                    }
                }
            }
            else -> { // Trường hợp 4 ảnh hoặc nhiều hơn
                // Lưới 2x2
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        PostImage(url = imageUrls[0], modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(2.dp))
                        PostImage(url = imageUrls[1], modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        PostImage(url = imageUrls[2], modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(2.dp))
                        PostImage(url = imageUrls[3], modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Component phụ để hiển thị ảnh, tránh lặp code
@Composable
fun PostImage(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun PostActions(
    post: Post,
    onLikeClicked: (String) -> Unit,
    onCommentClicked: (String) -> Unit,
    onSaveClicked: (String) -> Unit,
    onShareClicked: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ❤️ Nút Like
        IconButton(onClick = { onLikeClicked(post.id) }) {
            Icon(
                imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (post.isLiked) Color.Red else Color.Gray
            )
        }

        Text(
            text = post.likeCount.toString(),
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        // 💬 Nút Comment
        IconButton(onClick = { onCommentClicked(post.id) }) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "Comment",
                tint = Color.Gray
            )
        }
        Text(
            text = post.commentCount.toString(),
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 🔖 Nút Save
        IconButton(onClick = { onSaveClicked(post.id) }) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = "Save",
                tint = Color.Gray
            )
        }

        // 📤 Nút Share
        IconButton(onClick = { onShareClicked(post.id) }) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.Gray
            )
        }
    }
}
