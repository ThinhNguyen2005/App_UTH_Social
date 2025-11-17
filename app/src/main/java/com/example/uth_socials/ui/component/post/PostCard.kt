package com.example.uth_socials.ui.component.post

import PageIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlin.math.abs
import coil.request.ImageRequest
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview

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
    onNavigateToUserProfile: ((String) -> Unit)? = null
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
                    onNavigateToUserProfile = onNavigateToUserProfile
                )
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
    onNavigateToUserProfile: ((String) -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigateToUserProfile?.invoke(post.userId) ?: onUserProfileClicked(post.userId)
            }
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
    if (imageUrls.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown()
                            var horizontalDragConsumed = false
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.pressed }) {
                                    val dragAmount = event.changes.sumOf { it.positionChange().x.toDouble() }.toFloat()
                                    if (!horizontalDragConsumed && abs(dragAmount) > 0.5f) {
                                        horizontalDragConsumed = true
                                        event.changes.forEach {
                                            if (it.positionChange() != Offset.Zero) it.consume()
                                        }
                                    }
                                    pagerState.dispatchRawDelta(-dragAmount)
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
        ) { page ->

            var imageRatio by remember(page) { mutableStateOf<Float?>(null) }

            val safeAspectRatio = remember(imageRatio) {
                when {
                    imageRatio == null -> 1f // Default khi chưa load
                    imageRatio!! <= 0f -> 1f // Fallback nếu ratio <= 0
                    imageRatio!!.isNaN() -> 1f // Fallback nếu NaN
                    imageRatio!! > 3f -> 3f // ảnh quá ngang
                    imageRatio!! < 0.3f -> 0.3f // ảnh quá dọc
                    else -> imageRatio!!
                }
            }

            // ✅ Sửa: Tự quyết định ContentScale dựa trên tỉ lệ
            val scale = remember(safeAspectRatio) {
                when {
                    safeAspectRatio < 0.7f -> ContentScale.Fit        // ảnh dọc dài
                    safeAspectRatio > 1.6f -> ContentScale.FillWidth  // ảnh ngang dài
                    else -> ContentScale.Crop                    // gần vuông -> crop nhẹ cho đẹp
                }
            }

            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrls[page])
                    .crossfade(true)
                    .dispatcher(Dispatchers.IO)
                    .allowHardware(true)
                    .build(),
                contentDescription = "Post image ${page + 1}",

                onSuccess = { state ->
                    val w = state.result.drawable.intrinsicWidth
                    val h = state.result.drawable.intrinsicHeight
                    if (w > 0 && h > 0) {
                        val ratio = w.toFloat() / h.toFloat()
                        if (ratio.isFinite() && ratio > 0f) {
                            imageRatio = ratio
                        } else {
                            imageRatio = 1f // Fallback
                        }
                    } else {
                        imageRatio = 1f // Fallback nếu không lấy được size
                    }
                },

                loading = {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            strokeWidth = 2.dp
                        )
                    }
                },

                error = {
                    Box(Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.ImageNotSupported,
                            contentDescription = "Image loading failed",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                        )
                    }
                },

                contentScale = scale,

                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(safeAspectRatio)
                    .heightIn(max = 520.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = { /* TODO: xem ảnh full screen */ },
                        onLongClick = { /* TODO: lưu hoặc chia sẻ ảnh */ }
                    )
            )
        }

        // Indicator
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
                icon = Icons.Outlined.ModeComment,
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
                icon = Icons.Outlined.Share,
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
    // Row này để nhóm icon và số đếm, và để tăng vùng nhấn
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, // Tắt hiệu ứng gợn sóng để dùng hiệu ứng của IconButton
            onClick = { if (enabled) onClick() }
        )
    ) {
        IconButton(
            onClick = onClick,
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
            if (kotlin.random.Random.nextBoolean()) {
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

