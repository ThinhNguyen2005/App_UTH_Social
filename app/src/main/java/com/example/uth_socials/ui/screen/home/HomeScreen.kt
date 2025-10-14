package com.example.uth_socials.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.R // Đảm bảo bạn có file strings.xml để import R
import com.example.uth_socials.ui.component.common.FilterTabs
import com.example.uth_socials.ui.component.common.HomeBottomNavigation
import com.example.uth_socials.ui.component.common.HomeTopAppBar
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.screen.OnboardingLandscapeLayout
import com.example.uth_socials.ui.screen.OnboardingPortraitLayout
import com.example.uth_socials.ui.viewmodel.HomeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            HomeTopAppBar(
                onSearchClick = { /*TODO*/ },
                onMessagesClick = { /*TODO*/ }
            )
        },
        bottomBar = {
            HomeBottomNavigation()
        }
    ) { innerPadding ->
        // Sử dụng Column để xếp thanh FilterTabs và danh sách bài viết
        Column(modifier = Modifier.padding(innerPadding)) {
            // Thanh lọc nội dung
            FilterTabs(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category -> homeViewModel.onCategorySelected(category) }
            )

            // Phần hiển thị nội dung chính
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> CircularProgressIndicator()
                    uiState.error != null -> Text("Lỗi: ${uiState.error}")
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(uiState.posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    onLikeClicked = { homeViewModel.onLikeClicked(post.id) },
                                    onCommentClicked = { homeViewModel.onCommentClicked(post.id) },
                                    onSaveClicked = { homeViewModel.onSaveClicked(post.id) },
                                    onShareClicked = { homeViewModel.onShareClicked(post.id) },
                                    onUserProfileClicked = { homeViewModel.onUserProfileClicked(post.userId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- PREVIEW ---


