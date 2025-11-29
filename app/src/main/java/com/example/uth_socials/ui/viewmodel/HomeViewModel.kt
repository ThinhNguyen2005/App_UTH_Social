package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.AdminRepository
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.example.uth_socials.data.util.SecurityValidator
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.util.SecurityValidator.clearCache


//Enum để quản lý trạng thái gửi bình luận
enum class CommentPostState { IDLE, POSTING, SUCCESS, ERROR }

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,
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
    val reportErrorMessage: String? = null,
    val dialogType: DialogType = DialogType.None,
    val isProcessing: Boolean = false,
    val currentUserId: String? = null,
    val hiddenPostIds: Set<String> = emptySet(),
    // 🔸 Blocked users - để filter posts
    val blockedUserIds: Set<String> = emptySet(),
    // 🔸 Admin state
    val isCurrentUserAdmin: Boolean = false,
    val currentUserRole: String? = null,
    val adminStatusMap: Map<String, Boolean> = emptyMap(),
    // 🔸 Generic confirmation dialog
    val showGenericDialog: Boolean = false,
    val genericDialogAction: (() -> Unit)? = null,
    // 🔸 Ban status
    val isUserBanned: Boolean = false,
    val showBanDialog: Boolean = false,
    // 🔸 Edit post dialog
    val showEditPostDialog: Boolean = false,
    val editingPostId: String? = null,
    val editingPostContent: String = "",
    val isSavingPost: Boolean = false,
    val editPostErrorMessage: String? = null,

    // 🔸 Pagination & New Posts
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val hasNewPosts: Boolean = false
)

class HomeViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var commentsJob: Job? = null
    private var categoriesJob: Job? = null
    private val savingPosts = mutableSetOf<String>()
    private val auth = FirebaseAuth.getInstance()
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            Log.d("HomeViewModel", "Auth state changed: User IN (${user.uid}). Loading data.")
            loadDataForUser(user.uid)
        } else {
            Log.d("HomeViewModel", "Auth state changed: User OUT. Clearing data.")
            clearDataOnLogout()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("HomeViewModel", "Init: User already logged in. Loading data.")
            loadDataForUser(currentUser.uid)
        } else {
            Log.d("HomeViewModel", "Init: No user. Loading public data only.")
            loadCategoriesAndInitialPosts()
        }
    }

    private fun clearDataOnLogout() {
        commentsJob?.cancel()
        postsJob?.cancel()

        _uiState.update {
            it.copy(
                currentUserId = null,
                isCurrentUserAdmin = false,
                currentUserRole = null,
                isUserBanned = false,
                hiddenPostIds = emptySet(),
                blockedUserIds = emptySet(),
                posts = it.posts.map { post ->
                    post.copy(isLiked = false, isSaved = false)
                },
                isLoadingMore = false,
                canLoadMore = true,
                hasNewPosts = false
            )
        }
        loadCategoriesAndInitialPosts()
    }

    private fun loadDataForUser(userId: String) {
        _uiState.update { it.copy(currentUserId = userId, isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            loadCategoriesAndInitialPosts()
            loadHiddenPosts()
            loadBlockedUsers()
            loadBanStatus()
            checkAccout()
        }
    }

    private fun loadBanStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch
            try {
                val user = userRepository.getUser(currentUser.uid)
                _uiState.update {
                    it.copy(isUserBanned = user?.isBanned ?: false)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading ban status", e)
            }
        }
    }

    private fun loadCategoriesAndInitialPosts() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            listenToCategoriesChanges()
            listenToPostChanges("all") // Sử dụng "all" làm mặc định
        }
    }

    private fun listenToCategoriesChanges() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.getCategoriesFlow().collect { categories ->
                if (categories.isEmpty()) {
                    categoriesIfNeeded()
                } else {
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


    private suspend fun categoriesIfNeeded() {
        try {
            withContext(Dispatchers.IO) {
                val existingCategories = categoryRepository.getCategories()
                if (existingCategories.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            categories = existingCategories,
                            selectedCategory = existingCategories.firstOrNull(),
                            isLoading = false,
                        )
                    }
                } else {
                    categoryRepository.defaultCategories()
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

    private fun loadBlockedUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val blockedIds = userRepository.getBlockedUsers(currentUserId)
                _uiState.update { it.copy(blockedUserIds = blockedIds.toSet()) }
                Log.d("HomeViewModel", "Loaded ${blockedIds.size} blocked users")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading blocked users", e)
            }
        }
    }

    fun refreshBlockedUsers() {
        loadBlockedUsers()
    }


    private fun checkAccout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val (isAdmin, isSuperAdmin) = SecurityValidator.getCachedAdminStatus(
                    currentUserId
                )

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

                Log.d(
                    "HomeViewModel",
                    "Admin status refreshed: isAdmin=${isAdmin || isSuperAdmin}, role=$role"
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing admin status", e)
                _uiState.update {
                    it.copy(
                        isCurrentUserAdmin = false,
                        currentUserRole = null
                    )
                }
            }
        }
    }

    private var postsJob: Job? = null

    private fun listenToPostChanges(categoryId: String) {
        postsJob?.cancel()
        postsJob = viewModelScope.launch(Dispatchers.IO) {
            // Reset pagination state when category changes
            _uiState.update {
                it.copy(
                    canLoadMore = true,
                    isLoadingMore = false,
                    hasNewPosts = false
                )
            }

            postRepository.getPostsFlow(categoryId).collect { newPosts ->
                val sortedNewPosts = newPosts.sortedByDescending { it.timestamp?.seconds ?: 0L }

                _uiState.update { currentState ->
                    val currentPosts = currentState.posts

                    // Logic Merge:
                    // 1. Nếu list hiện tại rỗng -> Đây là lần load đầu tiên -> Thay thế toàn bộ
                    if (currentPosts.isEmpty()) {
                        currentState.copy(
                            posts = sortedNewPosts,
                            isLoading = false
                        )
                    } else {
                        // 2. Nếu list không rỗng -> Đây là update real-time
                        // Kiểm tra xem có bài viết mới thực sự không (so sánh ID bài đầu tiên)
                        val firstCurrentPost = currentPosts.firstOrNull()
                        val firstNewPost = sortedNewPosts.firstOrNull()

                        val hasNew = if (firstCurrentPost != null && firstNewPost != null) {
                            // Nếu ID khác nhau VÀ timestamp của bài mới lớn hơn bài cũ -> Có bài mới
                            firstNewPost.id != firstCurrentPost.id &&
                                    (firstNewPost.timestamp?.seconds
                                        ?: 0) > (firstCurrentPost.timestamp?.seconds ?: 0)
                        } else {
                            false
                        }

                        // Merge logic:
                        // - Lấy 20 bài mới nhất từ real-time (sortedNewPosts)
                        // - Lấy các bài cũ từ danh sách hiện tại (trừ những bài đã có trong 20 bài mới)
                        // - Kết hợp lại
                        val newPostIds = sortedNewPosts.map { it.id }.toSet()
                        val olderPosts = currentPosts.filter { !newPostIds.contains(it.id) }
                        val mergedPosts = sortedNewPosts + olderPosts

                        currentState.copy(
                            posts = mergedPosts,
                            isLoading = false,
                            hasNewPosts = currentState.hasNewPosts || hasNew // Giữ trạng thái true nếu đã có bài mới trước đó
                        )
                    }
                }

                loadAdminStatusForPosts(sortedNewPosts)
            }
        }
    }

    fun loadMorePosts() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.canLoadMore) return

        val categoryId = currentState.selectedCategory?.id ?: "all"
        val lastPost = currentState.posts.lastOrNull()

        if (lastPost == null) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingMore = true) }

            try {
                val olderPosts = postRepository.loadMorePosts(
                    categoryId = categoryId,
                    lastTimestamp = lastPost.timestamp
                )

                if (olderPosts.isEmpty()) {
                    _uiState.update { it.copy(isLoadingMore = false, canLoadMore = false) }
                } else {
                    _uiState.update { state ->
                        // Filter duplicates just in case
                        val currentIds = state.posts.map { it.id }.toSet()
                        val uniqueOlderPosts = olderPosts.filter { !currentIds.contains(it.id) }

                        state.copy(
                            posts = state.posts + uniqueOlderPosts,
                            isLoadingMore = false,
                            canLoadMore = uniqueOlderPosts.isNotEmpty() // Nếu load về ít hơn limit hoặc rỗng thì hết
                        )
                    }
                    loadAdminStatusForPosts(olderPosts)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading more posts", e)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun clearNewPostsFlag() {
        _uiState.update { it.copy(hasNewPosts = false) }
    }

    private fun loadAdminStatusForPosts(posts: List<Post>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Extract unique user IDs from posts
                val uniqueUserIds = posts.map { it.userId }.distinct()

                // Get current admin status map to avoid reloading
                val currentAdminMap = _uiState.value.adminStatusMap

                // Find user IDs that need to be checked (not in cache)
                val userIdsToCheck = uniqueUserIds.filter { userId ->
                    userId.isNotBlank() && !currentAdminMap.containsKey(userId)
                }

                if (userIdsToCheck.isEmpty()) {
                    return@launch // All admin statuses already cached
                }

                // Batch load admin status for all users
                val newAdminMap = mutableMapOf<String, Boolean>()
                userIdsToCheck.forEach { userId ->
                    try {
                        val (isAdmin, _) = SecurityValidator.getCachedAdminStatus(userId)
                        newAdminMap[userId] = isAdmin
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error loading admin status for $userId", e)
                        newAdminMap[userId] = false
                    }
                }

                // Merge with existing map
                val updatedAdminMap = currentAdminMap + newAdminMap

                _uiState.update {
                    it.copy(adminStatusMap = updatedAdminMap)
                }

                Log.d("HomeViewModel", "Loaded admin status for ${newAdminMap.size} users")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading admin status for posts", e)
            }
        }
    }

    fun onCategorySelected(category: Category) {
        if (_uiState.value.selectedCategory?.id != category.id) {
            // Reset posts list when changing category
            _uiState.update {
                it.copy(
                    selectedCategory = category,
                    isLoading = true,
                    posts = emptyList() // Clear old posts from previous category
                )
            }
            listenToPostChanges(category.id)
        }
    }

    // --- LOGIC XỬ LÝ CÁC HÀNH ĐỘNG ---

    fun onLikeClicked(postId: String) {
        // Check ban status trước khi like
        if (_uiState.value.isUserBanned) {
            _uiState.update { it.copy(showBanDialog = true) }
            return
        }

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
                Log.d("HomeViewModel", "Đã nhấn like bài viết: $postId")
                postRepository.toggleLikeStatus(postId, isCurrentlyLiked)
            } catch (e: Exception) {
                // Nếu có lỗi, khôi phục lại trạng thái UI ban đầu
                _uiState.update {
                    it.copy(
                        posts = originalPosts,
                        error = "Lỗi không thể like bài viết. Vui lòng thử lại."
                    )
                }
                Log.e("HomeViewModel", "Error updating like status", e)
            }
        }
    }

    fun onCommentClicked(postId: String) {
        // Clear error trước khi thực hiện action mới
        clearError()

        // Check ban status trước khi mở comment sheet
        if (_uiState.value.isUserBanned) {
            _uiState.update { it.copy(showBanDialog = true) }
            return
        }

        commentsJob?.cancel()

        // Cập nhật state để hiển thị sheet và trạng thái loading
        _uiState.update {
            it.copy(
                commentSheetPostId = postId,
                isSheetLoading = true,
                commentsForSheet = emptyList(),

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
        Log.d(
            "HomeViewModel",
            "addComment called with postId: $postId, commentText: '$commentText'"
        )
        if (commentText.isBlank()) {
            Log.w("HomeViewModel", "Comment text is blank, returning early")
            return
        }

        // Check ban status trước khi thêm comment
        if (_uiState.value.isUserBanned) {
            _uiState.update { it.copy(showBanDialog = true) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    commentPostState = CommentPostState.POSTING,
                    commentErrorMessage = null // update đang gửi
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

    fun addCommentReply(
        postId: String,
        commentParentname: String,
        commentParentId: String,
        commentText: String
    ) {
        Log.d(
            "HomeViewModel",
            "addComment called with postId: $postId, commentText: '$commentText'"
        )
        if (commentText.isBlank()) {
            Log.w("HomeViewModel", "Comment text is blank, returning early")
            return
        }

        // Check ban status trước khi thêm comment
        if (_uiState.value.isUserBanned) {
            _uiState.update { it.copy(showBanDialog = true) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    commentPostState = CommentPostState.POSTING,
                    commentErrorMessage = null // update đang gửi
                )
            }

            val result = postRepository.addCommentReply(
                postId,
                commentParentname,
                commentParentId,
                commentText
            )
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
        // Clear error trước khi thực hiện action mới
        clearError()

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
            val updatedComments =
                originalComments.map { if (it.id == commentId) updatedComment else it }
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
        // Clear error trước khi thực hiện action mới
        clearError()

        // Check ban status trước khi save
        if (_uiState.value.isUserBanned) {
            _uiState.update { it.copy(showBanDialog = true) }
            return
        }

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
                _uiState.update {
                    it.copy(
                        posts = originalPosts,
                        isLoading = false,
                        error = "Lỗi không thể lưu bài viết. Vui lòng thử lại."
                    )
                }
                Log.e("HomeViewModel", "Error toggling save status", e)
            } finally {
                // 4. Xóa khỏi set sau khi hoàn thành
                savingPosts.remove(postId)
            }
        }
    }

    fun onShareClicked(postId: String) {
        val shareableContent =
            "Xem bài viết này trên UTH Socials: htpps:://uthsocials://post/$postId"
        _uiState.update { it.copy(shareContent = shareableContent) }
    }

    fun onShareDialogLaunched() {
        _uiState.update { it.copy(shareContent = null) }
    }

    // --- 🔸 HÀM XỬ LÝ ẨN BÀI VIẾT ---
    fun onHideClicked(postId: String) {
        if (_uiState.value.isUserBanned) {
            _uiState.update { it.copy(showBanDialog = true) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = postRepository.hidePost(postId)
                if (success) {
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

    fun onReportClicked(postId: String) {
        if (_uiState.value.isUserBanned) {
            _uiState.update { it.copy(showBanDialog = true) }
            return
        }

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

    fun onReportReasonChanged(reason: String) {
        _uiState.update { it.copy(reportReason = reason) }
    }

    fun onReportDescriptionChanged(description: String) {
        _uiState.update { it.copy(reportDescription = description) }
    }

    // Báo cáo bài viết
    fun onSubmitReport() {
        val reportingPostId = _uiState.value.reportingPostId ?: return
        val reason = _uiState.value.reportReason.ifEmpty { return }
        val description = _uiState.value.reportDescription

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isReporting = true, reportErrorMessage = null) }

            try {
                val post = _uiState.value.posts.find { it.id == reportingPostId }
                if (post != null) {
                    val (isAdmin, _) = SecurityValidator.getCachedAdminStatus(post.userId)
                    if (isAdmin) {
                        throw IllegalArgumentException("Không thể báo cáo admin")
                    }
                }
                val success = postRepository.reportPost(reportingPostId, reason, description)
                if (success) {
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
                    Log.d("HomeModel", "Report submitted successfully")
                } else {
                    val errorMsg = "Gửi báo cáo thất bại. Vui lòng thử lại."
                    _uiState.update {
                        it.copy(
                            isReporting = false,
                            reportErrorMessage = errorMsg
                        )
                    }
                    Log.w("HomeModel", "Report submission failed: $errorMsg")
                }
            } catch (e: IllegalArgumentException) {
                val errorMsg = e.message ?: "Lỗi không xác định"
                _uiState.update {
                    it.copy(
                        isReporting = false,
                        reportErrorMessage = errorMsg
                    )
                }
                Log.w("HomeModel", "Report blocked: $errorMsg")
            } catch (e: Exception) {
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

    // DIALOG xóa bài viết

    private suspend fun canDeletePost(postUserId: String, currentUserId: String?): Boolean {
        return SecurityValidator.canDeletePost(currentUserId, postUserId)
    }

    fun onDeleteClicked(postId: String) {
        val post = _uiState.value.posts.find { it.id == postId } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (canDeletePost(post.userId, _uiState.value.currentUserId)) {
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.DeletePost(postId)
                    )
                }
            }
        }

    }

    fun onConfirmDialog() {
        when (val dialog = _uiState.value.dialogType) {
            is DialogType.DeletePost -> onConfirmDelete(dialog.postId)
            is DialogType.BlockUser -> {
                //Không dùng
            }

            is DialogType.UnblockUser -> {
                //Không dùng
            }

            is DialogType.None -> return
        }
    }

    private fun onConfirmDelete(postIdToDelete: String) {
        clearError()
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val post = _uiState.value.posts.find { it.id == postIdToDelete }
                var postOwnerId = post?.userId

                if (postOwnerId.isNullOrBlank()) {
                    val postFromDb = adminRepository.getPostById(postIdToDelete)
                    postOwnerId = postFromDb?.userId
                }
                val isAdmin = _uiState.value.isCurrentUserAdmin
                val currentUserId = _uiState.value.currentUserId

                val success = postRepository.deletePost(postIdToDelete)
                if (success) {
                    if (isAdmin && !postOwnerId.isNullOrBlank() && postOwnerId != currentUserId) {

                        val result = adminRepository.autoBanUser(postOwnerId)
                        result.onSuccess {
                            Log.d(
                                "HomeViewModel",
                                "Tự động ban user khi xóa bài viết > 3: $postOwnerId"
                            )
                        }.onFailure { e ->
                            Log.e(
                                "HomeViewModel",
                                "Không thể tự động ban user: $postOwnerId",
                                e
                            )
                            Log.e("HomeViewModel", "Exception details: ${e.message}", e)
                        }
                    }

                    val updatedPosts = _uiState.value.posts.filter { it.id != postIdToDelete }
                    _uiState.update {
                        it.copy(
                            posts = updatedPosts,
                            dialogType = DialogType.None,
                            isProcessing = false,
                            successMessage = "Bài viết đã được xóa thành công."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            dialogType = DialogType.None,
                            isProcessing = false,
                            error = "Lỗi không thể xóa bài viết. Vui lòng thử lại."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", " Error deleting post: $postIdToDelete")
                Log.e("HomeViewModel", "Exception details: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.None,
                        isProcessing = false,
                        error = "Lỗi không thể xóa bài viết. Vui lòng thử lại."
                    )
                }
            }
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

    fun onRetry() {
        _uiState.update { it.copy(error = null, isLoading = true) }
        listenToCategoriesChanges()
    }

    fun onDismissBanDialog() {
        _uiState.update { it.copy(showBanDialog = false) }
    }

    fun onEditPostClicked(postId: String) {
        val post = _uiState.value.posts.find { it.id == postId }
        if (post != null) {
            Log.d("HomeViewModel", "onEditPostClicked: $postId")
            _uiState.update {
                it.copy(
                    showEditPostDialog = true,
                    editingPostId = postId,
                    editingPostContent = post.textContent,
                    editPostErrorMessage = null
                )
            }
        }
    }

    fun onUpdatePostContent(newContent: String) {
        _uiState.update { it.copy(editingPostContent = newContent) }
    }

    fun onSaveEditedPost() {
        val postId = _uiState.value.editingPostId ?: return
        val newContent = _uiState.value.editingPostContent.trim()

        if (newContent.isEmpty()) {
            Log.w("HomeViewModel", "Empty post content")
            _uiState.update {
                it.copy(editPostErrorMessage = "Nội dung không được để trống")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            Log.d("HomeViewModel", "onSaveEditedPost: $postId, $newContent")

            _uiState.update {
                it.copy(isSavingPost = true, editPostErrorMessage = null)
            }

            try {
                val result = postRepository.updatePostContent(postId, newContent)
                result.onSuccess {
                    Log.d("HomeViewModel", "Post updated successfully: $postId")
                    // Update local state
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            Log.d("HomeViewModel", "Updating post content: $postId")
                            post.copy(textContent = newContent)
                        } else {
                            Log.d("HomeViewModel", "Keeping post unchanged: ${post.id}")
                            post
                        }
                    }
                    _uiState.update {
                        Log.d("HomeViewModel", "Updated posts: $updatedPosts")
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
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSavingPost = false,
                            editPostErrorMessage = "Lỗi khi lưu: ${e.message ?: "Vui lòng thử lại"}",
                            error = "Không thể cập nhật được bài viết! Vui lòng thử lại"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingPost = false,
                        editPostErrorMessage = "Lỗi khi lưu: ${e.message ?: "Vui lòng thử lại"}"
                    )
                }
            }
        }
    }

    fun onDismissEditDialog() {
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

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        categoriesJob?.cancel()
        commentsJob?.cancel()
        postsJob?.cancel()
        Log.d("HomeViewModel", "onCleared: Listeners and jobs cancelled.")
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun updateBanStatus(isBanned: Boolean) {
        _uiState.update { it.copy(isUserBanned = isBanned) }
        Log.d("HomeViewModel", "Ban status updated: $isBanned")
    }

    fun cleanupOnLogout() {
        Log.d("HomeViewModel", "Cleaning up listeners on logout")

        // Cancel tất cả jobs
        categoriesJob?.cancel()
        commentsJob?.cancel()
        postsJob?.cancel()

        // Reset jobs
        categoriesJob = null
        commentsJob = null
        postsJob = null
        clearCache()

        postRepository.clearCache()

        // Reset state
        _uiState.update { HomeUiState() }

        Log.d("HomeViewModel", "Cleanup completed")
    }
}


