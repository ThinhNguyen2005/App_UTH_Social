import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.uth_socials.config.AdminConfig
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.user.User
import com.example.uth_socials.ui.component.button.MoreButton
import com.example.uth_socials.ui.component.common.DeleteConfirmDialog
import com.example.uth_socials.ui.component.common.ReportDialog
import com.example.uth_socials.ui.component.post.CommentSheetContent
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.user.UserCard
import com.example.uth_socials.ui.viewmodel.HomeViewModel
import com.example.uth_socials.ui.viewmodel.PostViewModel
import com.example.uth_socials.ui.viewmodel.SearchViewModel
import com.example.uth_socials.ui.viewmodel.ViewModelFactory
import com.example.uth_socials.ui.component.common.SectionTitle
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    searchViewModel: SearchViewModel,
    navController: NavController
) {
    val resultsPost by searchViewModel.searchPostResults.collectAsState()
    val resultsUser by searchViewModel.searchUserResults.collectAsState()
    var visibleUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    val postRepository = remember { PostRepository() }
    val viewModelFactory = remember { ViewModelFactory(postRepository) }
    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)

    val uiState by homeViewModel.uiState.collectAsState()

    val filteredPosts = remember(uiState.posts, uiState.hiddenPostIds) {
        uiState.posts.filter { it.id !in uiState.hiddenPostIds }
    }

    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val adminStatusCache = remember { mutableStateMapOf<String, Boolean>() }

    val listState = rememberLazyListState()
    val scrollOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }

    var isCollapsed by remember { mutableStateOf(false) }
    val collapseRotate by animateFloatAsState(
        targetValue = if(isCollapsed){ -180f }else{0f},
        animationSpec = tween(800)
    )

    val isScrolled by remember {
        derivedStateOf { scrollOffset > 0 }
    }

    LaunchedEffect(resultsUser) {
        visibleUsers = resultsUser.take(2)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 🔹 Tiêu đề "Mọi người"
            SectionTitle(title = "Mọi người")

            if (resultsUser.isEmpty()) {
                EmptyMessage("Không tìm thấy người dùng nào")
            } else {
                AnimatedVisibility(
                    visible = !isScrolled,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(500)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 400)
                    ) + fadeOut(animationSpec = tween(400))
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        visibleUsers.forEachIndexed { index, user ->
                            // Sử dụng AnimatedVisibility để trượt xuống
                            val visibleState = remember { MutableTransitionState(false) }

                            LaunchedEffect(visibleUsers.size) {
                                delay(index * 230L)
                                visibleState.targetState = true
                            }

                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth }, // trượt từ trên xuống
                                    animationSpec = tween(durationMillis = 300)
                                ) + fadeIn(animationSpec = tween(300)),
                                exit = slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(300))
                            ) {
                                UserCard(user, navController)
                            }
                            Spacer(Modifier.height(13.dp))
                        }

                        // 🔹 Nút "Hiển thị thêm"
                        if (resultsUser.size > 2) {
                            MoreButton(
                                onClick = {
                                    visibleUsers = if (visibleUsers.size < resultsUser.size) {
                                        // Hiển thị thêm 3 người
                                        resultsUser.take(
                                            (visibleUsers.size + 2).coerceAtMost(
                                                resultsUser.size
                                            )
                                        )
                                    } else {
                                        // Thu gọn lại chỉ còn 3
                                        resultsUser.take(2)
                                    }
                                },
                                text = if (visibleUsers.size < resultsUser.size)
                                    "Hiển thị thêm"
                                else
                                    "Thu gọn",
                                rotateIconDegress = collapseRotate
                            )

                            if (visibleUsers.size < resultsUser.size) {
                                isCollapsed = false
                            } else {
                                isCollapsed = true
                            }

                        }
                        //Spacer(Modifier.height(5.dp))

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 🔹 Tiêu đề "Bài viết"
            SectionTitle(title = "Bài viết")

            if (resultsPost.isEmpty()) {
                EmptyMessage("Không tìm thấy bài viết nào")
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 800.dp), // Giới hạn chiều cao để scroll
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(resultsPost) { post ->
                        val isPostOwnerAdmin by remember(post.userId) {
                            mutableStateOf(adminStatusCache[post.userId] ?: false)
                        }

                        PostCard(
                            post = post,
                            onLikeClicked = { homeViewModel.onLikeClicked(post.id) },
                            onCommentClicked = { homeViewModel.onCommentClicked(post.id) },
                            onSaveClicked = { homeViewModel.onSaveClicked(post.id) },
                            onShareClicked = { homeViewModel.onShareClicked(post.id) },
                            onUserProfileClicked = { },
                            onReportClicked = { homeViewModel.onReportClicked(post.id) },
                            onDeleteClicked = { homeViewModel.onDeleteClicked(post.id) },
                            onHideClicked = { homeViewModel.onHideClicked(post.id) },
                            currentUserId = uiState.currentUserId,
                            isPostOwnerAdmin = isPostOwnerAdmin
                        )

                        HorizontalDivider(
                            thickness = 0.1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

        }

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

        if (uiState.commentSheetPostId != null) {
            ModalBottomSheet(
                onDismissRequest = { homeViewModel.onDismissCommentSheet() },
                sheetState = sheetState
            ) {
                CommentSheetContent(
                    postId = uiState.commentSheetPostId!!,
                    comments = uiState.commentsForSheet,
                    isLoading = uiState.isSheetLoading,
                    onAddComment = { commentText ->
                        homeViewModel.addComment(uiState.commentSheetPostId!!, commentText)
                    },
                    onLikeComment = homeViewModel::onCommentLikeClicked,
                    onUserProfileClick = { },
                    commentPostState = uiState.commentPostState,
                    currentUserAvatarUrl = uiState.currentUserAvatarUrl
                )
            }
        }

        // --- 🔸 REPORT DIALOG ---
        ReportDialog(
            isVisible = uiState.showReportDialog,
            onDismiss = { homeViewModel.onDismissReportDialog() },
            onReportReasonChanged = { homeViewModel.onReportReasonChanged(it) },
            onReportDescriptionChanged = { homeViewModel.onReportDescriptionChanged(it) },
            onSubmit = { homeViewModel.onSubmitReport() },
            reportReason = uiState.reportReason,
            reportDescription = uiState.reportDescription,
            isReporting = uiState.isReporting
        )

        // --- 🔸 DELETE CONFIRM DIALOG ---
        DeleteConfirmDialog(
            isVisible = uiState.showDeleteConfirmDialog,
            onDismiss = { homeViewModel.onDismissDeleteDialog() },
            onConfirm = { homeViewModel.onConfirmDelete() },
            isDeleting = uiState.isDeleting
        )
    }

}

@Composable
private fun EmptyMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}