package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//Mẫu khi có dữ liệu thật thì xóa cái này đi
const val LATEST_POSTS_ID = "latest"
const val ALL_POSTS_ID = "all"

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
    // 🔸 Thêm state cho infinite scroll
    val isLoadingMore: Boolean = false
)

class HomeViewModel(private val postRepository: PostRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var commentsJob: Job? = null
    private val savingPosts = mutableSetOf<String>() // ngăn spam

    init {
        loadCurrentUser()
        loadCategoriesAndInitialPosts()
        loadHiddenPosts()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            // --- LOGIC GIẢ ĐỊNH ---
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                _uiState.update { it.copy(currentUserId = currentUser.uid) }
            }

            // Tạm thời, chúng ta sẽ dùng một URL thật để thấy kết quả ngay
            val fakeUserAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=500&auto=format&fit=crop"
            _uiState.update { it.copy(currentUserAvatarUrl = fakeUserAvatarUrl) }
        }
    }

    private fun loadCategoriesAndInitialPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
//Mẫu khi có dữ liệu thật thì xóa cái này đi
            val virtualCategories = listOf(
                Category(id = LATEST_POSTS_ID, name = "Mới nhất", order = -2),
                Category(id = ALL_POSTS_ID, name = "Tất cả", order = -1)
            )

            val realCategories = postRepository.getCategories()
            val allCategories = (virtualCategories + realCategories).sortedBy { it.order }
            val initialCategory = allCategories.firstOrNull()

            _uiState.update { it.copy(categories = allCategories, selectedCategory = initialCategory) }

            initialCategory?.let { listenToPostChanges(it.id) }
        }
    }

    private fun loadHiddenPosts() {
        viewModelScope.launch {
            try {
                val hiddenIds = postRepository.getHiddenPostIds()
                _uiState.update { it.copy(hiddenPostIds = hiddenIds.toSet()) }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading hidden posts", e)
            }
        }
    }

    private fun listenToPostChanges(categoryId: String) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        commentsJob = viewModelScope.launch {
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

        viewModelScope.launch {
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

    //  xử lý like/unlike bình luận với Optimistic Update
    fun onCommentLikeClicked(commentId: String) {
        viewModelScope.launch {
            val originalComments = _uiState.value.commentsForSheet
            val commentToUpdate = originalComments.find { it.id == commentId } ?: return@launch
            val isCurrentlyLiked = commentToUpdate.isLiked

            // 1. Cập nhật UI ngay lập tức
            val updatedComment = commentToUpdate.copy(
                isLiked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) commentToUpdate.likes - 1 else commentToUpdate.likes + 1
            )
            val updatedComments = originalComments.map { if (it.id == commentId) updatedComment else it }
            _uiState.update { it.copy(commentsForSheet = updatedComments) }

            // 2. Gọi Repository để cập nhật server
            try {
                // Giả sử bạn sẽ tạo hàm này trong Repository
                // postRepository.toggleCommentLikeStatus(commentId, isCurrentlyLiked)
                Log.d("HomeViewModel", "Toggled like for comment $commentId")
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


    // ... trong HomeViewModel.kt

    fun onSaveClicked(postId: String) {
        // Nếu đang xử lý thì bỏ qua
        if (savingPosts.contains(postId)) return

        viewModelScope.launch {
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
        val shareableContent = "Xem bài viết này trên UTH Socials: https://uthsocials.example.com/post/$postId"
        _uiState.update { it.copy(shareContent = shareableContent) }
    }

    fun onShareDialogLaunched() {
        _uiState.update { it.copy(shareContent = null) }
    }

    fun onUserProfileClicked(userId: String) {
        Log.d("HomeViewModel", "User profile clicked for user: $userId")
    }

    // --- 🔸 HÀM XỬ LÝ ẨN BÀI VIẾT ---
    fun onHideClicked(postId: String) {
        viewModelScope.launch {
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

        viewModelScope.launch {
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

        viewModelScope.launch {
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

    // 🔸 Infinite scroll - load more posts
    fun onLoadMore() {
        // Chỉ load more nếu hiện tại không đang load
        if (_uiState.value.isLoadingMore || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val currentCategoryId = _uiState.value.selectedCategory?.id ?: return@launch
                val currentPosts = _uiState.value.posts
                
                // Giả sử repository có method để load thêm posts (pagination)
                // Nếu chưa có, bạn có thể implement pagination trong PostRepository
                postRepository.getPostsFlow(currentCategoryId).collect { newPosts ->
                    // Kết hợp posts cũ với posts mới, tránh duplicate
                    val allPosts = (currentPosts + newPosts).distinctBy { it.id }
                    _uiState.update {
                        it.copy(
                            posts = allPosts,
                            isLoadingMore = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading more posts", e)
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    // 🔸 Retry loading when error occurs
    fun onRetry() {
        _uiState.update { it.copy(error = null, isLoading = true) }
        loadCategoriesAndInitialPosts()
    }
}