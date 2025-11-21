package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.market.Product
import com.example.uth_socials.data.repository.ChatRepository
import com.example.uth_socials.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.util.SecurityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import com.example.uth_socials.ui.viewmodel.PostReAction

data class ProfileUiState(
    val posts: List<Post> = emptyList(),
    val products: List<Product> = emptyList(),
    val isOwner: Boolean = false,
    val username: String = "",
    val userAvatarUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val bio: String = "",
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val postCount: Int = 0,
    val productCount: Int = 0,
    val currentUserId: String? = null,
    val profileUserId: String = "",

    val commentSheetPostId: String? = null,
    val commentsForSheet: List<Comment> = emptyList(),
    val isSheetLoading: Boolean = false,
    val commentPostState: CommentPostState = CommentPostState.IDLE,
    val commentErrorMessage: String? = null,

    val isUserBanned: Boolean = false,
    val showBanDialog: Boolean = false,
    val successMessage: String? = null,
    // ✅ Single dialog state thay vì nhiều boolean flags
    val dialogType: DialogType = DialogType.None,
    val isProcessing: Boolean = false,
    val isUserBlocked: Boolean = false,
    val shouldNavigateBack: Boolean = false,

    //edit
    val showEditPostDialog: Boolean = false,
    val editingPostId: String? = null,
    val editingPostContent: String = "",
    val isSavingPost: Boolean = false,
    val editPostErrorMessage: String? = null,
    //reporrt
    val showReportDialog: Boolean = false,
    val reportingPostId: String? = null,
    val reportReason: String = "",
    val reportDescription: String = "",
    val isReporting: Boolean = false,
    val reportErrorMessage: String? = null,
)

class ProfileViewModel(
    private val userId: String,
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val marketRepository: MarketRepository = MarketRepository()
) : ViewModel() {
    private val chatRepository = ChatRepository()
    private var commentsJob: Job? = null

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val postReAction = PostReAction(postRepository, viewModelScope)

    private var postsJob: Job? = null
    private var productsJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        Log.d("ProfileViewModel", "🏁 Loading profile data for userId: $userId")
        _uiState.update { it.copy() }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = userRepository.getCurrentUserId()
                Log.d("ProfileViewModel", "👤 Current user ID: $currentUserId, Profile user ID: $userId")

                // ✅ Kiểm tra xem user đã bị block chưa
                val isBlocked = if (currentUserId != null) {
                    userRepository.isUserBlocked(currentUserId, userId)
                } else {
                    false
                }

                if (isBlocked) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Bạn đã chặn người dùng này.",
                            isUserBlocked = true,
                        )
                    }
                    return@launch
                }

                // Lấy thông tin user và thiết lập real-time listener cho posts
                val user = userRepository.getUser(userId)

                if (user != null) {
                    val isOwner = currentUserId == userId
                    val isFollowing = currentUserId?.let(user.followers::contains) == true

                    // Cập nhật thông tin user
                    _uiState.update {
                        it.copy(
                            isOwner = isOwner,
                            username = user.username,
                            userAvatarUrl = user.avatarUrl,
                            followers = user.followers.size,
                            following = user.following.size,
                            bio = user.bio,
                            isFollowing = isFollowing,
                            currentUserId = currentUserId,
                            profileUserId = userId,
                        )
                    }

                    // Thiết lập real-time listener cho posts và products
                    listenToUserPosts()
                    listenToUserProducts()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Không tìm thấy người dùng.",
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile data", e)
                _uiState.update { it.copy(isLoading = false, error = "Lỗi tải dữ liệu.") }
            }
        }
    }

    private fun listenToUserPosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch(Dispatchers.IO) {
            postRepository.getPostsForUserFlow(userId).collect { posts ->
                _uiState.update {
                    it.copy(
                        posts = posts,
                        isLoading = false,
                        postCount = posts.size,
                        error = null
                    )
                }
            }
        }
    }

    private fun listenToUserProducts() {
        productsJob?.cancel()
        Log.d("ProfileViewModel", "🔍 Listening to products for userId: $userId")
        productsJob = viewModelScope.launch(Dispatchers.IO) {
            marketRepository.getProductsForUserFlow(userId).collect { products ->
                Log.d("ProfileViewModel", "📦 Found ${products.size} products for user $userId")
                products.forEach { product ->
                    Log.d("ProfileViewModel", "   Product: ${product.name} (userId: ${product.userId})")
                }
                _uiState.update {
                    it.copy(
                        products = products,
                        productCount = products.size
                    )
                }
            }
        }
    }

    fun onFollowClicked() {
        val state = _uiState.value
        if (state.isOwner) return
        val resolvedCurrentUserId =
            state.currentUserId ?: userRepository.getCurrentUserId() ?: return
        if (state.currentUserId == null) {
            _uiState.update { it.copy(currentUserId = resolvedCurrentUserId) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val isCurrentlyFollowing = _uiState.value.isFollowing
            val success =
                userRepository.toggleFollow(resolvedCurrentUserId, userId, isCurrentlyFollowing)
            if (success) {
                _uiState.update {
                    it.copy(
                        followers = if (isCurrentlyFollowing) it.followers - 1 else it.followers + 1,
                        isFollowing = !isCurrentlyFollowing,
                    )
                }
            }
        }
    }

    fun onBlockUser() {
        val state = _uiState.value
        if (state.isOwner) return

        // ✅ Hiển thị dialog xác nhận
        _uiState.update {
            it.copy(
                dialogType = DialogType.BlockUser(
                    userId = userId,
                    username = state.username
                ),
            )
        }
    }

    fun onConfirmDialog() {
        when (val dialog = _uiState.value.dialogType) {
            is DialogType.DeletePost -> onConfirmDelete(dialog.postId)
            is DialogType.BlockUser -> onConfirmBlock(dialog.userId)
            is DialogType.UnblockUser -> {
                // Được xử lý ở BlockedUsersViewModel
            }

            is DialogType.None -> return
        }
    }

    fun onDismissDialog() {
        _uiState.update {
            it.copy(
                dialogType = DialogType.None,
                isProcessing = false
            )
        }
    }

    private fun onConfirmBlock(targetUserId: String) {
        val state = _uiState.value
        val currentUserId = state.currentUserId ?: userRepository.getCurrentUserId() ?: return

        if (state.currentUserId == null) {
            _uiState.update { it.copy(currentUserId = currentUserId) }
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = userRepository.blockUser(currentUserId, targetUserId)

                if (success) {
                    _uiState.update {
                        it.copy(
                            dialogType = DialogType.None,
                            isProcessing = false,
                            successMessage = "Người dùng đã bị chặn.",
                            isUserBlocked = true,
                            shouldNavigateBack = true,
                        )
                    }
                    Log.d("ProfileViewModel", "User blocked successfully: $targetUserId")
                } else {
                    _uiState.update {
                        it.copy(
                            dialogType = DialogType.None,
                            isProcessing = false,
                            error = "Lỗi không thể chặn người dùng.",
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error blocking user", e)
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.None,
                        isProcessing = false,
                        error = "Lỗi khi chặn người dùng. Vui lòng thử lại.",
                    )
                }
            }
        }
    }


    fun openChatWithUser(targetUserId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUserId = userRepository.getCurrentUserId()

                if (currentUserId == null) {
                    Log.w("ProfileViewModel", "Cannot open chat: User not logged in")
                    return@launch
                }                    // 🔹 Kiểm tra chat đã tồn tại chưa
                val existingChatId = chatRepository.getExistingChatId(targetUserId)

                // 🔹 Nếu có rồi → mở ngay
                if (existingChatId != null) {
                    onChatReady(existingChatId)
                } else {
                    // 🔹 Nếu chưa có → tạo chatId tạm để vào ChatScreen trống
                    val newChatId = chatRepository.buildChatId(currentUserId, targetUserId)
                    onChatReady(newChatId)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi mở chat", e)
            }
        }
    }

    fun onDeleteClicked(postId: String) {
        val state = _uiState.value
        postReAction.mutiOnDeleteClicked(
            postId = postId,
            posts = state.posts,
            currentUserId = state.currentUserId,
            onShowDeleteDialog = { deletePostId: String ->
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.DeletePost(deletePostId),
                        isProcessing = false
                    )
                }
            },
            onError = { error: String ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }

    private fun onConfirmDelete(postId: String) {
        val state = _uiState.value
        postReAction.mutiOnConfirmDelete(
            postId = postId,
            posts = state.posts,
            onDeleting = {
                _uiState.update { it.copy(isProcessing = true) }
            },
            onPostsUpdate = { updatedPosts: List<Post> ->
                _uiState.update {
                    it.copy(
                        posts = updatedPosts,
                        postCount = updatedPosts.size,
                        dialogType = DialogType.None,
                        isProcessing = false,
                        successMessage = "Bài viết đã được xóa thành công."
                    )
                }
            },
            onSuccess = { message: String ->
                // Success đã được xử lý trong onPostsUpdate
            },
            onError = { error: String ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = error
                    )
                }
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
            onOpenReportDialog = { reportPostId: String ->
                _uiState.update {
                    it.copy(
                        showReportDialog = true,
                        reportingPostId = reportPostId,
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
            posts = state.posts,
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
            onError = { errorMsg: String ->
                _uiState.update {
                    it.copy(
                        isReporting = false,
                        reportErrorMessage = errorMsg
                    )
                }
            }
        )
    }
    fun onCommentClicked(postId: String) {
        val state = _uiState.value
        postReAction.mutiOnCommentClicked(
            postId = postId,
            isUserBanned = state.isUserBanned,
            onBanDialog = { _uiState.update { it.copy(showBanDialog = true) } },
            onOpenSheet = { sheetPostId ->
                _uiState.update { it.copy(commentSheetPostId = sheetPostId) }
            },
            onCommentsUpdate = { comments: List<Comment> ->
                _uiState.update { it.copy(commentsForSheet = comments) }
            },
            onSheetLoading = { isLoading: Boolean ->
                _uiState.update { it.copy(isSheetLoading = isLoading) }
            },
            onJobCreated = { job: Job -> commentsJob = job }
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
    fun onLikeClicked(postId: String) {
        val state = _uiState.value
        postReAction.mutiLikePost(
            postId = postId,
            posts = state.posts,
            isUserBanned = state.isUserBanned,
            onBanDialog = { _uiState.update { it.copy(showBanDialog = true) } },
            onPostsUpdate = { updatedPosts: List<Post> ->
                _uiState.update { it.copy(posts = updatedPosts) }
            },
            onError = { error: String ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }
    fun onSaveClicked(postId: String) {
        val state = _uiState.value
        postReAction.mutiSavePost(
            postId = postId,
            posts = state.posts,
            isUserBanned = state.isUserBanned,
            onBanDialog = { _uiState.update { it.copy(showBanDialog = true) } },
            onPostsUpdate = { updatedPosts: List<Post> ->
                _uiState.update { it.copy(posts = updatedPosts) }
            },
            onError = { error: String ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }
    fun onCommentLikeClicked(postId: String, commentId: String) {
        val state = _uiState.value
        postReAction.mutiLikeComment(
            postId = postId,
            commentId = commentId,
            comments = state.commentsForSheet,
            onCommentsUpdate = { updatedComments: List<Comment> ->
                _uiState.update { it.copy(commentsForSheet = updatedComments) }
            },
            onError = { error: String ->
                _uiState.update { it.copy(error = error) }
            }
        )
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
    fun onEditPostClicked(postId: String) {
        val state = _uiState.value
        postReAction.mutiOnEditPostClicked(
            postId = postId,
            posts = state.posts,
            onShowEditDialog = { editPostId: String, content: String ->
                _uiState.update {
                    it.copy(
                        showEditPostDialog = true,
                        editingPostId = editPostId,
                        editingPostContent = content,
                        editPostErrorMessage = null
                    )
                }
            }
        )
    }

    fun onUpdatePostContent(newContent: String) {
        _uiState.update { it.copy(editingPostContent = newContent) }
    }

    fun onSaveEditedPost() {
        val state = _uiState.value
        val postId = state.editingPostId ?: return
        val newContent = state.editingPostContent

        postReAction.mutiSaveEditedPost(
            postId = postId,
            newContent = newContent,
            posts = state.posts,
            onSaving = {
                _uiState.update {
                    it.copy(
                        isSavingPost = true,
                        editPostErrorMessage = null
                    )
                }
            },
            onPostsUpdate = { updatedPosts: List<Post> ->
                _uiState.update {
                    it.copy(
                        posts = updatedPosts,
                        showEditPostDialog = false,
                        editingPostId = null,
                        editingPostContent = "",
                        isSavingPost = false,
                        editPostErrorMessage = null,
                        successMessage = "Bài viết đã được cập nhật."
                    )
                }
            },
            onSuccess = {
                // Success đã được xử lý trong onPostsUpdate
            },
            onError = { errorMessage: String ->
                _uiState.update {
                    it.copy(
                        isSavingPost = false,
                        editPostErrorMessage = errorMessage,
                        error = "Không thể cập nhật được bài viết! Vui lòng thử lại"
                    )
                }
            }
        )
    }

    fun onDismissEditDialog() {
        postReAction.mutiOnDismissEditDialog {
            _uiState.update {
                it.copy(
                    showEditPostDialog = false,
                    editingPostId = null,
                    editingPostContent = "",
                    editPostErrorMessage = null,
                    isSavingPost = false
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        postsJob?.cancel()
        productsJob?.cancel()
        commentsJob?.cancel()
    }
}

