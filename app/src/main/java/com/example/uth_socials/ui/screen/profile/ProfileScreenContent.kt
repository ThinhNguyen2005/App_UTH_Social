package com.example.uth_socials.ui.screen.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.profile.ProfileHeader
import com.example.uth_socials.ui.screen.market.ProductItem
import com.example.uth_socials.ui.viewmodel.ProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
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
    onProductClick: (String) -> Unit = {},
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
                products = uiState.products,
                isOwner = uiState.isOwner,
                isFollowing = uiState.isFollowing,
                onFollowClicked = onFollowClicked,
                showBackButton = !uiState.isOwner,
                onBackClicked = onBackClicked,
                onMoreClicked = onBlockUser,
                onSettingClicked = onSettingClicked,
                onMessageClicked = { onMessageClicked(uiState.profileUserId) },
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected
            )
        }

        item {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    val goingRight = targetState > initialState
                    val enterSlide = if (goingRight) {
                        slideInHorizontally(animationSpec = tween(280)) { it } + fadeIn(tween(220))
                    } else {
                        slideInHorizontally(animationSpec = tween(280)) { -it } + fadeIn(tween(220))
                    }
                    val exitSlide = if (goingRight) {
                        slideOutHorizontally(animationSpec = tween(280)) { -it } + fadeOut(tween(180))
                    } else {
                        slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(180))
                    }
                    enterSlide togetherWith exitSlide
                },
                label = "profileTabSwitch"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> PostsTabContent(
                        uiState = uiState,
                        onLikeClicked = onLikeClicked,
                        onCommentClicked = onCommentClicked,
                        onSaveClicked = onSaveClicked,
                        onShareClicked = onShareClicked,
                        onReportClicked = onReportClicked,
                        onDeletePost = onDeletePost,
                        onEditPostClicked = onEditPostClicked
                    )
                    else -> ProductsTabContent(
                        uiState = uiState,
                        onProductClick = onProductClick
                    )
                }
            }
        }
    }
}

@Composable
private fun PostsTabContent(
    uiState: ProfileUiState,
    onLikeClicked: (String) -> Unit,
    onCommentClicked: (String) -> Unit,
    onSaveClicked: (String) -> Unit,
    onShareClicked: (String) -> Unit,
    onReportClicked: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onEditPostClicked: (String) -> Unit
) {
    if (uiState.posts.isEmpty() && !uiState.isLoading) {
        EmptyPostsView()
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        uiState.posts.forEach { post ->
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PostCard(
                    post = post,
                    onLikeClicked = { onLikeClicked(post.id) },
                    onCommentClicked = { onCommentClicked(post.id) },
                    onSaveClicked = { onSaveClicked(post.id) },
                    onShareClicked = { onShareClicked(post.id) },
                    onUserProfileClicked = {},
                    onHideClicked = {},
                    onReportClicked = { onReportClicked(post.id) },
                    onDeleteClicked = {
                        if (uiState.isOwner) onDeletePost(post.id)
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

@Composable
private fun ProductsTabContent(
    uiState: ProfileUiState,
    onProductClick: (String) -> Unit
) {
    if (uiState.products.isEmpty() && !uiState.isLoading) {
        EmptyProductView()
        return
    }
    val productRows = uiState.products.chunked(2)
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))
        productRows.forEach { rowProducts ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    rowProducts.getOrNull(0)?.let { product ->
                        ProductItem(
                            product = product,
                            onClick = { onProductClick(product.id) }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    rowProducts.getOrNull(1)?.let { product ->
                        ProductItem(
                            product = product,
                            onClick = { onProductClick(product.id) }
                        )
                    }
                }
            }
        }
    }
}
