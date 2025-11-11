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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.data.repository.AdminRepository
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.ui.viewmodel.ViewModelFactory
import com.example.uth_socials.ui.component.navigation.FilterTabs
import com.example.uth_socials.ui.component.post.CommentSheetContent
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.post.PostCardSkeleton
import com.example.uth_socials.ui.component.common.ReportDialog
import com.example.uth_socials.ui.component.common.DeleteConfirmDialog
import com.example.uth_socials.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: (String) -> Unit = {}
) {
    val postRepository =
        remember { PostRepository() } // Dùng remember để không tạo lại mỗi lần recomposition
    val viewModelFactory = remember { ViewModelFactory(postRepository) }
    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)

    // ✅ BƯỚC 2: BÂY GIỜ bạn có thể sử dụng ViewModel một cách an toàn
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ✅ CHECK ADMIN STATUS của từng user để disable report button
    val adminStatusCache = remember { mutableStateMapOf<String, Boolean>() }


    // 2. Tạo một instance của Factory, truyền Repository vào.
    LaunchedEffect(uiState.commentSheetPostId) {
        if (uiState.commentSheetPostId != null) {
            sheetState.show()
        } else {
            // Đóng sheet nếu nó đang mở
            if (sheetState.isVisible) {
                sheetState.hide()
            }
        }
    }

    // ✅ Xử lý chia sẻ bài viết
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
                    // 🔸 Filter hidden posts trước khi hiển thị
                    val filteredPosts = remember(uiState.posts, uiState.hiddenPostIds) {
                        uiState.posts.filter { it.id !in uiState.hiddenPostIds }
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
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(filteredPosts, key = { it.id }) { post ->
                                // ✅ CHECK ADMIN STATUS cho từng post owner
                                var isPostOwnerAdmin by remember(post.userId) { mutableStateOf(adminStatusCache[post.userId] ?: false) }

                                LaunchedEffect(post.userId) {
                                    isPostOwnerAdmin = homeViewModel.getAdminStatus(post.userId)
                                }

                                PostCard(
                                    post = post,
                                    onLikeClicked = { homeViewModel.onLikeClicked(post.id) },
                                    onCommentClicked = { homeViewModel.onCommentClicked(post.id)
                                    },
                                    onSaveClicked = { homeViewModel.onSaveClicked(post.id) },
                                    onShareClicked = { homeViewModel.onShareClicked(post.id) },
                                    onUserProfileClicked = { onNavigateToProfile(post.userId) },
                                    onReportClicked = { homeViewModel.onReportClicked(post.id) },
                                    onDeleteClicked = { homeViewModel.onDeleteClicked(post.id) },
                                    onHideClicked = { homeViewModel.onHideClicked(post.id) },
                                    currentUserId = uiState.currentUserId,
                                    isPostOwnerAdmin = isPostOwnerAdmin,
                                    isCurrentUserAdmin = uiState.isCurrentUserAdmin
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
                    currentUserAvatarUrl = uiState.currentUserAvatarUrl
                )
            }
        }

        // --- 🔸 REPORT DIALOG ---
        ReportDialog(
            isVisible = uiState.showReportDialog,
            onDismiss = { homeViewModel.onDismissReportDialog() },
            onReportReasonChanged = { homeViewModel.onReportReasonChanged(it) },
            onReportDescriptionChanged = { homeViewModel.onReportDescriptionChanged(it) },
            onSubmit = { homeViewModel.onSubmitReport() },
            reportReason = uiState.reportReason,
            reportDescription = uiState.reportDescription,
            isReporting = uiState.isReporting
        )

        // --- 🔸 DELETE CONFIRM DIALOG ---
        DeleteConfirmDialog(
            isVisible = uiState.showDeleteConfirmDialog,
            onDismiss = { homeViewModel.onDismissDeleteDialog() },
            onConfirm = { homeViewModel.onConfirmDelete() },
            isDeleting = uiState.isDeleting
        )

    }
}
