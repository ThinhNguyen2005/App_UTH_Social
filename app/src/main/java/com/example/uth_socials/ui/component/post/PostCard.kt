package com.example.uth_socials.ui.component.post

import com.example.uth_socials.ui.component.common.PageIndicator
import androidx.compose.animation.AnimatedVisibility
import com.example.uth_socials.ui.component.common.formatTimeAgo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.uth_socials.data.util.MenuItemData
import com.example.uth_socials.ui.component.common.ReusablePopupMenu
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import coil.request.ImageRequest
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.*
import androidx.compose.ui.tooling.preview.Preview
import kotlin.random.Random
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.uth_socials.ui.component.common.FullScreenImageViewer

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
    onEditClicked: ((String) -> Unit)? = null,
    currentUserId: String? = null,
    isCurrentUserAdmin: Boolean = false,
    isUserBanned: Boolean = false,
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    isPostOwnerAdmin: Boolean = false,
    isScrolling: Boolean = false
) {
    Card(
        modifier = Modifier.padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Column(modifier = Modifier.padding(12.dp)) {
                PostHeader(
                    post = post,
                    onUserProfileClicked = onUserProfileClicked,
                    onHideClicked = onHideClicked,
                    onReportClicked = onReportClicked,
                    onDeleteClicked = onDeleteClicked,
                    onEditClicked = onEditClicked,
                    currentUserId = currentUserId,
                    isCurrentUserAdmin = isCurrentUserAdmin,
                    isPostOwnerAdmin = isPostOwnerAdmin,
                    onNavigateToUserProfile = onNavigateToUserProfile,
                    isScrolling = isScrolling
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExpandableText(
                    text = post.textContent,
                    modifier = Modifier.fillMaxWidth(),
                    isScrolling = isScrolling
                )
            }
            if (post.imageUrls.isNotEmpty()) {
                PostMedia(
                    imageUrls = post.imageUrls,
                    isScrolling = isScrolling
                )
            }
            // Column này chứa các hành động (actions) và cũng có padding
            Column(modifier = Modifier.padding(vertical = 0.dp, horizontal = 12.dp)) {//Khoảng cách ngang
                Spacer(modifier = Modifier.height(0.dp))
                PostActions(
                    post = post,
                    onLikeClicked = onLikeClicked,
                    onCommentClicked = onCommentClicked,
                    onSaveClicked = onSaveClicked,
                    onShareClicked = onShareClicked,
                    isEnabled = currentUserId != null && !isUserBanned
                )
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
    onEditClicked: ((String) -> Unit)? = null,
    currentUserId: String? = null,
    isPostOwnerAdmin: Boolean = false,
    isCurrentUserAdmin: Boolean = false,
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    isScrolling: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (post.userId != currentUserId) {
                    Modifier.clickable {
                        onNavigateToUserProfile?.invoke(post.userId) ?: onUserProfileClicked(post.userId)
                    }
                } else {
                    Modifier
                }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(post.userAvatarUrl)
                .crossfade(!isScrolling)
                .size(120)
                .dispatcher(Dispatchers.IO)
                .build(),
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
            val formattedTime = remember(post.timestamp) {
                formatTimeAgo(post.timestamp)
            }
            Text(
                text = formattedTime,
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
                )
            )

            if (!isPostOwnerAdmin && post.userId != currentUserId) {
                menuItems.add(
                    MenuItemData(
                        text = "Báo cáo",
                        icon = Icons.Default.Report,
                        onClick = {
                            onReportClicked(post.id)
                            menuExpanded = false
                        }
                    )
                )
            }

            // Hiện tùy chọn chỉnh sửa nếu là chủ bài viết
            if (post.userId == currentUserId && onEditClicked != null) {
                menuItems.add(
                    MenuItemData(
                        text = "Chỉnh sửa",
                        icon = Icons.Default.Edit,
                        onClick = {
                            onEditClicked(post.id)
                            menuExpanded = false
                        }
                    )
                )
            }

            // Hiện tùy chọn xóa nếu là admin và chủ bài viết
            if (post.userId == currentUserId || isCurrentUserAdmin) {
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
    collapsedMaxLines: Int = 2,
    isScrolling: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isTextOverflow by remember { mutableStateOf(false) }

    val displayText = remember(isExpanded, text) {
        buildAnnotatedString {
            append(text)

            if (isExpanded) {
                withStyle(
                    style = SpanStyle(
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append("  Thu gọn")
                }

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
            .then(
                if (!isScrolling) Modifier.animateContentSize() else Modifier
            )
    )
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostMedia(
    imageUrls: List<String>,
    isScrolling: Boolean = false
) {
    if (imageUrls.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    val context = LocalContext.current
    var showFullScreen by remember { mutableStateOf(false) }
    var initialImageIndex by remember { mutableIntStateOf(0) }

    // IG-style: container aspect ratio is derived from the first image, clamped to
    // [0.8 (4:5 portrait), 1.91 (≈16:9 landscape)]. Other carousel pages crop to fit.
    val minAspect = 0.8f
    val maxAspect = 1.91f
    var containerAspect by remember(imageUrls) { mutableStateOf<Float?>(null) }
    var firstImageClamped by remember(imageUrls) { mutableStateOf(false) }
    val singleImage = imageUrls.size == 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(containerAspect ?: 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 0.dp,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val isFirstPage = page == 0
            val useFit = singleImage && isFirstPage && containerAspect != null && !firstImageClamped
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrls[page])
                    .crossfade(!isScrolling)
                    .size(1080, 1920)
                    .dispatcher(Dispatchers.IO)
                    .build(),
                contentDescription = "Post image ${page + 1}",
                contentScale = if (useFit) ContentScale.Fit else ContentScale.Crop,
                onSuccess = { state ->
                    if (isFirstPage && containerAspect == null) {
                        val drawable = state.result.drawable
                        val w = drawable.intrinsicWidth.toFloat()
                        val h = drawable.intrinsicHeight.toFloat()
                        if (w > 0f && h > 0f) {
                            val natural = w / h
                            val clamped = natural.coerceIn(minAspect, maxAspect)
                            firstImageClamped = clamped != natural
                            containerAspect = clamped
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {
                            initialImageIndex = page
                            showFullScreen = true
                        },
                        onLongClick = { /* TODO: lưu/chia sẻ */ }
                    )
            )
        }

        AnimatedVisibility(
            visible = imageUrls.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${imageUrls.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        AnimatedVisibility(
            visible = imageUrls.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                PageIndicator(pageCount = imageUrls.size, currentPage = pagerState.currentPage)
            }
        }
    }

    // Full screen image viewer
    if (showFullScreen) {
        Dialog(
            onDismissRequest = { showFullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            FullScreenImageViewer(
                imageUrls = imageUrls,
                initialIndex = initialImageIndex,
                onDismiss = { showFullScreen = false }
            )
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
    onShareClicked: (String) -> Unit,
    isEnabled: Boolean = true
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
                onClick = { if (isEnabled) onLikeClicked(post.id) },
                icon = likeIcon,
                count = post.likes,
                tint = likeColor,
                contentDescription = "Like",
                enabled = isEnabled
            )
            // Thêm khoảng cách giữa các nút
            Spacer(modifier = Modifier.width(16.dp))
            PostActionItem(
                onClick = { if (isEnabled) onCommentClicked(post.id) },
                icon = Icons.Outlined.ChatBubbleOutline,
                count = post.commentCount,
                tint = defaultColor,
                contentDescription = "Comment",
                enabled = isEnabled
            )
        }

        // Nhóm các nút bên phải
        Row(verticalAlignment = Alignment.CenterVertically) {
            PostActionItem(
                onClick = { if (isEnabled) onSaveClicked(post.id) },
                icon = saveIcon,
                count = post.saveCount,
                tint = saveColor,
                contentDescription = "Save",
                enabled = isEnabled
            )
            Spacer(modifier = Modifier.width(16.dp))
            PostActionItem(
                onClick = { onShareClicked(post.id) },
                icon = Icons.AutoMirrored.Outlined.Send,
                count = post.shareCount,
                tint = defaultColor,
                contentDescription = "Share",
                enabled = true
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
    contentDescription: String,
    enabled: Boolean = true
) {
    // Row này để nhóm icon và số đếm
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (enabled) onClick() },
            enabled = enabled,
            modifier = Modifier.size(40.dp) // Kích thước vùng nhấn
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant,
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

// Skeleton Loading Component for PostCard
@Composable
fun PostCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header Skeleton
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar skeleton
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .shimmerEffect()
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Username skeleton
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.6f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Timestamp skeleton
                    Box(
                        modifier = Modifier
                            .height(12.dp)
                            .fillMaxWidth(0.4f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .shimmerEffect()
                    )
                }

                // Menu skeleton
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .shimmerEffect()
                )
            }

            // Content Skeleton
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                // Text content skeleton (multiple lines)
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(if (index == 2) 0.7f else 1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .shimmerEffect()
                    )
                    if (index < 2) Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Image skeleton (sometimes show, sometimes not for variety)
            if (Random.nextBoolean()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .shimmerEffect()
                )
            }

            // Actions skeleton
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left actions
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                                .shimmerEffect()
                        )
                    }
                }

                // Right action (share)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .shimmerEffect()
                )
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 0.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

// Preview for PostCardSkeleton
@Preview(showBackground = true)
@Composable
fun PostCardSkeletonPreview() {
    MaterialTheme {
        Column {
            repeat(3) {
                PostCardSkeleton()
            }
        }
    }
}

// Shimmer Effect Modifier
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2.0f * size.width.toFloat(),
        targetValue = 2.0f * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.surfaceVariant
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
        .onGloballyPositioned { size = it.size }
}

