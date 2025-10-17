package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Post
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// Trạng thái của trang home
data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val categories: List<String> = filterCategories,
    val selectedCategory: String = filterCategories.first()
)

val filterCategories = listOf("Mới nhất", "Tất cả", "Thông báo sinh viên", "Sự kiện", "Học tập")

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Khởi tạo Firebase Firestore
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        // Lấy danh sách bài đăng khi ViewModel được tạo
        fetchPosts()
    }

    // --- LOGIC LẤY DỮ LIỆU TỪ FIRESTORE ---
    private fun fetchPosts() {
        viewModelScope.launch {
            // Cập nhật trạng thái đang tải, nhưng giữ lại danh sách bài post cũ để UI không bị trống
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val currentCategory = _uiState.value.selectedCategory
                var query: Query = db.collection("posts")

                // Xây dựng query dựa trên category được chọn
                when (currentCategory) {
                    "Mới nhất" -> {
                        query = query.orderBy("timestamp", Query.Direction.DESCENDING)
                    }
                    "Tất cả" -> {
                        // Không cần thêm filter, chỉ sắp xếp
                        query = query.orderBy("timestamp", Query.Direction.DESCENDING)
                    }
                    else -> {
                        // Lọc theo các category khác và sắp xếp
                        query = query
                            .whereEqualTo("category", currentCategory)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                    }
                }

                // Thực thi query và chờ kết quả
                val snapshot = query.get().await()

                // Chuyển đổi DocumentSnapshot thành danh sách các đối tượng Post
                val posts = snapshot.documents.mapNotNull { document ->
                    // toObject sẽ tự động map các trường trong Firestore vào data class
                    // quan trọng: tên các trường phải khớp nhau
                    val post = document.toObject(Post::class.java)
                    // Gán ID của document vào đối tượng Post và kiểm tra user đã like chưa
                    post?.copy(
                        id = document.id,
                        isLiked = post.likedBy.contains(auth.currentUser?.uid)
                    )
                }
                _uiState.value = _uiState.value.copy(posts = posts, isLoading = false)

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching posts", e)
                _uiState.value = _uiState.value.copy(error = e.localizedMessage, isLoading = false)
            }
        }
    }

    // --- LOGIC XỬ LÝ CÁC HÀNH ĐỘNG ---

    fun onLikeClicked(postId: String) {
        val currentUserId = auth.currentUser?.uid ?: return // Cần user đăng nhập để like

        viewModelScope.launch {
            val postRef = db.collection("posts").document(postId)

            // Cập nhật giao diện ngay lập tức (Optimistic Update)
            val originalPosts = _uiState.value.posts
            val postToUpdate = originalPosts.find { it.id == postId } ?: return@launch
            val isCurrentlyLiked = postToUpdate.isLiked

            val updatedPost = postToUpdate.copy(
                isLiked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) postToUpdate.likes - 1 else postToUpdate.likes + 1
            )
            val updatedPosts = originalPosts.map { if (it.id == postId) updatedPost else it }
            _uiState.value = _uiState.value.copy(posts = updatedPosts)

            // Cập nhật lên Firestore ở background
            try {
                if (isCurrentlyLiked) {
                    // Nếu đang like -> bỏ like
                    postRef.update(
                        "likes", FieldValue.increment(-1),
                        "likedBy", FieldValue.arrayRemove(currentUserId)
                    ).await()
                } else {
                    // Nếu chưa like -> like
                    postRef.update(
                        "likes", FieldValue.increment(1),
                        "likedBy", FieldValue.arrayUnion(currentUserId)
                    ).await()
                }
            } catch (e: Exception) {
                // Nếu có lỗi, khôi phục lại trạng thái ban đầu
                _uiState.value = _uiState.value.copy(posts = originalPosts)
                Log.e("HomeViewModel", "Error updating like status", e)
            }
        }
    }

    fun onCommentClicked(postId: String) {
        // TODO: Điều hướng đến màn hình chi tiết bài viết hoặc mở bottom sheet bình luận
        // Ví dụ: _navigationEvent.tryEmit(NavigationEvent.ToComments(postId))
        Log.d("HomeViewModel", "Comment clicked for post: $postId")
    }

    fun onSaveClicked(postId: String) {
        // TODO: Xử lý logic lưu bài viết vào danh sách "đã lưu" của người dùng
        // Có thể tạo một collection `users` -> document `userId` -> collection `saved_posts`
        Log.d("HomeViewModel", "Save clicked for post: $postId")
    }

    fun onShareClicked(postId: String) {
        // TODO: Kích hoạt Intent.ACTION_SEND của hệ thống để chia sẻ link bài viết
        Log.d("HomeViewModel", "Share clicked for post: $postId")
    }

    fun onUserProfileClicked(userId: String) {
        // TODO: Điều hướng đến trang cá nhân của người dùng với userId
        // Ví dụ: _navigationEvent.tryEmit(NavigationEvent.ToUserProfile(userId))
        Log.d("HomeViewModel", "User profile clicked for user: $userId")
    }

    fun onCategorySelected(category: String) {
        // Cập nhật category đang được chọn trong state
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        // Gọi lại fetchPosts để tải dữ liệu mới tương ứng với category đã chọn.
        fetchPosts()
        Log.d("HomeViewModel", "Selected category: $category")
    }
}