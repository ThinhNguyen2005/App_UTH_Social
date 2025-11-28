package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SearchUiState(
    // Trạng thái chung
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val currentUserId: String? = null, // Có thể cần để kiểm tra quyền

    // Trạng thái Dialogs
    val dialogType: DialogType = DialogType.None,
    val isProcessing: Boolean = false, // Dùng cho các hành động cần thời gian xử lý

    // Trạng thái người dùng
    val isUserBanned: Boolean = false,
    val showBanDialog: Boolean = false,

    // Trạng thái Comment Sheet
    val commentSheetPostId: String? = null,
    val commentsForSheet: List<Comment> = emptyList(),
    val isSheetLoading: Boolean = false,
    val commentPostState: CommentPostState = CommentPostState.IDLE,
    val commentErrorMessage: String? = null,

    // Trạng thái Report Dialog
    val showReportDialog: Boolean = false,
    val reportingPostId: String? = null,
    val reportReason: String = "",
    val reportDescription: String = "",
    val isReporting: Boolean = false,
    val reportErrorMessage: String? = null,

    // Trạng thái Chia sẻ
    val shareContent: String? = null
)
class SearchViewModel(
    postRepository: PostRepository = PostRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    private val _searchPostResults = MutableStateFlow<List<Post>>(emptyList())
    val searchPostResults: StateFlow<List<Post>> = _searchPostResults

    private val _searchUserResults = MutableStateFlow<List<User>>(emptyList())
    val searchUserResults: StateFlow<List<User>> = _searchUserResults

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    private val postReAction = PostReAction(postRepository, viewModelScope)

    private var commentsJob: Job? = null

    init {
        // Load currentUserId khi ViewModel được khởi tạo
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = userRepository.getCurrentUserId()
            _uiState.update { it.copy(currentUserId = currentUserId) }
        }
    }

    fun searchPosts(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val formatQuery = query.trim().lowercase()
            val snapshot = Firebase.firestore.collection("posts")
                .orderBy("textContentFormat")
                .startAt(formatQuery)
                .endAt(formatQuery + "\uf8ff")
                .get()
                .await()

            _searchPostResults.value =
                snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
            _isLoading.value = false
            val currentUserId = _uiState.value.currentUserId ?: userRepository.getCurrentUserId()
            if (currentUserId != null && _uiState.value.currentUserId == null) {
                _uiState.update { it.copy(currentUserId = currentUserId) }
            }

            // Enrich posts với isLiked và isSaved
            val enrichedPosts = snapshot.documents.mapNotNull { doc ->
                val post = doc.toObject(Post::class.java) ?: return@mapNotNull null
                val isLiked = currentUserId?.let { post.likedBy.contains(it) } ?: false
                val isSaved = currentUserId?.let { post.savedBy.contains(it) } ?: false
                post.copy(
                    id = doc.id,
                    isLiked = isLiked,
                    isSaved = isSaved
                )
            }

            _searchPostResults.value = enrichedPosts
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            val formatQuery = query.trim().lowercase()
            val snapshot = Firebase.firestore.collection("users")
                .orderBy("usernameFormat")
                .startAt(formatQuery)
                .endAt(formatQuery + "\uf8ff")
                .get()
                .await()

            _searchUserResults.value =
                snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            _isLoading.value = false
        }
    }

    fun onLikeClicked(postId: String) {
        Log.d("SearchViewModel", "onLikeClicked for post ID: $postId")

        postReAction.mutiLikePost(
            postId = postId,
            posts = _searchPostResults.value,
            isUserBanned = _uiState.value.isUserBanned,
            onBanDialog = { _uiState.update { it.copy(showBanDialog = true) } },
            onPostsUpdate = { updatedPosts ->
                _searchPostResults.value = updatedPosts
            },
            onError = { error ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }

    fun onSaveClicked(postId: String) {
        postReAction.mutiSavePost(
            postId = postId,
            posts = _searchPostResults.value,
            isUserBanned = _uiState.value.isUserBanned,
            onBanDialog = { _uiState.update { it.copy(showBanDialog = true) } },
            onPostsUpdate = { updatedPosts ->
                _searchPostResults.value = updatedPosts
            },
            onError = { error ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }
    fun onDeleteClicked(postId: String) {
        val state = _uiState.value
        postReAction.mutiOnDeleteClicked(
            postId = postId,
            posts = _searchPostResults.value,
            currentUserId = state.currentUserId,
            onShowDeleteDialog = { postId ->
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.DeletePost(postId),
                        isProcessing = false
                    )
                }
            },
            onError = { error ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }

    fun onConfirmDialog() {
        when (val dialogType = _uiState.value.dialogType) {
            is DialogType.DeletePost -> {
                onConfirmDelete(dialogType.postId)
            }
            else -> {
                _uiState.update { it.copy(dialogType = DialogType.None) }
            }
        }
    }

    private fun onConfirmDelete(postId: String) {
        postReAction.mutiOnConfirmDelete(
            postId = postId,
            posts = _searchPostResults.value,
            onDeleting = {
                _uiState.update { it.copy(isProcessing = true) }
            },
            onPostsUpdate = { updatedPosts ->
                _searchPostResults.value = updatedPosts
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.None,
                        isProcessing = false,
                        successMessage = "Đã xóa bài viết"
                    )
                }
            },
            onSuccess = { message ->
                // Success đã được xử lý trong onPostsUpdate
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = error
                    )
                }
            }
        )
    }
    fun onHideClicked(postId: String) {
        val currentPosts = _searchPostResults.value

        postReAction.mutiHidePost(
            postId = postId,
            posts = currentPosts,
            onPostsUpdate = { updatedPosts ->
                _searchPostResults.value = updatedPosts
            },
            onSuccess = { msg ->
                _uiState.update { it.copy(successMessage = msg) }
            },
            onError = { msg ->
                _uiState.update { it.copy(error = msg) }
            }
        )
    }

    fun onDismissCommentSheet() {
        postReAction.mutiOnDismissCommentSheet(
            commentsJob = commentsJob,
            onCloseSheet = {
                _uiState.update { it.copy(commentSheetPostId = null) }
            }
        )
    }

    fun onCommentClicked(postId: String) {
        val state = _uiState.value
        postReAction.mutiOnCommentClicked(
            postId = postId,
            isUserBanned = state.isUserBanned,
            onBanDialog = { _uiState.update { it.copy(showBanDialog = true) } },
            onOpenSheet = { postId ->
                _uiState.update { it.copy(commentSheetPostId = postId) }
            },
            onCommentsUpdate = { comments ->
                _uiState.update { it.copy(commentsForSheet = comments) }
            },
            onSheetLoading = { isLoading ->
                _uiState.update { it.copy(isSheetLoading = isLoading) }
            },
            onJobCreated = { job -> commentsJob = job }
        )
    }
    fun onCommentLikeClicked(postId: String, commentId: String) {
        val state = _uiState.value
        postReAction.mutiLikeComment(
            postId = postId,
            commentId = commentId,
            comments = state.commentsForSheet,
            onCommentsUpdate = { updatedComments ->
                _uiState.update { it.copy(commentsForSheet = updatedComments) }
            },
            onError = { error ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }
    fun onDismissReportDialog() {
        postReAction.mutiOnDismissReportDialog {
            _uiState.update {
                it.copy(
                    showReportDialog = false,
                    reportingPostId = null,
                    reportReason = "",
                    reportDescription = "",
                    reportErrorMessage = null,
                    isReporting = false
                )
            }
        }
    }

    fun onReportClicked(postId: String) {
        val state = _uiState.value
        postReAction.mutiOnReportClicked(
            postId = postId,
            isUserBanned = state.isUserBanned,
            onBanDialog = { _uiState.update { it.copy(showBanDialog = true) } },
            onOpenReportDialog = { postId ->
                _uiState.update {
                    it.copy(
                        showReportDialog = true,
                        reportingPostId = postId,
                        reportReason = "",
                        reportDescription = "",
                        successMessage = null
                    )
                }
            }
        )
    }

    fun onReportReasonChanged(reason: String) {
        _uiState.update { it.copy(reportReason = reason) }
    }

    fun onReportDescriptionChanged(description: String) {
        _uiState.update { it.copy(reportDescription = description) }
    }

    // Báo cáo bài viết
    fun onSubmitReport() {
        val state = _uiState.value
        val reportingPostId = state.reportingPostId ?: return
        val reason = state.reportReason
        val description = state.reportDescription

        postReAction.mutiOnSubmitReport(
            reportingPostId = reportingPostId,
            reason = reason,
            description = description,
            posts = _searchPostResults.value,
            onReporting = {
                _uiState.update { it.copy(isReporting = true, reportErrorMessage = null) }
            },
            onSuccess = {
                _uiState.update {
                    it.copy(
                        showReportDialog = false,
                        isReporting = false,
                        reportingPostId = null,
                        reportReason = "",
                        reportDescription = "",
                        reportErrorMessage = null,
                        successMessage = "Báo cáo bài viết thành công."
                    )
                }
            },
            onError = { errorMsg ->
                _uiState.update {
                    it.copy(
                        isReporting = false,
                        reportErrorMessage = errorMsg
                    )
                }
            }
        )
    }


    fun onDismissDialog() {
        _uiState.update { it.copy(dialogType = DialogType.None) }
    }

    fun addComment(postId: String, commentText: String) {
        val state = _uiState.value
        postReAction.mutiAddComment(
            postId = postId,
            commentText = commentText,
            isUserBanned = state.isUserBanned,
            onBanDialog = { _uiState.update { it.copy(showBanDialog = true) } },
            onPosting = {
                _uiState.update {
                    it.copy(
                        commentPostState = CommentPostState.POSTING,
                        commentErrorMessage = null
                    )
                }
            },
            onSuccess = {
                _uiState.update { it.copy(commentPostState = CommentPostState.SUCCESS) }
            },
            onError = { errorMessage ->
                _uiState.update {
                    it.copy(
                        commentPostState = CommentPostState.ERROR,
                        commentErrorMessage = errorMessage
                    )
                }
            }
        )
    }
    fun onShareClicked(postId: String) {
        postReAction.mutiSharePost(postId) { content ->
            _uiState.update { it.copy(shareContent = content) }
        }
    }

    fun onShareDialogLaunched() {
        _uiState.update { it.copy(shareContent = null) }
    }
}

