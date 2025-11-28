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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.CircularProgressIndicator
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

import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.user.User
import com.example.uth_socials.ui.component.button.MoreButton
import com.example.uth_socials.ui.component.common.ConfirmDialog

import com.example.uth_socials.ui.component.common.ReportDialog
import com.example.uth_socials.ui.component.post.CommentSheetContent
import com.example.uth_socials.ui.component.post.PostCard
import com.example.uth_socials.ui.component.user.UserCard
import com.example.uth_socials.ui.viewmodel.HomeViewModel
import com.example.uth_socials.ui.viewmodel.PostViewModel
import com.example.uth_socials.ui.viewmodel.SearchViewModel

import com.example.uth_socials.ui.component.common.SectionTitle
import com.example.uth_socials.ui.viewmodel.BlockedUsersViewModel
import com.example.uth_socials.ui.viewmodel.DialogType
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    searchViewModel: SearchViewModel,
//    blockedUsersViewModel: BlockedUsersViewModel,
    navController: NavController
) {
    val resultsPost by searchViewModel.searchPostResults.collectAsState()
    val resultsUser by searchViewModel.searchUserResults.collectAsState()
    var visibleUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    val isLoading by searchViewModel.isLoading.collectAsState()

    val homeViewModel: HomeViewModel = viewModel()

    val uiState by searchViewModel.uiState.collectAsState()

    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val adminStatusCache = remember { mutableStateMapOf<String, Boolean>() }

    val listState = rememberLazyListState()
    val scrollOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }

    var isCollapsed by remember { mutableStateOf(false) }
    val collapseRotate by animateFloatAsState(
        targetValue = if (isCollapsed) {
            -180f
        } else {
            0f
        },
        animationSpec = tween(800)
    )

    val isScrolled by remember {
        derivedStateOf { scrollOffset > 0 }
    }

    val originUserItemValue = 3
    val moreUserItemValue = 3

    LaunchedEffect(resultsUser) {
        visibleUsers = resultsUser.take(originUserItemValue)
    }

    if (isLoading) {
        LoadingScreen()
    } else {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Tiêu đề "Mọi người"
            SectionTitle(title = "Mọi người")

            if (resultsUser.isEmpty()) {
                AnimatedVisibility(
                    visible = !isScrolled,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(500)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 400)
                    ) + fadeOut(animationSpec = tween(400))
                ) {
                    EmptyMessage("Không tìm thấy người dùng nào")
                }
            } else {
                AnimatedVisibility(
                    visible = !isScrolled,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 500)
                    ) + fadeIn(animationSpec = tween(500)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 400)
                    ) + fadeOut(animationSpec = tween(400))
                ) {
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

                        //Nút "Hiển thị thêm"
                        if (resultsUser.size > visibleUsers.size) {
                            MoreButton(
                                onClick = {
                                    visibleUsers = if (visibleUsers.size < resultsUser.size) {
                                        // Hiển thị thêm 3 người
                                        resultsUser.take(
                                            (visibleUsers.size + moreUserItemValue).coerceAtMost(
                                                resultsUser.size
                                            )
                                        )
                                    } else {
                                        // Thu gọn lại chỉ còn 3
                                        resultsUser.take(originUserItemValue)
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

            // Tiêu đề "Bài viết"
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
                        onLikeClicked = { searchViewModel.onLikeClicked(post.id) },
                        onCommentClicked = { searchViewModel.onCommentClicked(post.id) },
                        onSaveClicked = { searchViewModel.onSaveClicked(post.id) },
                        onShareClicked = { searchViewModel.onShareClicked(post.id) },
                        onUserProfileClicked = { },
                        onReportClicked = { searchViewModel.onReportClicked(post.id) },
                        onDeleteClicked = { searchViewModel.onDeleteClicked(post.id) },
                        onHideClicked = { searchViewModel.onHideClicked(post.id) },
                        currentUserId = uiState.currentUserId,
                        //isPostOwnerAdmin = isPostOwnerAdmin
                    )

                        HorizontalDivider(
                            thickness = 0.1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
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

            searchViewModel.onShareDialogLaunched()
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
    if (uiState.commentSheetPostId != null) {
        ModalBottomSheet(
            onDismissRequest = { searchViewModel.onDismissCommentSheet() },
            sheetState = sheetState
        ) {
            CommentSheetContent(
                postId = uiState.commentSheetPostId!!,
                comments = uiState.commentsForSheet,
                isLoading = uiState.isSheetLoading,
                onAddComment = { commentText ->
                    searchViewModel.addComment(uiState.commentSheetPostId!!, commentText)
                },
                onAddCommentReply = { commentParentname, commentParentId, commentText ->
                    homeViewModel.addCommentReply(
                        uiState.commentSheetPostId!!,
                        commentParentname,
                        commentParentId,
                        commentText
                    )
                },
                onLikeComment = searchViewModel::onCommentLikeClicked,
                onUserProfileClick = { },
                commentPostState = uiState.commentPostState,
            )
        }
    }

    // --- REPORT DIALOG ---
    ReportDialog(
        isVisible = uiState.showReportDialog,
        onDismiss = { searchViewModel.onDismissReportDialog() },
        onReportReasonChanged = { searchViewModel.onReportReasonChanged(it) },
        onReportDescriptionChanged = { searchViewModel.onReportDescriptionChanged(it) },
        onSubmit = { searchViewModel.onSubmitReport() },
        reportReason = uiState.reportReason,
        reportDescription = uiState.reportDescription,
        isReporting = uiState.isReporting
    )

    when (uiState.dialogType) {
        is DialogType.None -> { /* Nothing to show */
        }

        is DialogType.DeletePost -> {
            ConfirmDialog(
                isVisible = true,
                onDismiss = { searchViewModel.onDismissDialog() },
                onConfirm = { searchViewModel.onConfirmDialog() },
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
                onDismiss = { searchViewModel.onDismissDialog() },
                onConfirm = { searchViewModel.onConfirmDialog() },
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

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}