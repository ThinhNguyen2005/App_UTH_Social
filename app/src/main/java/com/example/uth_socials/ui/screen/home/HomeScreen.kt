package com.example.uth_socials.ui.screen.home

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ArrowUpward
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: (String) -> Unit = {},
    onLogout: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val homeViewModel: HomeViewModel = viewModel()
    val banStatusViewModel: BanStatusViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val banStatus by banStatusViewModel.banStatus.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
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

    var isScrolling by remember { mutableStateOf(false) }
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
                    val filteredPosts by remember {
                        derivedStateOf {
                            uiState.posts.filter { post ->
                                // Loại bỏ hidden posts
                                post.id !in uiState.hiddenPostIds &&
                                        // Loại bỏ posts của blocked users
                                        post.userId !in uiState.blockedUserIds
                            }
                        }
                    }

                    // 🔸 Pagination Logic
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val layoutInfo = lazyListState.layoutInfo
                            val totalItems = layoutInfo.totalItemsCount
                            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            // Load more when we are 3 items away from the end
                            totalItems > 0 && lastVisibleItemIndex >= totalItems - 3
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) {
                            homeViewModel.loadMorePosts()
                        }
                    }

                    // 🔸 Scroll to Top Button Logic
                    var showScrollToTop by remember { mutableStateOf(false) }
                    var lastScrollIndex by remember { mutableIntStateOf(0) }
                    
                    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
                        val currentIndex = lazyListState.firstVisibleItemIndex
                        
                        // Chỉ hiện nút khi:
                        // 1. Đã scroll xuống (không ở đầu trang)
                        // 2. Đang scroll LÊN (currentIndex < lastScrollIndex)
                        if (currentIndex > 0 && currentIndex < lastScrollIndex) {
                            showScrollToTop = true
                            
                            // Tự động ẩn sau 2 giây không scroll
                            kotlinx.coroutines.delay(2000)
                            showScrollToTop = false
                        } else if (currentIndex == 0) {
                            // Ẩn ngay khi về đầu trang
                            showScrollToTop = false
                        }
                        
                        lastScrollIndex = currentIndex
                    }
                    
                    val scope = rememberCoroutineScope()

                    Box(modifier = Modifier.fillMaxSize()) {
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
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredPosts, key = { it.id }) { post ->
                                    val isPostOwnerAdmin = uiState.adminStatusMap[post.userId] ?: false

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
                                        isUserBanned = uiState.isUserBanned,
                                        isPostOwnerAdmin = isPostOwnerAdmin,
                                        isScrolling = isScrolling
                                    )
                                }

                                // 🔸 Loading Indicator at bottom
                                if (uiState.isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                
                                // 🔸 End of Posts Indicator
                                if (!uiState.canLoadMore && !uiState.isLoadingMore && filteredPosts.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Đã hết bài viết",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showScrollToTop,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 24.dp),
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                        ) {
                            if (uiState.hasNewPosts) {
                                // Khi có bài mới: Hiển thị ExtendedFAB với text
                                ExtendedFloatingActionButton(
                                    text = { Text("Bài viết mới") },
                                    icon = { Icon(Icons.Default.ArrowUpward, contentDescription = "Có bài viết mới") },
                                    onClick = {
                                        scope.launch {
                                            lazyListState.animateScrollToItem(0)
                                            homeViewModel.clearNewPostsFlag()
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                // Khi không có bài mới: Hiển thị FAB nhỏ gọn
                                FloatingActionButton(
                                    onClick = {
                                        scope.launch {
                                            lazyListState.animateScrollToItem(0)
                                            homeViewModel.clearNewPostsFlag()
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Lên đầu trang"
                                    )
                                }
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
