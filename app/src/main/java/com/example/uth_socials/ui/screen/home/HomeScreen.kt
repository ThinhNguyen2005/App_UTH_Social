package com.example.uth_socials.ui.screen.home

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.uth_socials.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    // navController: NavHostController
) {
    val postRepository = remember { PostRepository() } // Dùng remember để không tạo lại mỗi lần recomposition
    val viewModelFactory = remember { ViewModelFactory(postRepository) }
    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)

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
                                    onCommentClicked = {
                                        homeViewModel.onCommentClicked(post.id)
                                    },
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
}
