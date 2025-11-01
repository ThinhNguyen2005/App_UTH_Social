package com.example.uth_socials.ui.screen.home

import android.content.Intent
<<<<<<< HEAD
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
=======
import androidx.compose.foundation.layout.*
>>>>>>> main
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.di.ViewModelFactory
import com.example.uth_socials.ui.component.logo.HomeTopAppBar
import com.example.uth_socials.ui.component.navigation.FilterTabs
import com.example.uth_socials.ui.component.navigation.HomeBottomNavigation
import com.example.uth_socials.ui.component.post.CommentSheetContent
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.common.ReportDialog
import com.example.uth_socials.ui.component.common.DeleteConfirmDialog
import com.example.uth_socials.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    // navController: NavHostController
) {
<<<<<<< HEAD
    val uiState by homeViewModel.uiState.collectAsState()
    // THÊM MỚI: Lấy context hiện tại để sử dụng cho Intent
    val context = LocalContext.current
// 🔹 Lấy FirebaseAuth để đăng xuất
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
=======
    val postRepository = remember { PostRepository() } // Dùng remember để không tạo lại mỗi lần recomposition
    val viewModelFactory = remember { ViewModelFactory(postRepository) }
    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
>>>>>>> main

    // ✅ BƯỚC 2: BÂY GIỜ bạn có thể sử dụng ViewModel một cách an toàn
    val uiState by homeViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


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



    Scaffold(
        topBar = {
            HomeTopAppBar(
                onSearchClick = { /* TODO */ },
                onMessagesClick = { /* TODO */ }
            )
        },
        bottomBar = {
            HomeBottomNavigation()
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Tabs lọc danh mục
            FilterTabs(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    homeViewModel.onCategorySelected(category)
                }
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
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

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(filteredPosts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    onLikeClicked = { homeViewModel.onLikeClicked(post.id) },
                                    onCommentClicked = {
                                        homeViewModel.onCommentClicked(post.id)
                                    },
                                    onSaveClicked = { homeViewModel.onSaveClicked(post.id) },
                                    onShareClicked = { homeViewModel.onShareClicked(post.id) },
                                    onUserProfileClicked = { homeViewModel.onUserProfileClicked(post.userId) },
                                    onReportClicked = { homeViewModel.onReportClicked(post.id) },
                                    onDeleteClicked = { homeViewModel.onDeleteClicked(post.id) },
                                    onHideClicked = { homeViewModel.onHideClicked(post.id) },
                                    currentUserId = uiState.currentUserId
                                )
                            }
                            
                            // 🔸 Infinite scroll - load more trigger
                            if (!uiState.isLoading && filteredPosts.isNotEmpty() && !uiState.isLoadingMore) {
                                item {
                                    LaunchedEffect(Unit) {
                                        homeViewModel.onLoadMore()
                                    }
                                }
                            }
                            
                            // 🔸 Show loading indicator at bottom when loading more
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp)
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

    if (uiState.commentSheetPostId != null) {
        ModalBottomSheet(
            onDismissRequest = { homeViewModel.onDismissCommentSheet() },
            sheetState = sheetState
        ) {
            CommentSheetContent(
                comments = uiState.commentsForSheet,
                isLoading = uiState.isSheetLoading,
                onAddComment = { commentText ->
                    homeViewModel.addComment(uiState.commentSheetPostId!!, commentText)
                },
                onLikeComment = homeViewModel::onCommentLikeClicked, // Dùng function reference
                onUserProfileClick = homeViewModel::onUserProfileClicked,
                commentPostState = uiState.commentPostState,
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


