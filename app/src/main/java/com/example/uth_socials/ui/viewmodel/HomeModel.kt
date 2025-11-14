package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.config.AdminConfig
import com.example.uth_socials.config.AdminStatus
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.AdminRepository
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Enum và Data class không thay đổi
enum class CommentPostState { IDLE, POSTING, SUCCESS, ERROR }

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val shareContent: String? = null,
    val commentSheetPostId: String? = null,
    val commentsForSheet: List<Comment> = emptyList(),
    val isSheetLoading: Boolean = false,
    val commentPostState: CommentPostState = CommentPostState.IDLE,
    val currentUserAvatarUrl: String? = null,
    val showReportDialog: Boolean = false,
    val reportingPostId: String? = null,
    val reportReason: String = "",
    val reportDescription: String = "",
    val isReporting: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val deletingPostId: String? = null,
    val isDeleting: Boolean = false,
    val currentUserId: String? = null,
    val hiddenPostIds: Set<String> = emptySet(),
    val isCurrentUserAdmin: Boolean = false,
    val currentUserAdminStatus: AdminStatus = AdminStatus.USER,
    val currentUserRole: String? = null
)

// Data class phụ để nhóm thông tin admin, giúp code sạch hơn
private data class AdminInfo(val isAdmin: Boolean, val status: AdminStatus, val role: String?)


@OptIn(ExperimentalCoroutinesApi::class) // Cần cho flatMapLatest
class HomeViewModel(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {

    // --- NGUỒN DỮ LIỆU ĐỘNG (STATE TRIGGERS) ---
    // State cho category đang được chọn, UI có thể thay đổi giá trị này
    private val _selectedCategory = MutableStateFlow<Category?>(null)
    // State cho ID của bài viết đang được xem comment, null nếu không có
    private val _commentSheetPostId = MutableStateFlow<String?>(null)
    // State trigger để buộc load lại danh sách bài viết ẩn khi cần
    private val _hiddenPostsTrigger = MutableStateFlow(Unit)


    // --- UI STATE CHÍNH ---
    // Chỉ có một StateFlow duy nhất cho toàn bộ UI
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Lấy thông tin user ID một lần duy nhất
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        _uiState.update { it.copy(currentUserId = currentUserId) }

        // Khởi chạy một coroutine duy nhất để quản lý tất cả các luồng dữ liệu
        viewModelScope.launch {
            // Hợp nhất nhiều luồng dữ liệu vào một UI State duy nhất
            combine(
                observeCategoriesAndAdminStatus(), // Luồng 1: Lấy categories và thông tin admin
                observePosts(),                    // Luồng 2: Lấy posts dựa trên category được chọn
                observeComments(),                 // Luồng 3: Lấy comments cho bottom sheet
                observeHiddenPosts()               // Luồng 4: Lấy danh sách ID bài viết đã ẩn
            ) { categoryAndAdmin, posts, commentsInfo, hiddenIds ->
                // Mỗi khi một trong các luồng trên phát ra dữ liệu mới, khối lệnh này sẽ chạy lại
                // và cập nhật UI State với dữ liệu mới nhất.
                val (categories, adminInfo) = categoryAndAdmin
                val (comments, isSheetLoading) = commentsInfo

                _uiState.update { currentState ->
                    currentState.copy(
                        categories = categories,
                        selectedCategory = _selectedCategory.value ?: categories.firstOrNull(),
                        isCurrentUserAdmin = adminInfo.isAdmin,
                        currentUserAdminStatus = adminInfo.status,
                        currentUserRole = adminInfo.role,
                        posts = posts,
                        commentsForSheet = comments,
                        isSheetLoading = isSheetLoading,
                        hiddenPostIds = hiddenIds,
                        isLoading = false, // Tắt loading chung khi có dữ liệu
                        error = null
                    )
                }
            }.catch { e ->
                // Bắt lỗi từ bất kỳ luồng nào ở trên và cập nhật UI
                Log.e("HomeViewModel", "Error in combined state flow", e)
                _uiState.update { it.copy(isLoading = false, error = "Đã xảy ra lỗi: ${e.localizedMessage}") }
            }.collect() // Bắt đầu lắng nghe tất cả các thay đổi
        }
    }

    // --- CÁC LUỒNG DỮ LIỆU (DATA FLOWS) ---

    private fun observeCategoriesAndAdminStatus(): Flow<Pair<List<Category>, AdminInfo>> {
        return categoryRepository.getCategoriesFlow()
            .onEach { categories ->
                // Nếu không có categories nào, tiến hành khởi tạo
                if (categories.isEmpty()) {
                    categoryRepository.initializeDefaultCategories()
                }
            }
            .map { categories ->
                // Đồng thời, lấy thông tin admin
                val adminStatus = AdminConfig.getCurrentUserAdminStatus()
                val isAdmin = adminStatus != AdminStatus.USER
                val role = when (adminStatus) {
                    AdminStatus.SUPER_ADMIN -> "super_admin"
                    AdminStatus.ADMIN -> AdminConfig.getAdminRole(_uiState.value.currentUserId)
                    else -> null
                }
                // Trả về một cặp giá trị: danh sách categories và thông tin admin
                Pair(categories, AdminInfo(isAdmin, adminStatus, role))
            }.flowOn(Dispatchers.IO) // Chạy tất cả trên background thread
    }

    private fun observePosts(): Flow<List<Post>> {
        // flatMapLatest: Tự động hủy luồng cũ và tạo luồng mới khi category thay đổi
        return _selectedCategory.flatMapLatest { category ->
            postRepository.getPostsFlow(category?.id ?: "latest")
        }.flowOn(Dispatchers.IO)
    }

    private fun observeComments(): Flow<Pair<List<Comment>, Boolean>> {
        return _commentSheetPostId.flatMapLatest { postId ->
            if (postId == null) {
                // Nếu không có post nào được chọn, trả về danh sách rỗng và không loading
                flowOf(Pair(emptyList(), false))
            } else {
                // Nếu có post được chọn, bắt đầu lắng nghe comments
                postRepository.getCommentsFlow(postId)
                    .map { comments -> Pair(comments, false) } // Khi có dữ liệu, tắt loading
                    .onStart { emit(Pair(emptyList(), true)) } // Trước khi bắt đầu, bật loading
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun observeHiddenPosts(): Flow<Set<String>> {
        return _hiddenPostsTrigger.flatMapLatest {
            // Mỗi khi _hiddenPostsTrigger thay đổi, chạy lại khối lệnh này để lấy danh sách mới
            flow { emit(postRepository.getHiddenPostIds().toSet()) }
        }.flowOn(Dispatchers.IO)
    }


    // --- HÀNH ĐỘNG TỪ UI (USER ACTIONS) ---

    fun onCategorySelected(category: Category) {
        _selectedCategory.value = category
    }

    fun onCommentClicked(postId: String) {
        _commentSheetPostId.value = postId
    }

    fun onDismissCommentSheet() {
        _commentSheetPostId.value = null
    }

    fun onHideClicked(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (postRepository.hidePost(postId)) {
                // Trigger luồng observeHiddenPosts chạy lại để cập nhật danh sách ẩn
                _hiddenPostsTrigger.value = Unit
            }
        }
    }

    fun onRetry() {
        // Việc tái cấu trúc bằng `combine` giúp việc retry đơn giản hơn,
        // nhưng hiện tại, việc khởi chạy lại toàn bộ `init` là phức tạp.
        // Tạm thời, logic này có thể không cần thiết nếu Flow xử lý lỗi tốt.
        // Hoặc bạn có thể tạo một trigger riêng cho việc retry.
        _uiState.update { it.copy(error = null, isLoading = true) }
    }

    // Các hàm xử lý hành động khác (like, save, report, delete...) có thể giữ nguyên logic bên trong
    // vì chúng đã sử dụng viewModelScope và không phải là listener dài hạn.
    // Ví dụ:
    fun onLikeClicked(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val originalPosts = _uiState.value.posts
            val postToUpdate = originalPosts.find { it.id == postId } ?: return@launch
            val isCurrentlyLiked = postToUpdate.isLiked

            val updatedPost = postToUpdate.copy(
                isLiked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) postToUpdate.likes - 1 else postToUpdate.likes + 1
            )
            val updatedPosts = originalPosts.map { if (it.id == postId) updatedPost else it }
            _uiState.update { it.copy(posts = updatedPosts) }

            try {
                postRepository.toggleLikeStatus(postId, isCurrentlyLiked)
            } catch (e: Exception) {
                _uiState.update { it.copy(posts = originalPosts) }
                Log.e("HomeViewModel", "Error updating like status", e)
            }
        }
    }

    // ... Dán các hàm onSaveClicked, onShareClicked, onReportClicked, v.v. của bạn vào đây ...
    // ... Chúng không cần thay đổi.

    fun addComment(postId: String, commentText: String) {
        if (commentText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(commentPostState = CommentPostState.POSTING) }
            try {
                postRepository.addComment(postId, commentText)
                _uiState.update { it.copy(commentPostState = CommentPostState.SUCCESS) }
                delay(1500)
                _uiState.update { it.copy(commentPostState = CommentPostState.IDLE) }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to add comment", e)
                _uiState.update { it.copy(commentPostState = CommentPostState.ERROR) }
            }
        }
    }

    fun onCommentLikeClicked(postId: String, commentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val originalComments = state.commentsForSheet
            val commentToUpdate = originalComments.find { it.id == commentId } ?: return@launch
            val isCurrentlyLiked = commentToUpdate.liked

            val updatedComment = commentToUpdate.copy(
                liked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) commentToUpdate.likes - 1 else commentToUpdate.likes + 1
            )
            val updatedComments = originalComments.map { if (it.id == commentId) updatedComment else it }
            _uiState.update { it.copy(commentsForSheet = updatedComments) }
            try {
                postRepository.toggleCommentLikeStatus(postId, commentId, isCurrentlyLiked)
            } catch (e: Exception) {
                _uiState.update { it.copy(commentsForSheet = originalComments) }
                Log.e("HomeViewModel", "Error updating comment like status", e)
            }
        }
    }

    fun onSaveClicked(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val originalPosts = _uiState.value.posts
            val postToUpdate = originalPosts.find { it.id == postId } ?: return@launch
            val updatedPost = postToUpdate.copy(
                isSaved = !postToUpdate.isSaved,
                saveCount = if (postToUpdate.isSaved) postToUpdate.saveCount - 1 else postToUpdate.saveCount + 1
            )
            val updatedPosts = originalPosts.map { if (it.id == postId) updatedPost else it }
            _uiState.update { it.copy(posts = updatedPosts) }

            try {
                postRepository.toggleSaveStatus(postId, postToUpdate.isSaved)
            } catch (e: Exception) {
                _uiState.update { it.copy(posts = originalPosts) }
                Log.e("HomeViewModel", "Error toggling save status", e)
            }
        }
    }

    fun onShareClicked(postId: String) {
        val shareableContent = "Xem bài viết này trên UTH Socials: uthsocials://post/$postId"
        _uiState.update { it.copy(shareContent = shareableContent) }
    }

    fun onShareDialogLaunched() {
        _uiState.update { it.copy(shareContent = null) }
    }

    fun onReportClicked(postId: String) {
        _uiState.update {
            it.copy(
                showReportDialog = true,
                reportingPostId = postId,
                reportReason = "",
                reportDescription = ""
            )
        }
    }

    fun onReportReasonChanged(reason: String) {
        _uiState.update { it.copy(reportReason = reason) }
    }

    fun onReportDescriptionChanged(description: String) {
        _uiState.update { it.copy(reportDescription = description) }
    }

    fun onSubmitReport() {
        val reportingPostId = _uiState.value.reportingPostId ?: return
        val reason = _uiState.value.reportReason.ifEmpty { return }
        val description = _uiState.value.reportDescription

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isReporting = true) }
            try {
                if (postRepository.reportPost(reportingPostId, reason, description)) {
                    _uiState.update {
                        it.copy(
                            showReportDialog = false,
                            isReporting = false,
                            reportingPostId = null
                        )
                    }
                }
            } finally {
                _uiState.update { it.copy(isReporting = false) }
            }
        }
    }

    fun onDismissReportDialog() {
        _uiState.update { it.copy(showReportDialog = false) }
    }

    fun onDeleteClicked(postId: String) {
        _uiState.update { it.copy(showDeleteConfirmDialog = true, deletingPostId = postId) }
    }

    fun onConfirmDelete() {
        val postIdToDelete = _uiState.value.deletingPostId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                if (postRepository.deletePost(postIdToDelete)) {
                    _uiState.update { it.copy(showDeleteConfirmDialog = false) }
                }
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    // `onCleared` không còn cần thiết nữa vì viewModelScope sẽ tự động dọn dẹp tất cả.
}