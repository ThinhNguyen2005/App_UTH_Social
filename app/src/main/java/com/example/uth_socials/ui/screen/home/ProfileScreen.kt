package com.example.uth_socials.ui.screen.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.uth_socials.ui.component.common.ConfirmDialog
import com.example.uth_socials.ui.component.common.EditPostDialog
import com.example.uth_socials.ui.component.common.ReportDialog
import com.example.uth_socials.ui.component.post.CommentSheetContent
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.profile.ProfileHeader
import com.example.uth_socials.ui.viewmodel.DialogType
import com.example.uth_socials.ui.viewmodel.HomeViewModel
import com.example.uth_socials.ui.viewmodel.ProfileUiState
import com.example.uth_socials.ui.viewmodel.ProfileViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    homeViewModel: HomeViewModel,
    onBackClicked: () -> Unit = {},
    onMessageClicked: (String) -> Unit = {},
    onSettingClicked: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},

    ) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            onBackClicked()
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
    if (uiState.isUserBlocked && !uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bạn đã chặn người dùng này",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Bạn sẽ không thể xem bài viết hoặc tương tác với họ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    ProfileScreenContent(
        uiState = uiState,
        onBackClicked = onBackClicked,
        onLikeClicked = {viewModel.onLikeClicked(it)},
        onCommentClicked = {viewModel.onCommentClicked(it)},
        onSaveClicked = {viewModel.onSaveClicked(it)},
        onShareClicked = {homeViewModel.onShareClicked(it)},
        onFollowClicked = { viewModel.onFollowClicked() },
        onBlockUser = { viewModel.onBlockUser() },
        onDeletePost = { postId -> viewModel.onDeleteClicked(postId) },
        onMessageClicked = { onMessageClicked(uiState.profileUserId) },
        onSettingClicked = { onSettingClicked() },
        onReportClicked = { viewModel.onReportClicked(it) },
        onEditPostClicked ={ postId -> viewModel.onEditPostClicked(postId)},

    )
    if (uiState.commentSheetPostId != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissCommentSheet() },
            sheetState = sheetState
        ) {
            CommentSheetContent(
                postId = uiState.commentSheetPostId!!,
                comments = uiState.commentsForSheet,
                isLoading = uiState.isSheetLoading,
                onAddComment = { commentText ->
                    viewModel.addComment(uiState.commentSheetPostId!!, commentText)
                },
                onLikeComment = viewModel::onCommentLikeClicked,
                onUserProfileClick = onNavigateToProfile,
                commentPostState = uiState.commentPostState,
                commentErrorMessage = uiState.commentErrorMessage,
            )
        }
    }
    when (uiState.dialogType) {
        is DialogType.None -> { /* Nothing to show */
        }

        is DialogType.DeletePost -> {
            ConfirmDialog(
                isVisible = true,
                onDismiss = { viewModel.onDismissDialog() },
                onConfirm = { viewModel.onConfirmDialog() },
                isLoading = uiState.isProcessing,
                title = "Xóa bài viết",
                message = "Bạn có chắc chắn muốn xóa bài viết này? Hành động này không thể hoàn tác.",
                confirmButtonText = "Xóa",
                confirmButtonColor = MaterialTheme.colorScheme.error
            )
        }

        is DialogType.BlockUser -> {
            ConfirmDialog(
                isVisible = true,
                onDismiss = { viewModel.onDismissDialog() },
                onConfirm = { viewModel.onConfirmDialog() },
                isLoading = uiState.isProcessing,
                title = "Chặn người dùng",
                message = "Bạn có chắc chắn muốn chặn người dùng này? Bạn sẽ không thể xem bài viết hoặc tương tác với họ.",
                confirmButtonText = "Chặn",
                confirmButtonColor = MaterialTheme.colorScheme.error
            )
        }

        is DialogType.UnblockUser -> {
            // Được xử lý ở BlockedUsersScreen
        }
    }
    EditPostDialog(
        isVisible = uiState.showEditPostDialog,
        currentContent = uiState.editingPostContent,
        isLoading = uiState.isSavingPost,
        errorMessage = uiState.editPostErrorMessage,
        onDismiss = { viewModel.onDismissEditDialog() },
        onSave = { viewModel.onSaveEditedPost() },
        onContentChange = { viewModel.onUpdatePostContent(it) }
    )
    ReportDialog(
        isVisible = uiState.showReportDialog,
        onDismiss = { viewModel.onDismissReportDialog() },
        onReportReasonChanged = { viewModel.onReportReasonChanged(it) },
        onReportDescriptionChanged = { viewModel.onReportDescriptionChanged(it) },
        onSubmit = { viewModel.onSubmitReport() },
        reportReason = uiState.reportReason,
        reportDescription = uiState.reportDescription,
        isReporting = uiState.isReporting,
        reportErrorMessage = uiState.reportErrorMessage
    )
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onLikeClicked: (String) -> Unit = {},
    onCommentClicked: (String) -> Unit = {},
    onSaveClicked: (String) -> Unit = {},
    onShareClicked: (String) -> Unit = {},
    onBackClicked: () -> Unit,
    onFollowClicked: () -> Unit,
    onBlockUser: () -> Unit,
    onDeletePost: (String) -> Unit,
    onReportClicked: (String) -> Unit,
    onMessageClicked: (String) -> Unit,
    onSettingClicked: () -> Unit,
    onEditPostClicked: (String) -> Unit,

) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            ProfileHeader(
                username = uiState.username,
                avatarUrl = uiState.userAvatarUrl,
                bio = uiState.bio,
                followers = uiState.followers,
                following = uiState.following,
                postCount = uiState.postCount,
                isOwner = uiState.isOwner,
                isFollowing = uiState.isFollowing,
                onFollowClicked = onFollowClicked,
                showBackButton = !uiState.isOwner,
                onBackClicked = onBackClicked,
                onMoreClicked = onBlockUser,
                onSettingClicked = onSettingClicked,
                onMessageClicked = { onMessageClicked(uiState.profileUserId) },
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
        }

        if (selectedTabIndex == 0) {
            if (uiState.posts.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyPostsView()
                }
            } else {
                items(uiState.posts, key = { it.id }) { post ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PostCard(
                            post = post,
                            onLikeClicked = {onLikeClicked(post.id)},
                            onCommentClicked = {onCommentClicked(post.id)},
                            onSaveClicked = {onSaveClicked(post.id)},
                            onShareClicked = {onShareClicked(post.id)},
                            onUserProfileClicked = {},
                            onHideClicked = {},
                            onReportClicked = { onReportClicked(post.id) },
                            onDeleteClicked = {
                                if (uiState.isOwner) {
                                    onDeletePost(post.id)
                                }
                            },
                            onEditClicked = {
                                if (uiState.isOwner && post.userId == uiState.currentUserId) {
                                    onEditPostClicked(post.id)
                                }
                            },


                            currentUserId = uiState.currentUserId ?: ""
                        )
                    }
                }
            }
        }
        if (selectedTabIndex == 1) {
            item {
                EmptyProductView()
            }
        }
    }
}

@Composable
private fun EmptyPostsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Chưa có bài viết nào",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyProductView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Chưa có sản phẩm nào",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

    }

}

