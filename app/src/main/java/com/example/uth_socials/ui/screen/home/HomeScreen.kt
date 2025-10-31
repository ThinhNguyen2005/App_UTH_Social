package com.example.uth_socials.ui.screen.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.ui.component.navigation.FilterTabs
import com.example.uth_socials.ui.component.navigation.HomeBottomNavigation
import com.example.uth_socials.ui.component.logo.HomeTopAppBar
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.viewmodel.HomeViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by homeViewModel.uiState.collectAsState()
    // THÊM MỚI: Lấy context hiện tại để sử dụng cho Intent
    val context = LocalContext.current
// 🔹 Lấy FirebaseAuth để đăng xuất
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    // THÊM MỚI: LaunchedEffect để xử lý hành động share
    // Nó sẽ kích hoạt khi giá trị của uiState.shareContent thay đổi
    LaunchedEffect(uiState.shareContent) {
        uiState.shareContent?.let { content ->
            // Tạo một Intent để chia sẻ văn bản
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            // Tạo chooser để người dùng chọn ứng dụng chia sẻ
            val chooser = Intent.createChooser(intent, "Chia sẻ bài viết qua...")
            context.startActivity(chooser)

            // Sau khi gọi Intent, reset lại state để không bị gọi lại khi recompose
            homeViewModel.onShareDialogLaunched()
        }
    }


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
        Column(modifier = Modifier.padding(innerPadding)) {
            FilterTabs(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category -> homeViewModel.onCategorySelected(category) }
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