package com.example.uth_socials.ui.screen.home

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.profile.ProfileHeader
import com.example.uth_socials.ui.viewmodel.ProfileUiState
import com.example.uth_socials.ui.viewmodel.ProfileViewModel
import com.google.firebase.Timestamp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClicked: () -> Unit = {},
    onMessageClicked: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileScreenContent(
        uiState = uiState,
        onBackClicked = onBackClicked,
        onFollowClicked = { viewModel.onFollowClicked() },
        onBlockUser = { viewModel.onBlockUser() },
        onDeletePost = { postId -> viewModel.onDeletePost(postId) },
        onMessageClicked = onMessageClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onBackClicked: () -> Unit,
    onFollowClicked: () -> Unit,
    onBlockUser: () -> Unit,
    onDeletePost: (String) -> Unit,
    onMessageClicked: (String) -> Unit
) {
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
                onMessageClicked = { onMessageClicked(uiState.profileUserId) }
            )
        }

        if (uiState.posts.isEmpty() && !uiState.isLoading) {
            item {
                EmptyPostsView()
            }
        } else {
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

@Composable
private fun EmptyPostsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Chưa có bài viết nào",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val fakePosts = listOf(
        Post(
            id = "1",
            userId = "user1",
            username = "PreviewUser",
            userAvatarUrl = "",
            textContent = "This is a sample post for preview.",
            imageUrls = listOf("https://picsum.photos/seed/1/800/600"),
            likes = 10,
            commentCount = 5,
            timestamp = Timestamp.now()
        ),
        Post(
            id = "2",
            userId = "user1",
            username = "PreviewUser",
            userAvatarUrl = "",
            textContent = "Another beautiful day in the world of Compose!",
            imageUrls = emptyList(),
            likes = 25,
            commentCount = 12,
            timestamp = Timestamp(System.currentTimeMillis() / 1000 - 300, 0) // 5 minutes ago
        )
    )

    val fakeUiState = ProfileUiState(
        posts = fakePosts,
        isOwner = true,
        username = "PreviewUser",
        userAvatarUrl = "",
        followers = 120,
        following = 75,
        bio = "This is a bio for the preview user. I love coding, Jetpack Compose and Android development!",
        isFollowing = false,
        isLoading = false,
        error = null,
        postCount = fakePosts.size,
        currentUserId = "currentUser"
    )

    MaterialTheme {
        ProfileScreenContent(
            uiState = fakeUiState,
            onBackClicked = {},
            onFollowClicked = {},
            onBlockUser = {},
            onDeletePost = {},
            onMessageClicked = {}
        )
    }
}

