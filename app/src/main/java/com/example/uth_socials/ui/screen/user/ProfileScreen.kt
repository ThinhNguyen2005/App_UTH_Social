package com.example.uth_socials.ui.screen.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.profile.ProfileHeader
import com.example.uth_socials.ui.viewmodel.ProfileViewModel
import com.example.uth_socials.ui.theme.UTH_SocialsTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClicked: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    // Thiết lập scroll behavior cho TopAppBar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // Sử dụng LazyListState để theo dõi vị trí cuộn
    val lazyListState = rememberLazyListState()

    // Tính toán scrollOffset để truyền xuống ProfileHeader
    val scrollOffsetPx by remember {
        derivedStateOf {
            // Tính toán offset dựa vào vị trí cuộn của item đầu tiên
            val firstVisibleItem = lazyListState.firstVisibleItemIndex
            val firstVisibleItemOffset = lazyListState.firstVisibleItemScrollOffset

            if (firstVisibleItem == 0) {
                // Nếu đang hiển thị item đầu tiên (header), trả về offset âm
                -firstVisibleItemOffset.toFloat()
            } else {
                // Nếu đã cuộn qua header, trả về giá trị tối đa (header đã thu gọn hoàn toàn)
                -1000f // Một giá trị đủ lớn để đảm bảo header thu gọn hoàn toàn
            }
        }
    }

    // Kiểm tra xem header đã thu gọn đủ để hiển thị tên người dùng trên TopAppBar chưa
    val showTitleInAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 150
        }
    }

    // Sử dụng Scaffold đơn giản không có TopAppBar
    Scaffold { innerPadding ->
        // Sử dụng LazyColumn với trạng thái cuộn đã được theo dõi
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                ProfileHeader(
                    username = state.username,
                    avatarUrl = state.userAvatarUrl,
                    bio = state.bio,
                    followers = state.followers,
                    following = state.following,
                    postCount = state.postCount,
                    isOwner = state.isOwner,
                    isFollowing = state.isFollowing,
                    onFollowClicked = { viewModel.onFollowClicked() },
                    onBackClicked = onBackClicked,
                    onMoreClicked = { viewModel.onBlockUser() },
                    scrollOffsetPx = scrollOffsetPx
                )
            }

            if (state.posts.isEmpty()) {
                item {
                    EmptyPostsView()
                }
            } else {
                items(state.posts, key = { it.id }) { post ->
                    // Thêm padding ngang cho mỗi bài viết
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PostCard(
                            post = post,
                            onLikeClicked = {}, // TODO: Nối các hàm này với ViewModel
                            onCommentClicked = {},
                            onSaveClicked = {},
                            onShareClicked = {},
                            onUserProfileClicked = {},
                            onHideClicked = {},
                            onReportClicked = {},
                            onDeleteClicked = {},
//                            currentUserId = state.currentUserId
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onBackClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (scrollBehavior.state.overlappedFraction > 0.01f)
                        MaterialTheme.colorScheme.onSurface else Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        ),
        scrollBehavior = scrollBehavior
    )
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


