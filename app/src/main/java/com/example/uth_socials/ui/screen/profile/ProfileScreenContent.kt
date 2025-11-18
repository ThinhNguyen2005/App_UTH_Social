package com.example.uth_socials.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    // selectedTabIndex now comes from parent

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
            if (uiState.products.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyProductView()
                }
            } else {
                // Chia products thành các nhóm 2 item mỗi nhóm để tạo grid layout
                val productRows = uiState.products.chunked(2)

                items(productRows.size) { rowIndex ->
                    val rowProducts = productRows[rowIndex]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = if (rowIndex == 0) 12.dp else 0.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Product đầu tiên trong row
                        Box(modifier = Modifier.weight(1f)) {
                            rowProducts.getOrNull(0)?.let { product ->
                                val id = product.id
                                ProductItem(
                                    product = product,
                                    onClick = { onProductClick(id) }
                                )
                            }
                        }

                        // Product thứ hai trong row (nếu có)
                        Box(modifier = Modifier.weight(1f)) {
                            rowProducts.getOrNull(1)?.let { product ->
                                val id = product.id
                                ProductItem(
                                    product = product,
                                    onClick = { onProductClick(id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}