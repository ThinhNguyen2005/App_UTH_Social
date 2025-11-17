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
import androidx.compose.material3.Text
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
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.profile.ProfileHeader
import com.example.uth_socials.ui.viewmodel.DialogType
import com.example.uth_socials.ui.viewmodel.ProfileUiState
import com.example.uth_socials.ui.viewmodel.ProfileViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClicked: () -> Unit = {},
    onMessageClicked: (String) -> Unit = {},
    onSettingClicked: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ✅ Navigate back sau khi block user
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            onBackClicked()
        }
    }

    // ✅ Hiển thị message khi user đã bị block
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
        onFollowClicked = { viewModel.onFollowClicked() },
        onBlockUser = { viewModel.onBlockUser() },
        onDeletePost = { postId -> viewModel.onDeleteClicked(postId) },
        onMessageClicked = { onMessageClicked(uiState.profileUserId) },
        onSettingClicked = { onSettingClicked() }
    )
    
    when (uiState.dialogType) {
        is DialogType.None -> { /* Nothing to show */ }
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
    onBackClicked: () -> Unit,
    onFollowClicked: () -> Unit,
    onBlockUser: () -> Unit,
    onDeletePost: (String) -> Unit,
    onMessageClicked: (String) -> Unit,
    onSettingClicked: () -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column() {
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
            onMessageClicked = { onMessageClicked(uiState.profileUserId) },
            onSettingClicked = { onSettingClicked()},
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it }
        )
        when(selectedTabIndex){
            0->{
                if (uiState.posts.isEmpty() && !uiState.isLoading) {
                        EmptyPostsView()
                }
                else {
                    LazyColumn(modifier = Modifier
                        .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ){
                        items(uiState.posts, key = { it.id }) { post ->

                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                PostCard(
                                    post = post,
                                    onLikeClicked = {},
                                    onCommentClicked = {},
                                    onSaveClicked = {},
                                    onShareClicked = {},
                                    onUserProfileClicked = {},
                                    onHideClicked = {},
                                    onReportClicked = {},
                                    onDeleteClicked = {
                                        if (uiState.isOwner) {
                                            onDeletePost(post.id)
                                        }
                                    },
                                    currentUserId = uiState.currentUserId ?: ""
                                )
                            }
                        }
                    }

                }
            }
            1->{
//                if (uiState.products.isEmpty() && !uiState.isLoading) {
                    EmptyProductView()
//                }
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
    Column  (
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text(
            text = "Chưa có sản phẩm nào",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

    }

}

