package com.example.uth_socials.ui.component.post

import PageIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.uth_socials.R


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
//    // State để quản lý việc hiển thị trình xem ảnh
//    var showImageViewer by remember { mutableStateOf(false) }
//    var initialImageIndex by remember { mutableIntStateOf(0) }


    Card(
        modifier = Modifier.padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {

        Column {
            Column(modifier = Modifier.padding(12.dp)) {
                PostHeader(post, onUserProfileClicked)
                Spacer(modifier = Modifier.height(8.dp))
                ExpandableText(text = post.textContent, modifier = Modifier.fillMaxWidth())
            }

            if (post.imageUrls.isNotEmpty()) {
                PostMedia(
                    imageUrls = post.imageUrls
                )
            }

            // Column này chứa các hành động (actions) và cũng có padding
            Column(modifier = Modifier.padding(vertical = 0.dp, horizontal = 12.dp)) {//Khoảng cách ngang
                Spacer(modifier = Modifier.height(0.dp))
                PostActions(post, onLikeClicked, onCommentClicked, onSaveClicked, onShareClicked)
            }

            // Đường kẻ ngang cắt giữa các bài viết
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 0.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
//Phần tên và avatar người đăng bài

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
            Text(
                text = post.username,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "10 ngày trước",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }
        IconButton(onClick = { /* TODO: Mở menu */ }) {
            Icon(Icons.Default.MoreHoriz, contentDescription = "More options")
        }
        // ... Thêm IconButton cho dấu "..." ở đây nếu muốn
    }
}

//Mở rộng text
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 2
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isTextOverflow by remember { mutableStateOf(false) }

    val displayText = buildAnnotatedString {
        append(text)

        if (isExpanded) {
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append("  Thu gọn")
            }

        }
    }

    Text(
        text = displayText,
        maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 20.sp,
        textAlign = TextAlign.Justify,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onTextLayout = { result ->
            isTextOverflow = result.hasVisualOverflow
        },
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isTextOverflow || isExpanded) {
                    isExpanded = !isExpanded
                }
            }
            .animateContentSize()
    )
}
//Phần hình ảnh và có thể lướt nhiều hình ảnh
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostMedia(
    imageUrls: List<String>
) {
    if (imageUrls.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { imageUrls.size })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrls[pageIndex])
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post image $pageIndex",

                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)) // 👈 Bo góc ảnh

                )
            }

            if (imageUrls.size > 1) {
                PageIndicator(
                    pageCount = imageUrls.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}
//Hành động tim, bình luận ....
@Composable
fun PostActions(
    post: Post,
    onLikeClicked: (String) -> Unit,
    onCommentClicked: (String) -> Unit,
    onSaveClicked: (String) -> Unit,
    onShareClicked: (String) -> Unit
) {
    val defaultColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val likeColor = if (post.isLiked) MaterialTheme.colorScheme.error else defaultColor
    val saveColor = if (post.isSaved == true) primaryColor else defaultColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ❤️ LIKE
        IconButton(onClick = { onLikeClicked(post.id) }) {
            Icon(
                imageVector = if (post.isLiked)
                    Icons.Filled.Favorite        // tim đầy khi liked
                else
                    Icons.Outlined.FavoriteBorder, // tim viền khi chưa like
                contentDescription = "Like",
                tint = likeColor
            )
        }
        if (post.likes > 0) {
            Text(
                text = post.likes.toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        // 💬 COMMENT
        IconButton(onClick = { onCommentClicked(post.id) }) {
            Icon(
                imageVector = Icons.Outlined.ModeComment,
                contentDescription = "Comment",
                tint = defaultColor
            )
        }
        if (post.commentCount > 0) {
            Text(
                text = post.commentCount.toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 🔖 SAVE
        IconButton(onClick = { onSaveClicked(post.id) }) {
            Icon(
                imageVector = if (post.isSaved == true)
                    Icons.Filled.Bookmark          // icon đầy khi đã lưu
                else
                    Icons.Outlined.BookmarkBorder,  // icon viền khi chưa lưu
                contentDescription = "Save",
                tint = saveColor
            )
        }
        if (post.saveCount > 0) {
            Text(
                text = post.saveCount.toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        // 📤 SHARE
        IconButton(onClick = { onShareClicked(post.id) }) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                tint = defaultColor
            )
        }
        if (post.shareCount > 0) {
            Text(
                text = post.shareCount.toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}
