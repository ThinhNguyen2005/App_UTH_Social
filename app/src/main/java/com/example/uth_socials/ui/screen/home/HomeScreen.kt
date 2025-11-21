package com.example.uth_socials.ui.screen.home

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.ui.component.navigation.FilterTabs
import com.example.uth_socials.ui.component.post.CommentSheetContent
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.post.PostCardSkeleton
import com.example.uth_socials.ui.component.common.ReportDialog
import com.example.uth_socials.ui.component.common.ConfirmDialog
import com.example.uth_socials.ui.component.common.BannedUserDialog
import com.example.uth_socials.ui.component.common.EditPostDialog
import com.example.uth_socials.ui.viewmodel.HomeViewModel
import com.example.uth_socials.ui.viewmodel.BanStatusViewModel
import com.example.uth_socials.ui.viewmodel.DialogType
import com.example.uth_socials.data.util.SecurityValidator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: (String) -> Unit = {},
    onLogout: () -> Unit,
    onScrollStateChanged: (isScrollingUp: Boolean, isAtTop: Boolean) -> Unit = { _, _ -> },
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val homeViewModel: HomeViewModel = viewModel()
    val banStatusViewModel: BanStatusViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val banStatus by banStatusViewModel.banStatus.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val adminStatusCache = remember { mutableStateMapOf<String, Boolean>() }
    val snackbarHostState = remember { SnackbarHostState() }

    // Scroll state management for top/bottom bar visibility
    val lazyListState = rememberLazyListState()
    var lastScrollPosition by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        homeViewModel.refreshBlockedUsers()
    }

    LaunchedEffect(banStatus.isBanned) {
        if (uiState.isUserBanned != banStatus.isBanned) {
            homeViewModel.updateBanStatus(banStatus.isBanned)
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Long)
            homeViewModel.clearError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            homeViewModel.clearSuccessMessage()
        }
    }
    LaunchedEffect(uiState.commentSheetPostId) {
        if (uiState.commentSheetPostId != null) {
            sheetState.show()
        } else {
            if (sheetState.isVisible) {
                sheetState.hide()
            }
        }
    }
    LaunchedEffect(uiState.shareContent) {
        uiState.shareContent?.let { content ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            val chooser = Intent.createChooser(intent, "Chia sẻ bài viết qua...")
            context.startActivity(chooser)

            homeViewModel.onShareDialogLaunched()
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex * 1000 + lazyListState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged() // Chỉ emit khi giá trị thực sự thay đổi
            .collect { currentScrollPosition ->
                val isScrollingUp = currentScrollPosition < lastScrollPosition
                val isAtTop = currentScrollPosition <= 0

                onScrollStateChanged(isScrollingUp, isAtTop)
                lastScrollPosition = currentScrollPosition
            }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // Tabs lọc danh mục
        FilterTabs(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { category ->
                homeViewModel.onCategorySelected(category)
            },
            isLoading = uiState.categories.isEmpty() && uiState.isLoading,
            error = if (uiState.categories.isEmpty() && uiState.error != null) uiState.error else null
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    // 🔸 Skeleton Loading - Hiển thị 5-7 skeleton posts
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(6) { // Hiển thị 6 skeleton posts
                            PostCardSkeleton()
                        }
                    }
                }

                uiState.error != null -> {
                    // 🔸 Error dialog đẹp hơn với icon và button
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Oops! Có lỗi xảy ra",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "Vui lòng thử lại",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { homeViewModel.onRetry() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Thử lại")
                            }
                        }
                    }
                }

                else -> {
                    val filteredPosts =
                        remember(uiState.posts, uiState.hiddenPostIds, uiState.blockedUserIds) {
                            uiState.posts.filter { post ->
                                // Loại bỏ hidden posts
                                post.id !in uiState.hiddenPostIds &&
                                        // Loại bỏ posts của blocked users
                                        post.userId !in uiState.blockedUserIds
                            }
                        }

                    if (filteredPosts.isEmpty()) {
                        // 🔸 Empty state - không có posts trong category này
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Article,
                                    contentDescription = "No posts",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when (uiState.selectedCategory?.id) {
                                        "all", "latest" -> "Chưa có bài viết nào"
                                        else -> "Chưa có bài viết trong chủ đề này"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (uiState.selectedCategory?.id) {
                                        "all", "latest" -> "Hãy là người đầu tiên chia sẻ điều gì đó!"
                                        else -> "Bài viết trong chủ đề \"${uiState.selectedCategory?.name}\" sẽ xuất hiện ở đây"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier),
                            state = lazyListState,
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(filteredPosts, key = { it.id }) { post ->
                                // check admin cho từng post owner

                                LaunchedEffect(post.userId) {
                                    if (adminStatusCache[post.userId] == null) {
                                        try {
                                            val (isAdmin, _) = SecurityValidator.getCachedAdminStatus(
                                                post.userId
                                            )
                                            adminStatusCache[post.userId] = isAdmin
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (_: Exception) {
                                            adminStatusCache[post.userId] = false
                                        }
                                    } else {
                                        adminStatusCache[post.userId] ?: false
                                    }
                                }

                                PostCard(
                                    post = post,
                                    onLikeClicked = { homeViewModel.onLikeClicked(post.id) },
                                    onCommentClicked = {
                                        homeViewModel.onCommentClicked(post.id)
                                    },
                                    onSaveClicked = { homeViewModel.onSaveClicked(post.id) },
                                    onShareClicked = { homeViewModel.onShareClicked(post.id) },
                                    onUserProfileClicked = { onNavigateToProfile(post.userId) },
                                    onReportClicked = { homeViewModel.onReportClicked(post.id) },
                                    onDeleteClicked = { homeViewModel.onDeleteClicked(post.id) },
                                    onHideClicked = { homeViewModel.onHideClicked(post.id) },
                                    onEditClicked = { homeViewModel.onEditPostClicked(post.id) },
                                    currentUserId = uiState.currentUserId,
                                    isCurrentUserAdmin = uiState.isCurrentUserAdmin,
                                    isUserBanned = uiState.isUserBanned
                                )
                            }
                        }
                    }
                }
            }
        }


        if (uiState.commentSheetPostId != null) {
            ModalBottomSheet(
                onDismissRequest = { homeViewModel.onDismissCommentSheet() },
                sheetState = sheetState
            ) {
                CommentSheetContent(
                    postId = uiState.commentSheetPostId!!,
                    comments = uiState.commentsForSheet,
                    isLoading = uiState.isSheetLoading,
                    onAddComment = { commentText ->
                        homeViewModel.addComment(uiState.commentSheetPostId!!, commentText)
                    },
                    onLikeComment = homeViewModel::onCommentLikeClicked,
                    onUserProfileClick = onNavigateToProfile,
                    commentPostState = uiState.commentPostState,
                    commentErrorMessage = uiState.commentErrorMessage,
                )
            }
        }

        ReportDialog(
            isVisible = uiState.showReportDialog,
            onDismiss = { homeViewModel.onDismissReportDialog() },
            onReportReasonChanged = { homeViewModel.onReportReasonChanged(it) },
            onReportDescriptionChanged = { homeViewModel.onReportDescriptionChanged(it) },
            onSubmit = { homeViewModel.onSubmitReport() },
            reportReason = uiState.reportReason,
            reportDescription = uiState.reportDescription,
            isReporting = uiState.isReporting,
            reportErrorMessage = uiState.reportErrorMessage
        )

        when (uiState.dialogType) {
            is DialogType.DeletePost -> {
                ConfirmDialog(
                    isVisible = true,
                    onDismiss = { homeViewModel.onDismissDialog() },
                    onConfirm = { homeViewModel.onConfirmDialog() },
                    isLoading = uiState.isProcessing,
                    title = if (uiState.isCurrentUserAdmin) "Xóa bài viết (Admin)" else "Xóa bài viết",
                    message = if (uiState.isCurrentUserAdmin)
                        "Bạn đang xóa bài viết này với quyền Admin. Người đăng bài sẽ bị cấm tự động. Hành động này không thể hoàn tác."
                    else
                        "Bạn có chắc chắn muốn xóa bài viết này? Hành động này không thể hoàn tác.",
                    confirmButtonText = "Xóa",
                    confirmButtonColor = MaterialTheme.colorScheme.error,
                    isCurrentUserAdmin = uiState.isCurrentUserAdmin
                )
            }

            is DialogType.None -> {}
            is DialogType.BlockUser -> {}
            is DialogType.UnblockUser -> {}
        }

        BannedUserDialog(
            isVisible = uiState.showBanDialog,
            banReason = banStatus.banReason,
            onDismiss = { homeViewModel.onDismissBanDialog() },
            onLogout = {
                homeViewModel.cleanupOnLogout()
                homeViewModel.onDismissBanDialog()
                onLogout()
            }
        )

        EditPostDialog(
            isVisible = uiState.showEditPostDialog,
            currentContent = uiState.editingPostContent,
            isLoading = uiState.isSavingPost,
            errorMessage = uiState.editPostErrorMessage,
            onDismiss = { homeViewModel.onDismissEditDialog() },
            onSave = { homeViewModel.onSaveEditedPost() },
            onContentChange = { homeViewModel.onUpdatePostContent(it) }
        )

    }
}
