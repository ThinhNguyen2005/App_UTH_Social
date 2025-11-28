package com.example.uth_socials.ui.component.post

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.util.TableInfo
import coil.compose.AsyncImage
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.ui.component.common.SectionTitleButton
import com.example.uth_socials.ui.component.common.formatTimeAgo
import com.example.uth_socials.ui.viewmodel.CommentPostState
import com.example.uth_socials.ui.viewmodel.HomeViewModel


@Composable
fun CommentSheetContent(
    postId: String,
    comments: List<Comment>,
    isLoading: Boolean,
    onAddComment: (String) -> Unit,
    onAddCommentReply: (String, String, String) -> Unit,
    onLikeComment: (String, String) -> Unit,
    onUserProfileClick: (String) -> Unit,
    commentPostState: CommentPostState,
    commentErrorMessage: String? = null,

    ) {
    var userReplyToText by remember { mutableStateOf("") }
    var commentContent by remember { mutableStateOf("") }
    var countToReset by remember { mutableIntStateOf(1) }
    var commentText by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }

    var commentParentname by remember { mutableStateOf("") }
    var commentParentId by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxHeight(0.8f)
    ) {
        Text("Bình luận", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                comments.isEmpty() -> {
                    Text(
                        text = "Chưa có bình luận nào.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(comments, key = { it.id }) { commentParent ->
                            var isMore by remember { mutableStateOf(false) }
                            val replyComments = comments.filter { it.parentId == commentParent.id }
                            val childHeights = remember { mutableMapOf<String, Int>() }
                            var allMeasured by remember { mutableStateOf(false) }

                            if (commentParent.parentId == "") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                        //.fillMaxHeight()
                                        //.background(Color.Gray)
                                    ) {

                                        if (replyComments.isNotEmpty()) {
                                            VerticalLine(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .padding(top = 8.dp, end = 8.dp)
                                                    .fillMaxHeight()
                                            )
                                        }

                                        Column() {
                                            CommentItem(
                                                comment = commentParent,
                                                modifier = Modifier.fillMaxWidth(),
                                                parentName = commentParent.username,
                                                isParent = true,
                                                ammountReplyComments = replyComments.size,
                                                onUserClick = onUserProfileClick,
                                                onLikeClick = { onLikeComment(postId, it) },
                                                onReplyClick = {
                                                    countToReset = 1
                                                    userReplyToText = "@${commentParent.username} "
                                                    focusRequester.requestFocus()
                                                    isReplying = true
                                                    commentParentname = commentParent.username
                                                    commentParentId = commentParent.id
                                                },
                                                onMoreClick = { isMore = true }
                                            )

                                            if (isMore) {

                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier
                                                        .padding(start = 30.dp)
                                                        .onGloballyPositioned { coordinates ->
                                                            //childrenHeightPx = coordinates.size.height.toFloat()
                                                        }
                                                ) {
                                                    replyComments.forEach { commentChildren ->

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                5.dp
                                                            ),
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(
                                                                        top = 22.dp,
                                                                        start = 0.dp,
                                                                        end = 8.dp
                                                                    )
                                                            ) {
                                                                HorizonLine()
                                                            }

                                                            CommentItem(
                                                                comment = commentChildren,
                                                                modifier = Modifier
                                                                    .onGloballyPositioned { layout ->
                                                                        childHeights[commentChildren.id] =
                                                                            layout.size.height

                                                                        // Kiểm tra đo xong hết chưa
                                                                        if (childHeights.size == replyComments.size) {
                                                                            allMeasured = true
                                                                        }
                                                                    }
                                                                    .fillMaxWidth(),
                                                                parentName = commentParent.username,
                                                                isParent = false,
                                                                ammountReplyComments = replyComments.size,
                                                                onUserClick = onUserProfileClick,
                                                                onLikeClick = {
                                                                    onLikeComment(
                                                                        postId,
                                                                        it
                                                                    )
                                                                },
                                                                onReplyClick = {},
                                                                onMoreClick = {}
                                                            )
                                                        }
                                                        //}
                                                    }
                                                }
                                            }

                                            if (!isMore && replyComments.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 30.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {

                                                    Box(
                                                        modifier = Modifier
                                                            .padding(
                                                                top = 22.dp,
                                                                start = 0.dp,
                                                                end = 5.dp
                                                            )
                                                    ) {
                                                        HorizonLine()
                                                    }

                                                    SectionTitleButton(
                                                        "Hiển thị thêm ${replyComments.size} bình luận",
                                                        onClick = {
                                                            //onMoreClick()
                                                            isMore = true
                                                        },
                                                    )
                                                }
                                            }
                                        }

                                        if (replyComments.isNotEmpty()) {
                                            val lineHeightDp =
                                                if (allMeasured) {
                                                    val lastHeight =
                                                        childHeights[replyComments.last().id] ?: 0
                                                    val totalHeight = replyComments.sumOf {
                                                        childHeights[it.id] ?: 0
                                                    }
                                                    with(LocalDensity.current) { (totalHeight).toDp() }
                                                } else {
                                                    0.dp
                                                }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight()
                                                    .padding(top = 50.dp, start = 18.dp)
                                                //.background(Color.Gray)
                                            ) {
                                                VerticalLine(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(if (isMore) lineHeightDp else 75.dp)
                                                        //.fillMaxHeight()
                                                        .padding(top = 0.dp, start = 0.dp)
                                                )
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        // Error message display
        commentErrorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(modifier = Modifier.width(12.dp))

            val isPosting = commentPostState == CommentPostState.POSTING
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (commentText.isBlank() && !isReplying) {
                    Text(
                        text = "Thêm bình luận...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isReplying) {
                        Text(
                            text = userReplyToText,
                            color = Color(0xFF0066FF),
                        )
                    }
                    BasicTextField(
                        value = commentText,
                        onValueChange = { it ->
                            countToReset = 0
                            commentText = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent() { event ->
                                if (event.type == KeyEventType.KeyUp && event.key == Key.Backspace) {
                                    if (commentText.isEmpty() && isReplying) {
                                        countToReset += 1
                                        if (countToReset >= 2) {
                                            isReplying = false
                                            userReplyToText = ""
                                            countToReset = 0
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            },
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        enabled = !isPosting
                    )
                }

            }

            IconButton(
                onClick = {
                    Log.d("CommentSheet", "Send button clicked, commentText: '$commentText'")

                    if (isReplying) {
                        onAddCommentReply(commentParentname, commentParentId, commentText)
                    } else {
                        onAddComment(commentText)
                    }

                    focusRequester.freeFocus()
                    keyboardController?.hide()
                    countToReset = 1
                    isReplying = false
                    userReplyToText = ""
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
                        tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.6f
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    modifier: Modifier,
    parentName: String = "",
    isParent: Boolean = false,
    ammountReplyComments: Int = 0,
    onUserClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onReplyClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val timeAgo = formatTimeAgo(comment.timestamp)

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
                            buildAnnotatedString {
                                if (!isParent) {
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFF0066FF),
                                            fontSize = 15.sp
                                        )
                                    ) {
                                        append("@$parentName ")
                                    }
                                }
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                ) {
                                    append(comment.text)
                                }
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp, start = 12.dp)
                ) {
                    Text(text = timeAgo, fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    if (isParent) {
                        Text(
                            text = "Trả lời",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }
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
                targetValue = if (comment.liked) 1.1f else 1.0f,
                animationSpec = tween(durationMillis = 200),
                label = "likeScale"
            )
            IconButton(onClick = { onLikeClick(comment.id) }) {
                Icon(
                    imageVector = if (comment.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like comment",
                    tint = if (comment.liked) Color.Red else Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(scale)
                )
            }
        }

    }
}

@Composable
fun HorizonLine() {
    Card(
        modifier = Modifier
            .width(30.dp)
            .height(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text("")
    }
}

@Composable
fun VerticalLine(
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text("")
    }
}

