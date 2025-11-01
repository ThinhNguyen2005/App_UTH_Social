package com.example.uth_socials.ui.component.post

import PageIndicator
import com.example.uth_socials.ui.component.common.formatTimeAgo
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.uth_socials.data.util.MenuItemData
import com.example.uth_socials.ui.component.common.ReusablePopupMenu
import androidx.compose.ui.layout.ContentScale


@Composable
fun PostCard(
    post: Post,
    onLikeClicked: (String) -> Unit,
    onCommentClicked: (String) -> Unit,
    onSaveClicked: (String) -> Unit,
    onShareClicked: (String) -> Unit,
    onUserProfileClicked: (String) -> Unit,
    onHideClicked: (String) -> Unit,
    onReportClicked: (String) -> Unit,
    onDeleteClicked: (String) -> Unit,
    currentUserId: String? = null

) {
    Card(
        modifier = Modifier.padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Column(modifier = Modifier.padding(12.dp)) {
                PostHeader(post, onUserProfileClicked, onHideClicked, onReportClicked, onDeleteClicked, currentUserId)
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
private fun PostHeader(
    post: Post,
    onUserProfileClicked: (String) -> Unit,
    onHideClicked: (String) -> Unit,
    onReportClicked: (String) -> Unit,
    onDeleteClicked: (String) -> Unit,
    currentUserId: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserProfileClicked(post.userId) }
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
                text = formatTimeAgo(post.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }
        Box {
            var menuExpanded by remember { mutableStateOf(false) }

            IconButton(onClick = {
                menuExpanded = true
            }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "More options")
            }

            // 🔸 Tạo danh sách menu items động dựa trên quyền
            val menuItems = mutableListOf(
                MenuItemData(
                    text = "Ẩn bài viết",
                    icon = Icons.Default.HideSource,
                    onClick = {
                        onHideClicked(post.id)
                        menuExpanded = false
                    }
                ),
                MenuItemData(
                    text = "Báo cáo",
                    icon = Icons.Default.Report,
                    onClick = {
                        onReportClicked(post.id)
                        menuExpanded = false
                    }
                )
            )

            // Chỉ hiển thị "Xóa" nếu người dùng hiện tại là chủ bài
            if (post.userId == currentUserId) {
                menuItems.add(
                    MenuItemData(
                        text = "Xóa",
                        icon = Icons.Default.Delete,
                        onClick = {
                            onDeleteClicked(post.id)
                            menuExpanded = false
                        }
                    )
                )
            }

            ReusablePopupMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                menuItems = menuItems
            )
        }
    }
}

//Mở rộng text
@Composable
private fun ExpandableText(
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
private fun PostMedia(
    imageUrls: List<String>
) {
    if (imageUrls.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { imageUrls.size })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clip(RoundedCornerShape(12.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                AsyncImage(
                    model = imageUrls[pageIndex],
                    contentDescription = "Post image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
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
// Trong file PostCard.kt

@Composable
private fun PostActions(
    post: Post,
    onLikeClicked: (String) -> Unit,
    onCommentClicked: (String) -> Unit,
    onSaveClicked: (String) -> Unit,
    onShareClicked: (String) -> Unit
) {
    val defaultColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    val likeColor = if (post.isLiked) MaterialTheme.colorScheme.error else defaultColor
    val likeIcon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder

    val saveColor = if (post.isSaved) primaryColor else defaultColor
    val saveIcon = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Thêm padding dọc để tạo khoảng trống với nội dung bên trên
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        // Dùng Arrangement.SpaceBetween để đẩy nút Save/Share sang phải
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Nhóm các nút bên trái
        Row(verticalAlignment = Alignment.CenterVertically) {
            PostActionItem(
                onClick = { onLikeClicked(post.id) },
                icon = likeIcon,
                count = post.likes,
                tint = likeColor,
                contentDescription = "Like"
            )
            // Thêm khoảng cách giữa các nút
            Spacer(modifier = Modifier.width(16.dp))
            PostActionItem(
                onClick = { onCommentClicked(post.id) },
                icon = Icons.Outlined.ModeComment,
                count = post.commentCount,
                tint = defaultColor,
                contentDescription = "Comment"
            )
        }

        // Nhóm các nút bên phải
        Row(verticalAlignment = Alignment.CenterVertically) {
            PostActionItem(
                onClick = { onSaveClicked(post.id) },
                icon = saveIcon,
                count = post.saveCount,
                tint = saveColor,
                contentDescription = "Save"
            )
            Spacer(modifier = Modifier.width(16.dp))
            PostActionItem(
                onClick = { onShareClicked(post.id) },
                icon = Icons.Outlined.Share,
                count = post.shareCount,
                tint = defaultColor,
                contentDescription = "Share"
            )
        }
    }
}

@Composable
private fun PostActionItem(
    onClick: () -> Unit,
    icon: ImageVector,
    count: Int,
    tint: Color,
    contentDescription: String
) {
    // Row này để nhóm icon và số đếm, và để tăng vùng nhấn
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // Tắt hiệu ứng gợn sóng để dùng hiệu ứng của IconButton
            onClick = onClick
        )
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp) // Kích thước vùng nhấn
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(24.dp) // Kích thước của riêng icon
            )
        }
        if (count > 0) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

