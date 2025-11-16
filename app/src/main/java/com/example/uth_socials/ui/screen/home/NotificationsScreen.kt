package com.example.uth_socials.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults


import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

import androidx.navigation.NavController

import coil.compose.AsyncImage

import com.example.uth_socials.data.notification.Notification
import com.example.uth_socials.ui.component.button.MoreButton
import com.example.uth_socials.ui.component.common.SectionTitle
import com.example.uth_socials.ui.component.navigation.Screen
import com.example.uth_socials.ui.viewmodel.NotificationViewModel

import kotlinx.coroutines.delay
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.uth_socials.ui.component.common.formatTimeAgo

@Composable
fun NotificationsScreen(
    notificationViewModel: NotificationViewModel,
    navController: NavController
) {
    val notifications by notificationViewModel.notifications.collectAsState()

    var visibleNotifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isCollapsed by remember { mutableStateOf(false) }
    val collapseRotate by animateFloatAsState(
        targetValue = if (isCollapsed) {
            -180f
        } else {
            0f
        },
        animationSpec = tween(800)
    )

    val originItemValue = 10
    val moreItemValue = notifications.size

    LaunchedEffect(notifications) {
        visibleNotifications = notifications.take(originItemValue)
    }

        Column(
            modifier = Modifier
                //.padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (notifications.isNotEmpty()) {
                SectionTitle("Thông báo")

                Column {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        itemsIndexed(
                            items = visibleNotifications,
                            key = { _, item -> item.id }) { index, notification ->

                            if(!notification.isRead) notificationViewModel.markAsRead(notification)

                            val visibleState = remember { MutableTransitionState(false) }

                            LaunchedEffect(visibleNotifications.size) {
                                delay(index * 230L)
                                visibleState.targetState = true
                            }
//
                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth }, // trượt từ trên xuống
                                    animationSpec = tween(durationMillis = 300)
                                ) + fadeIn(animationSpec = tween(300)),
                                exit = slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(300)),
                                modifier = Modifier
                            ) {
                                SwipeToDeleteNotification(
                                    notification = notification,
                                    onDelete = {
                                        notificationViewModel.deleteNotification(notification.id)
                                    },
                                    onUserClick = {
                                        navController.navigate(
                                            Screen.Profile.createRoute(
                                                notification.userId
                                            )
                                        ) {
                                            launchSingleTop = true
                                        }
                                    },
                                )
                            }
                        }
                    }
                    // 🔹 Nút "Hiển thị thêm"
                    if (notifications.size > visibleNotifications.size) {
                        MoreButton(
                            onClick = {
                                visibleNotifications = notifications.take(
                                    (visibleNotifications.size + moreItemValue).coerceAtMost(
                                        notifications.size
                                    )
                                )
//                                    if (visibleNotifications.size < notifications.size) {
//                                        // Hiển thị thêm
//                                        notifications.take(
//                                            (visibleNotifications.size + moreItemValue).coerceAtMost(
//                                                notifications.size
//                                            )
//                                        )
//                                    } else {
//                                        // Thu gọn lại
//                                        notifications.take(originItemValue)
//                                    }
                            },
                            text = "Hiển thị thêm",//if (visibleNotifications.size < notifications.size)
//                                "Hiển thị thêm"
//                            else
//                                "Thu gọn",
                            rotateIconDegress = collapseRotate
                        )

                        if (visibleNotifications.size < notifications.size) {
                            isCollapsed = false
                        } else {
                            isCollapsed = true
                        }
                    }
                }
            } else {
                SectionTitle("Bạn chưa có thông báo nào")
            }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeToDeleteNotification(
    notification: Notification,
    onDelete: () -> Unit,
    onUserClick: () -> Unit,
) {
    var isLocalDeleted by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }

    val dismissState = rememberDismissState(
        confirmStateChange = {
            if (it == DismissValue.DismissedToStart) {
                isLocalDeleted = true
                pendingDelete = true
            }
            true
        }
    )

    // 🔹 Chạy delay chỉ khi pendingDelete = true
    if (pendingDelete) {
        LaunchedEffect(notification.id) {
            dismissState.reset()
            delay(5_000)
            if (isLocalDeleted) {
                //isLocalDeleted = false
                //delay(5000)
                onDelete()
            }
            pendingDelete = false
        }
    }

    var cardHeightPx by remember { mutableStateOf(0) }
    val cardHeightDp = with(LocalDensity.current) { cardHeightPx.toDp() }

    val widthAnimDeleteText by animateFloatAsState(
        targetValue = if(isLocalDeleted) 1f else -dismissState.offset.value / 900f,
        animationSpec = tween(190)
    )
    val widthDeleteText by remember {
        derivedStateOf {
            when {
                isLocalDeleted -> widthAnimDeleteText
                else -> (-dismissState.offset.value / 900f)
            }
        }
    }

    val visbleDeleteText by animateFloatAsState(
        targetValue = if (!isLocalDeleted) 1f else 0f,
        animationSpec = tween(1000)
    )

    val visbleUndoButton by animateFloatAsState(
        targetValue = if (!isLocalDeleted) 0f else 1f,
        animationSpec = tween(500)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
    ) {
        //if (!isLocalDeleted) {
        AnimatedVisibility(
            visible = !isLocalDeleted,
            enter = expandHorizontally(),
            exit = shrinkHorizontally(),
        ) {
            SwipeToDismiss(
                state = dismissState,
                background = {},
                dismissContent = {
                    NotificationCard(
                        notification = notification,
                        onUserClick = onUserClick,
                        modifier = Modifier
                            .onGloballyPositioned {
                                cardHeightPx = it.size.height
                            }
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(0.dp))
                            .clickable { onUserClick() }
                            .padding(vertical = 0.dp)
                            .zIndex(2f)
                    )
                },
                directions = setOf(DismissDirection.EndToStart)
            )
        }

        AnimatedVisibility(
            visible = isLocalDeleted,
            enter = fadeIn(animationSpec = tween(durationMillis = 2000)) + slideInVertically { it / 4 }, //+ expandHorizontally(),
            exit = fadeOut(animationSpec = tween(durationMillis = 0)) + slideOutVertically { it / 4 } //+ shrinkHorizontally()
        ) {
            UndoButton(
                onClick = {
                    isLocalDeleted = false
                    pendingDelete = false
                },
            )

        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeightDp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthDeleteText)
                    .fillMaxHeight()
                    .alpha(visbleDeleteText)
                    //.height(cardHeightDp)
                    // .align(Alignment.CenterEnd)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Xóa",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun UndoButton(
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Button(
            onClick = {
                onClick()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), // nền nhạt
                contentColor = MaterialTheme.colorScheme.primary // màu text
            ),
            shape = RoundedCornerShape(16.dp), // bo góc
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Hoàn tác",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onUserClick: () -> Unit,
    modifier: Modifier,
) {
    val notificationContent = when (notification.category) {
        "post" -> "đã đăng một bài viết"
        "product" -> "đã thêm một sản phẩm mới"
        "like" -> "đã thích bài viết của bạn"
        "comment" -> "đã bình luận vào bài viết của bạn"
        else -> ""
    }

    val timeAgo = formatTimeAgo(notification.timestamp)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent//MaterialTheme.colorScheme.surfaceVariant
        ),
        //elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = notification.avatarUrl,
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(notification.username)
                        }
                        append(" ")
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            append(notificationContent)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = timeAgo.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
