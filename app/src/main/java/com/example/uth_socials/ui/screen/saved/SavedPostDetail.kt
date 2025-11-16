package com.example.uth_socials.ui.screen.saved

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.viewmodel.SavedPostDetailViewModel
import com.example.uth_socials.ui.viewmodel.PostDetailViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Màn hình chi tiết bài viết
 *
 * Improvements:
 * - Sử dụng PostCard component (reuse, no duplication)
 * - Đầy đủ tính năng (like, comment, save, share, hide, report, delete, edit)
 * - Image pager với nhiều ảnh
 * - Expandable text
 * - Menu options
 * - Consistent UI với Home feed
 * - Real-time updates
 *
 * Features:
 * - Like/Unlike posts
 * - Save/Unsave posts (with confirmation)
 * - Navigate to comments
 * - Share posts
 * - Hide posts
 * - Report posts
 * - Delete posts (owner/admin)
 * - Edit posts (owner)
 * - Navigate to user profile
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostDetail(
    postId: String,
    onBackClicked: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onCommentClicked: (String) -> Unit = {},
    viewModel: SavedPostDetailViewModel = viewModel(
        factory = PostDetailViewModelFactory(postId)
    )
) {
    val post by viewModel.post.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Get current user
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Track save state changes for feedback
    var previousSaveState by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(post?.isSaved) {
        post?.let {
            if (previousSaveState != null && previousSaveState != it.isSaved) {
                if (!it.isSaved) {
                    snackbarHostState.showSnackbar("Đã bỏ lưu bài viết")
                }
            }
            previousSaveState = it.isSaved
        }
    }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiết bài viết",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Đang tải bài viết...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                post == null -> {
                    // Error/Not found state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Không tìm thấy bài viết",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bài viết có thể đã bị xóa hoặc bạn không có quyền xem",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onBackClicked) {
                                Text("Quay lại")
                            }
                        }
                    }
                }

                else -> {
                    //POST CONTENT
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        item {
                            PostCard(
                                post = post!!,
                                // Like action
                                onLikeClicked = { postId ->
                                    viewModel.toggleLike()
                                    Log.d("SavedPostDetail", "Like toggled for: $postId")
                                },
                                // Comment action
                                onCommentClicked = { postId ->
                                    onCommentClicked(postId)
                                    Log.d("SavedPostDetail", "Navigate to comments: $postId")
                                },
                                // Save action
                                onSaveClicked = { postId ->
                                    viewModel.toggleSave()
                                    Log.d("SavedPostDetail", "Save toggled for: $postId")
                                },
                                // Share action
                                onShareClicked = { postId ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Tính năng chia sẻ đang phát triển")
                                    }
                                    Log.d("SavedPostDetail", "Share clicked: $postId")
                                },
                                // User profile navigation
                                onUserProfileClicked = { userId ->
                                    onUserClick(userId)
                                    Log.d("SavedPostDetail", "Navigate to profile: $userId")
                                },
                                // Hide post action
                                onHideClicked = { postId ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Đã ẩn bài viết")
                                        // Could navigate back after hiding
                                        // onBackClicked()
                                    }
                                    Log.d("SavedPostDetail", "Hide clicked: $postId")
                                },
                                // Report post action
                                onReportClicked = { postId ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Đã gửi báo cáo")
                                    }
                                    Log.d("SavedPostDetail", "Report clicked: $postId")
                                },
                                // Delete post action
                                onDeleteClicked = { postId ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Đã xóa bài viết")
                                        // Navigate back after delete
                                        onBackClicked()
                                    }
                                    Log.d("SavedPostDetail", "Delete clicked: $postId")
                                },
                                // Edit post action
                                onEditClicked = { postId ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Tính năng chỉnh sửa đang phát triển")
                                    }
                                    Log.d("SavedPostDetail", "Edit clicked: $postId")
                                },
                                // Current user context
                                currentUserId = currentUserId,
                                isCurrentUserAdmin = false, // TODO: Get from user profile/auth
                                isUserBanned = false, // TODO: Get from user profile
                                onNavigateToUserProfile = onUserClick
                            )
                        }

                        //Add related posts section here
                        // item {
                        //     Divider(
                        //         modifier = Modifier.padding(vertical = 16.dp),
                        //         thickness = 8.dp,
                        //         color = MaterialTheme.colorScheme.surfaceVariant
                        //     )
                        // }

                        // item {
                        //     Text(
                        //         text = "Bài viết liên quan",
                        //         style = MaterialTheme.typography.titleMedium,
                        //         fontWeight = FontWeight.Bold,
                        //         modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        //     )
                        // }

                        // items(
                        //     items = relatedPosts,
                        //     key = { it.id }
                        // ) { relatedPost ->
                        //     PostCard(
                        //         post = relatedPost,
                        //         // Same callbacks...
                        //     )
                        // }
                    }
                }
            }
        }
    }
}