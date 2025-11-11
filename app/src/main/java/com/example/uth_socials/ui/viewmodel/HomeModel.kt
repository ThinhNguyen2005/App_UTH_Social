package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.example.uth_socials.data.util.SecurityValidator


//Enum để quản lý trạng thái gửi bình luận
enum class CommentPostState { IDLE, POSTING, SUCCESS, ERROR }

// Cập nhật State để làm việc với object Category
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
    val commentErrorMessage: String? = null, // Thêm error message cho comments
    val currentUserAvatarUrl: String? = null,
    // 🔸 Thêm state cho report dialog

    val showReportDialog: Boolean = false,
    val reportingPostId: String? = null,
    val reportReason: String = "",
    val reportDescription: String = "",
    val isReporting: Boolean = false,
    val reportErrorMessage: String? = null,  // 🔸 Thêm error message cho report
    // 🔸 Thêm state cho delete confirmation dialog
    val showDeleteConfirmDialog: Boolean = false,
    val deletingPostId: String? = null,
    val isDeleting: Boolean = false,
    val currentUserId: String? = null,
    val hiddenPostIds: Set<String> = emptySet(),
    // 🔸 Admin state
    val isCurrentUserAdmin: Boolean = false,
    val currentUserRole: String? = null,
    // 🔸 Generic confirmation dialog
    val showGenericDialog: Boolean = false,
    val genericDialogAction: (() -> Unit)? = null
)

/**
 * HomeViewModel - Quản lý toàn bộ logic của màn hình chính (Home Screen)
 *
 * Chức năng chính:
 * - Quản lý danh sách bài viết theo category
 * - Xử lý tương tác bài viết (like, save, share, hide)
 * - Quản lý hệ thống bình luận
 * - Xử lý báo cáo và xóa bài viết
 * - Quản lý quyền admin và moderation
 *
 * Kiến trúc: MVVM với StateFlow cho reactive UI updates
 */
class HomeViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    private val adminStatusCache = mutableMapOf<String, Boolean>()

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var commentsJob: Job? = null
    private var categoriesJob: Job? = null
    private val savingPosts = mutableSetOf<String>()

    // ==========================================
    // INITIALIZATION (Khởi tạo ViewModel)
    // ==========================================

    /**
     * Block khởi tạo ViewModel - chạy ngay khi ViewModel được tạo
     *
     * Thứ tự thực hiện:
     * 1. loadCurrentUser() - Lấy thông tin user hiện tại
     * 2. loadCategoriesAndInitialPosts() - Load categories và posts ban đầu
     * 3. loadHiddenPosts() - Load danh sách bài viết đã ẩn
     * 4. checkAdminStatus() - Delay để tránh blocking UI (không cần lúc khởi động)
     *
     * ✅ OPTIMIZATION: Delay admin check để tránh skipped frames
     */
    init {
        loadCurrentUser()
        loadCategoriesAndInitialPosts()
        loadHiddenPosts()
        // ✅ Delay admin check đến sau 1.5s - không cần lúc khởi động
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            checkAdminStatus()
        }
    }

    /**
     * Load thông tin user hiện tại từ Firebase Auth
     *
     * Logic:
     * - Lấy currentUser từ FirebaseAuth
     * - Nếu có user, lưu userId và refresh admin status
     * - Chạy trên background thread (IO dispatcher)
     */
    private fun loadCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                _uiState.update { it.copy(currentUserId = currentUser.uid) }
                refreshAdminStatus()
            }
        }
    }

    private fun loadCategoriesAndInitialPosts() {
        // Khởi tạo loading state
        _uiState.update { it.copy(isLoading = true, error = null) }

        // 🔧 Chạy trên background thread để tránh blocking main thread
        viewModelScope.launch(Dispatchers.IO) {
            // Lắng nghe categories real-time
            listenToCategoriesChanges()

            // Load posts với category mặc định ban đầu (fallback)
            listenToPostChanges("all") // Sử dụng "all" làm mặc định
        }
    }

    /**
     * Lắng nghe thay đổi categories theo thời gian thực
     */
    private fun listenToCategoriesChanges() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.getCategoriesFlow().collect { categories ->
                if (categories.isEmpty()) {
                    // Nếu chưa có categories, thử tạo mặc định
                    initializeDefaultCategoriesIfNeeded()
                } else {
                    // Cập nhật categories và chọn category đầu tiên nếu chưa có selectedCategory
                    _uiState.update { currentState ->
                        val newSelectedCategory = currentState.selectedCategory
                            ?: categories.firstOrNull()

                        currentState.copy(
                            categories = categories,
                            selectedCategory = newSelectedCategory,
                            isLoading = false,
                            error = null
                        )
                    }

                    // Nếu đây là lần đầu load categories, bắt đầu lắng nghe posts
                    val currentState = _uiState.value
                    if (currentState.selectedCategory == null && categories.isNotEmpty()) {
                        listenToPostChanges(categories.first().id)
                    }
                }
            }
        }
    }

    /**
     * Khởi tạo categories mặc định nếu cần (Chạy trên IO thread)
     */
    private suspend fun initializeDefaultCategoriesIfNeeded() {
        try {
            // 🔧 Chạy trên IO thread để tránh blocking main thread
            withContext(Dispatchers.IO) {
                val existingCategories = categoryRepository.getCategories()
                if (existingCategories.isNotEmpty()) {
                    // Nếu đã có categories, emit chúng
                    _uiState.update {
                        it.copy(
                            categories = existingCategories,
                            selectedCategory = existingCategories.firstOrNull(),
                            isLoading = false
                        )
                    }
                } else {
                    // Nếu thực sự chưa có, tạo mặc định
                    categoryRepository.initializeDefaultCategories()
                    // Sau khi tạo, Flow sẽ tự động emit lại
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error initializing categories", e)
            _uiState.update {
                it.copy(
                    error = "Lỗi khởi tạo danh mục: ${e.localizedMessage ?: "Không xác định"}",
                    isLoading = false
                )
            }
        }
    }

    private fun loadHiddenPosts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hiddenIds = postRepository.getHiddenPostIds()
                _uiState.update { it.copy(hiddenPostIds = hiddenIds.toSet()) }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading hidden posts", e)
            }
        }
    }


    /**
     * Check and update current user's admin status
     */
     private fun checkAdminStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ✅ OPTIMIZATION: Chỉ check admin status, không gọi init (đã gọi ở loadCurrentUser)
                val isAdmin = adminRepository.isCurrentUserAdmin()
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val role = if (isAdmin) {
                    adminRepository.getAdminRole(currentUserId ?: "")
                } else null

                _uiState.update {
                    it.copy(
                        isCurrentUserAdmin = isAdmin,
                        currentUserRole = role
                    )
                }
                Log.d("HomeViewModel", "Admin status checked: isAdmin=$isAdmin, role=$role")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error checking admin status", e)
                _uiState.update { it.copy(
                    isCurrentUserAdmin = false,
                    currentUserRole = null
                )}
            }
        }
    }

    private fun refreshAdminStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val (isAdmin, isSuperAdmin) = SecurityValidator.getCachedAdminStatus(currentUserId)

                val role = when {
                    isSuperAdmin -> "super_admin"
                    isAdmin -> "admin"
                    else -> null
                }

                _uiState.update {
                    it.copy(
                        isCurrentUserAdmin = isAdmin || isSuperAdmin,
                        currentUserRole = role
                    )
                }

                Log.d("HomeViewModel", "Admin status refreshed: isAdmin=${isAdmin || isSuperAdmin}, role=$role")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing admin status", e)
                // Fallback to non-admin status
                _uiState.update {
                    it.copy(
                        isCurrentUserAdmin = false,
                        currentUserRole = null
                    )
                }
            }
        }
    }
    /**
     * Khởi tạo quản trị viên cấp cao trong Firebase nếu chưa thực hiện
     * Thao tác này sẽ di chuyển quản trị viên cấp cao được mã hóa cứng sang Firebase
     */
    // ✅ REMOVED: initializeSuperAdminIfNeeded() - gọi ở lần đầu login, không cần lúc khởi động ViewModel
    // Super admin initialization nên được xử lý ở AuthViewModel lúc login, không phải lúc mở Home
    // Điều này sẽ giảm tác vụ nặng lúc khởi động app


    suspend fun getAdminStatus(userId: String): Boolean {
        return adminStatusCache.getOrPut(userId) {
            try {
                adminRepository.isAdmin(userId) || adminRepository.isSuperAdmin(userId)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Lỗi khi kiểm tra trạng thái quản trị viên", e)
                false
            }
        }
    }


    private var postsJob: Job? = null

    private fun listenToPostChanges(categoryId: String) {
        postsJob?.cancel()
        postsJob = viewModelScope.launch(Dispatchers.IO) {
            postRepository.getPostsFlow(categoryId).collect { posts ->
                _uiState.update { it.copy(posts = posts, isLoading = false) }
            }
        }
    }

    fun onCategorySelected(category: Category) {
        if (_uiState.value.selectedCategory?.id != category.id) {
            _uiState.update { it.copy(selectedCategory = category, isLoading = true) }
            listenToPostChanges(category.id)
        }
    }

    // --- LOGIC XỬ LÝ CÁC HÀNH ĐỘNG ---

    fun onLikeClicked(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Bước 1: Cập nhật UI ngay lập tức (Optimistic Update)
            val originalPosts = _uiState.value.posts
            val postToUpdate = originalPosts.find { it.id == postId } ?: return@launch
            val isCurrentlyLiked = postToUpdate.isLiked

            val updatedPost = postToUpdate.copy(
                isLiked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) postToUpdate.likes - 1 else postToUpdate.likes + 1
            )
            val updatedPosts = originalPosts.map { if (it.id == postId) updatedPost else it }
            _uiState.update { it.copy(posts = updatedPosts) }

            // Bước 2: Gọi Repository để cập nhật dữ liệu trên server
            try {
                postRepository.toggleLikeStatus(postId, isCurrentlyLiked)
            } catch (e: Exception) {
                // Nếu có lỗi, khôi phục lại trạng thái UI ban đầu
                _uiState.update { it.copy(posts = originalPosts) }
                Log.e("HomeViewModel", "Error updating like status", e)
            }
        }
    }

    fun onCommentClicked(postId: String) {
        commentsJob?.cancel()

        // Cập nhật state để hiển thị sheet và trạng thái loading
        _uiState.update {
            it.copy(
                commentSheetPostId = postId,
                isSheetLoading = true,
                commentsForSheet = emptyList()
            )
        }

        // Bắt đầu một coroutine mới để lắng nghe bình luận cho postId mới
        commentsJob = viewModelScope.launch(Dispatchers.IO) {
            postRepository.getCommentsFlow(postId).collect { comments ->
                _uiState.update {
                    it.copy(
                        commentsForSheet = comments,
                        isSheetLoading = false
                    )
                }
            }
        }

        Log.d("HomeViewModel", "Comment clicked for post: $postId")
    }

    fun addComment(postId: String, commentText: String) {
        Log.d("HomeViewModel", "addComment called with postId: $postId, commentText: '$commentText'")
        if (commentText.isBlank()) {
            Log.w("HomeViewModel", "Comment text is blank, returning early")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Cập nhật UI sang trạng thái "Đang gửi"
            _uiState.update {
                it.copy(
                    commentPostState = CommentPostState.POSTING,
                    commentErrorMessage = null // Clear previous error
                )
            }

            val result = postRepository.addComment(postId, commentText)
            result.onSuccess {
                // 2. Cập nhật UI sang trạng thái "Thành công"
                _uiState.update { it.copy(commentPostState = CommentPostState.SUCCESS) }
                // 3. Reset lại trạng thái sau một khoảng thời gian ngắn
                delay(1500)
                _uiState.update {
                    it.copy(
                        commentPostState = CommentPostState.IDLE,
                        commentErrorMessage = null
                    )
                }
            }.onFailure { e ->
                Log.e("HomeViewModel", "Failed to add comment", e)
                // Show specific error message based on exception
                val errorMessage = when (e) {
                    is IllegalStateException -> e.message ?: "Lỗi không xác định"
                    else -> "Không thể gửi bình luận. Vui lòng thử lại."
                }
                Log.e("HomeViewModel", "Comment error: $errorMessage")
                _uiState.update {
                    it.copy(
                        commentPostState = CommentPostState.ERROR,
                        commentErrorMessage = errorMessage
                    )
                }
            }
        }
    }

    fun onCommentLikeClicked(postId: String, commentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val originalComments = state.commentsForSheet
            val commentToUpdate = originalComments.find { it.id == commentId } ?: return@launch
            val isCurrentlyLiked = commentToUpdate.liked

            // 1. Cập nhật UI ngay lập tức
            val updatedComment = commentToUpdate.copy(
                liked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) commentToUpdate.likes - 1 else commentToUpdate.likes + 1
            )
            val updatedComments = originalComments.map { if (it.id == commentId) updatedComment else it }
            _uiState.update { it.copy(commentsForSheet = updatedComments) }
            try {
                postRepository.toggleCommentLikeStatus(postId, commentId, isCurrentlyLiked)
                Log.d("HomeViewModel", "Toggled comment like: $commentId")
            } catch (e: Exception) {
                // 3. Nếu lỗi, khôi phục lại trạng thái cũ
                _uiState.update { it.copy(commentsForSheet = originalComments) }
                Log.e("HomeViewModel", "Error updating comment like status", e)
            }
        }
    }

    fun onDismissCommentSheet() {
        commentsJob?.cancel()
        _uiState.update { it.copy(commentSheetPostId = null) }
    }

    fun onSaveClicked(postId: String) {
        // Nếu đang xử lý thì bỏ qua
        if (savingPosts.contains(postId)) return

        viewModelScope.launch(Dispatchers.IO) {
            val originalPosts = _uiState.value.posts
            val postToUpdate = originalPosts.find { it.id == postId } ?: return@launch

            // Thêm vào set để chống spam click
            savingPosts.add(postId)

            // 1. Optimistic UI Update (cập nhật giao diện ngay lập tức)
            val updatedPost = postToUpdate.copy(
                isSaved = !postToUpdate.isSaved, // Đảo ngược trạng thái hiện tại
                saveCount = if (postToUpdate.isSaved) postToUpdate.saveCount - 1 else postToUpdate.saveCount + 1
            )
            val updatedPosts = originalPosts.map { if (it.id == postId) updatedPost else it }
            _uiState.update { it.copy(posts = updatedPosts) }

            // 2. Gọi Repository để cập nhật server
            try {
                postRepository.toggleSaveStatus(postId, postToUpdate.isSaved)
            } catch (e: Exception) {
                // 3. Nếu lỗi, khôi phục lại trạng thái cũ
                _uiState.update { it.copy(posts = originalPosts) }
                Log.e("HomeViewModel", "Error toggling save status", e)
            } finally {
                // 4. Xóa khỏi set sau khi hoàn thành
                savingPosts.remove(postId)
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

    // --- 🔸 HÀM XỬ LÝ ẨN BÀI VIẾT ---
    fun onHideClicked(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = postRepository.hidePost(postId)
                if (success) {
                    // Cập nhật UI: thêm postId vào hiddenPostIds
                    _uiState.update {
                        it.copy(hiddenPostIds = it.hiddenPostIds + postId)
                    }
                    // Lọc bài viết ẩn ra khỏi danh sách
                    val filteredPosts = _uiState.value.posts.filter { it.id != postId }
                    _uiState.update { it.copy(posts = filteredPosts) }
                    Log.d("HomeViewModel", "Post hidden successfully: $postId")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error hiding post", e)
            }
        }
    }

    // --- 🔸 HÀM MỞ DIALOG BÁO CÁO ---
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

    // --- 🔸 HÀM GỬI BÁO CÁO ---
    fun onSubmitReport() {
        val reportingPostId = _uiState.value.reportingPostId ?: return
        val reason = _uiState.value.reportReason.ifEmpty { return }
        val description = _uiState.value.reportDescription

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isReporting = true, reportErrorMessage = null) }
            try {
                val success = postRepository.reportPost(reportingPostId, reason, description)
                if (success) {
                    _uiState.update {
                        it.copy(
                            showReportDialog = false,
                            isReporting = false,
                            reportingPostId = null,
                            reportReason = "",
                            reportDescription = "",
                            reportErrorMessage = null
                        )
                    }
                    Log.d("HomeViewModel", "Report submitted successfully")
                } else {
                    // ✅ FIX: Xử lý khi báo cáo thất bại
                    val errorMsg = "Gửi báo cáo thất bại. Vui lòng thử lại."
                    _uiState.update {
                        it.copy(
                            isReporting = false,
                            reportErrorMessage = errorMsg
                        )
                    }
                    Log.w("HomeViewModel", "Report submission failed: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error submitting report", e)
                val errorMsg = "Lỗi khi gửi báo cáo: ${e.message ?: "Vui lòng thử lại"}"
                _uiState.update {
                    it.copy(
                        isReporting = false,
                        reportErrorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun onDismissReportDialog() {
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

    // --- 🔸 HÀM MỞ DIALOG XÓA BÀI VIẾT ---
    fun onDeleteClicked(postId: String) {
        // Kiểm tra xem người dùng hiện tại có phải chủ bài không
        val post = _uiState.value.posts.find { it.id == postId }
        if (post?.userId == _uiState.value.currentUserId) {
            _uiState.update {
                it.copy(
                    showDeleteConfirmDialog = true,
                    deletingPostId = postId
                )
            }
        }
    }

    // --- 🔸 HÀM XÓA BÀI VIẾT ---
    fun onConfirmDelete() {
        val postIdToDelete = _uiState.value.deletingPostId ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                val success = postRepository.deletePost(postIdToDelete)
                if (success) {
                    // Xóa bài viết khỏi danh sách
                    val updatedPosts = _uiState.value.posts.filter { it.id != postIdToDelete }
                    _uiState.update {
                        it.copy(
                            posts = updatedPosts,
                            showDeleteConfirmDialog = false,
                            isDeleting = false,
                            deletingPostId = null
                        )
                    }
                    Log.d("HomeViewModel", "Post deleted successfully: $postIdToDelete")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error deleting post", e)
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun onDismissDeleteDialog() {
        _uiState.update {
            it.copy(
                showDeleteConfirmDialog = false,
                deletingPostId = null
            )
        }
    }

    fun onRetry() {
        _uiState.update { it.copy(error = null, isLoading = true) }
        // Restart categories listener
        listenToCategoriesChanges()
    }

    override fun onCleared() {
        super.onCleared()
        categoriesJob?.cancel()
        commentsJob?.cancel()
        postsJob?.cancel()
    }
}