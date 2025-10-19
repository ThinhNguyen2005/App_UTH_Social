package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Trạng thái của trang home
data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val categories: List<String> = filterCategories,
    val selectedCategory: String = filterCategories.first(),
    val shareContent: String? = null // <-- THÊM: Nội dung cần chia sẻ
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
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentCategory = _uiState.value.selectedCategory
                var query: Query = db.collection("posts")

                when (currentCategory) {
                    "Mới nhất", "Tất cả" -> {
                        query = query.orderBy("timestamp", Query.Direction.DESCENDING)
                    }
                    else -> {
                        query = query
                            .whereEqualTo("category", currentCategory)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                    }
                }

                val snapshot = query.get().await()
                val currentUserId = auth.currentUser?.uid

                val posts = snapshot.documents.mapNotNull { document ->
                    val post = document.toObject(Post::class.java)
                    post?.copy(
                        id = document.id,
                        isLiked = post.likedBy.contains(currentUserId)
                    )
                }
                _uiState.value = _uiState.value.copy(posts = posts, isLoading = false, error = null)

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching posts", e)
                _uiState.value = _uiState.value.copy(error = e.localizedMessage, isLoading = false)
            }
        }
    }

    // --- LOGIC XỬ LÝ CÁC HÀNH ĐỘNG ---

    fun onLikeClicked(postId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        Log.d("HomeViewModel", "Like clicked for post: $postId")

        viewModelScope.launch {
            val postRef = db.collection("posts").document(postId)
            val originalPosts = _uiState.value.posts
            val postToUpdate = originalPosts.find { it.id == postId } ?: return@launch
            val isCurrentlyLiked = postToUpdate.isLiked

            val updatedPost = postToUpdate.copy(
                isLiked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) postToUpdate.likes - 1 else postToUpdate.likes + 1
            )
            val updatedPosts = originalPosts.map { if (it.id == postId) updatedPost else it }
            _uiState.value = _uiState.value.copy(posts = updatedPosts)

            try {
                if (isCurrentlyLiked) {
                    postRef.update(
                        "likes", FieldValue.increment(-1),
                        "likedBy", FieldValue.arrayRemove(currentUserId)
                    ).await()
                } else {
                    postRef.update(
                        "likes", FieldValue.increment(1),
                        "likedBy", FieldValue.arrayUnion(currentUserId)
                    ).await()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(posts = originalPosts)
                Log.e("HomeViewModel", "Error updating like status", e)
            }
        }
    }

    fun onCommentClicked(postId: String) {
        Log.d("HomeViewModel", "Comment clicked for post: $postId")
    }

    fun onSaveClicked(postId: String) {
        Log.d("HomeViewModel", "Save clicked for post: $postId")
    }

    // SỬA LẠI: Cập nhật state để kích hoạt chia sẻ
    fun onShareClicked(postId: String) {
        val shareableContent = "Xem bài viết này trên UTH Socials: https://uthsocials.example.com/post/$postId"
        _uiState.value = _uiState.value.copy(shareContent = shareableContent)
    }

    // THÊM: Hàm để reset trạng thái sau khi đã chia sẻ xong
    fun onShareDialogLaunched() {
        _uiState.value = _uiState.value.copy(shareContent = null)
    }

    fun onUserProfileClicked(userId: String) {
        Log.d("HomeViewModel", "User profile clicked for user: $userId")
    }

    fun onCategorySelected(category: String) {
        Log.d("HomeViewModel", "Đã click vào danh mục: $category")

        if (_uiState.value.selectedCategory != category) {
            _uiState.value = _uiState.value.copy(selectedCategory = category)
            fetchPosts()
        }
    }
}

