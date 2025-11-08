package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.config.AdminConfig
import com.example.uth_socials.config.AdminStatus
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.repository.AdminRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



//Enum để quản lý trạng thái gửi bình luận
enum class CommentPostState { IDLE, POSTING, SUCCESS, ERROR }

// 🔸 Pagination State
data class PaginationState(
    val currentPage: Int = 0,
    val pageSize: Int = 10,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

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
    val currentUserAvatarUrl: String? = null,
    // 🔸 Thêm state cho report dialog
    val showReportDialog: Boolean = false,
    val reportingPostId: String? = null,
    val reportReason: String = "",
    val reportDescription: String = "",
    val isReporting: Boolean = false,
    // 🔸 Thêm state cho delete confirmation dialog
    val showDeleteConfirmDialog: Boolean = false,
    val deletingPostId: String? = null,
    val isDeleting: Boolean = false,
    val currentUserId: String? = null,
    val hiddenPostIds: Set<String> = emptySet(),
    // 🔸 Pagination state
    val paginationState: PaginationState = PaginationState(),
    // 🔸 Admin state
    val isCurrentUserAdmin: Boolean = false,
    val currentUserAdminStatus: AdminStatus = AdminStatus.USER,
    val currentUserRole: String? = null
)

class HomeViewModel(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var commentsJob: Job? = null
    private var categoriesJob: Job? = null
    private val savingPosts = mutableSetOf<String>()

    init {
        loadCurrentUser()
        loadCategoriesAndInitialPosts()
        loadHiddenPosts()
        checkAdminStatus()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                _uiState.update { it.copy(currentUserId = currentUser.uid) }
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
                // First, ensure super admin is initialized in Firebase
                initializeSuperAdminIfNeeded()

                val adminStatus = AdminConfig.getCurrentUserAdminStatus()
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                val isAdmin = adminStatus != AdminStatus.USER
                val role = when (adminStatus) {
                    AdminStatus.SUPER_ADMIN -> "super_admin"
                    AdminStatus.ADMIN -> AdminConfig.getAdminRole(currentUserId)
                    AdminStatus.USER -> null
                }

                _uiState.update { it.copy(
                    isCurrentUserAdmin = isAdmin,
                    currentUserAdminStatus = adminStatus,
                    currentUserRole = role
                )}

                Log.d("HomeViewModel", "Admin check: isAdmin=$isAdmin, status=$adminStatus, role=$role")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error checking admin status", e)
                // Fallback to user status on error
                _uiState.update { it.copy(
                    isCurrentUserAdmin = false,
                    currentUserAdminStatus = AdminStatus.USER,
                    currentUserRole = null
                )}
            }
        }
    }

    /**
     * Initialize super admin in Firebase if not already done
     * This migrates the legacy hard-coded super admin to Firebase
     */
    private suspend fun initializeSuperAdminIfNeeded() {
        try {
            if (!AdminConfig.isSuperAdminInitialized()) {
                val result = AdminConfig.initializeSuperAdmin()
                if (result.isSuccess) {
                    Log.d("HomeViewModel", "Super admin initialized in Firebase")
                } else {
                    Log.e("HomeViewModel", "Failed to initialize super admin: ${result.exceptionOrNull()?.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error initializing super admin", e)
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
        if (commentText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Cập nhật UI sang trạng thái "Đang gửi"
            _uiState.update { it.copy(commentPostState = CommentPostState.POSTING) }
            try {
                postRepository.addComment(postId, commentText)
                // 2. Cập nhật UI sang trạng thái "Thành công"
                _uiState.update { it.copy(commentPostState = CommentPostState.SUCCESS) }
                // 3. Reset lại trạng thái sau một khoảng thời gian ngắn
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
            _uiState.update { it.copy(isReporting = true) }
            try {
                val success = postRepository.reportPost(reportingPostId, reason, description)
                if (success) {
                    _uiState.update {
                        it.copy(
                            showReportDialog = false,
                            isReporting = false,
                            reportingPostId = null,
                            reportReason = "",
                            reportDescription = ""
                        )
                    }
                    Log.d("HomeViewModel", "Report submitted successfully")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error submitting report", e)
                _uiState.update { it.copy(isReporting = false) }
            }
        }
    }

    fun onDismissReportDialog() {
        _uiState.update {
            it.copy(
                showReportDialog = false,
                reportingPostId = null,
                reportReason = "",
                reportDescription = ""
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

    // 🔸 INFINITE SCROLL - Load more posts with proper pagination
    fun onLoadMore() {
        val currentState = _uiState.value
        val pagination = currentState.paginationState

        // Kiểm tra các điều kiện
        if (pagination.isLoadingMore) {
            Log.d("HomeViewModel", "Already loading more posts")
            return
        }

        if (!pagination.hasMore) {
            Log.d("HomeViewModel", "No more posts to load")
            return
        }

        val categoryId = currentState.selectedCategory?.id ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    paginationState = it.paginationState.copy(isLoadingMore = true)
                )
            }

            try {
                // 🔸 Gọi API với pagination (page-based)
                val newPosts = postRepository.getPostsByPage(
                    categoryId = categoryId,
                    page = pagination.currentPage,
                    pageSize = pagination.pageSize
                )

                if (newPosts.isNotEmpty()) {
                    // Thêm posts mới vào cuối danh sách (lọc duplicate bằng distinctBy)
                    val allPosts = (currentState.posts + newPosts).distinctBy { it.id }
                    val hasMorePages = newPosts.size >= pagination.pageSize

                    _uiState.update {
                        it.copy(
                            posts = allPosts,
                            paginationState = it.paginationState.copy(
                                currentPage = pagination.currentPage + 1,
                                hasMore = hasMorePages,
                                isLoadingMore = false
                            )
                        )
                    }
                    Log.d("HomeViewModel", "Loaded page ${pagination.currentPage} with ${newPosts.size} posts")
                } else {
                    _uiState.update {
                        it.copy(
                            paginationState = it.paginationState.copy(
                                hasMore = false,
                                isLoadingMore = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading more posts", e)
                _uiState.update {
                    it.copy(
                        paginationState = it.paginationState.copy(isLoadingMore = false)
                    )
                }
            }
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