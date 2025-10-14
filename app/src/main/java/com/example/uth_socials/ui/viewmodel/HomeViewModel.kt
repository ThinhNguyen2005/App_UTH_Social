package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Post
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await // Rất quan trọng!

//Trạng thái của trang home
data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val categories: List<String> = filterCategories, // Thêm danh sách categories
    val selectedCategory: String = filterCategories.first() // Thêm category đang được chọn
)
val filterCategories = listOf("Tất cả", "Thông báo sinh viên", "Mới nhất", "Sự kiện", "Học tập")

class HomeViewModel : ViewModel(){
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchPosts()
    }
    private fun fetchPosts() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                delay(1000L) // Giả lập tải trong 1 giây

                val dummyPosts = listOf(
                    Post(
                        id = "1", userId = "user1", username = "Thomus Jupiter",
                        userAvatarUrl = "https://picsum.photos/seed/user1/200",
                        timestamp = Timestamp.now(),
                        textContent = "Social Networks are still extremely popular, even though there is a lot of competition from Facebook, Instagram, Twitter, and others. It's a great way to connect with friends and family.",
                        imageUrls = listOf("https://picsum.photos/seed/post1/1600/900"),
                        likes = 3, commentCount = 999
                    ),
                    Post(
                        id = "2", userId = "user2", username = "Jane Doe",
                        userAvatarUrl = "https://picsum.photos/seed/user2/200",
                        timestamp = Timestamp.now(),
                        textContent = "Bài viết có 2 ảnh để test layout grid!",
                        imageUrls = listOf(
                            "https://picsum.photos/seed/post2a/800/800",
                            "https://picsum.photos/seed/post2b/800/800"
                        ),
                        likes = 15, commentCount = 20
                    ),
                    Post(
                        id = "3", userId = "user3", username = "John Smith",
                        userAvatarUrl = "https://picsum.photos/seed/user3/200",
                        timestamp = Timestamp.now(),
                        textContent = "Bài viết có 4 ảnh!",
                        imageUrls = listOf(
                            "https://picsum.photos/seed/post3a/800/800",
                            "https://picsum.photos/seed/post3b/800/800",
                            "https://picsum.photos/seed/post3c/800/800",
                            "https://picsum.photos/seed/post3d/800/800"
                        ),
                        likes = 150, commentCount = 45
                    )
                )

                _uiState.value = HomeUiState(posts = dummyPosts, isLoading = false)

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error creating dummy data", e)
                _uiState.value = HomeUiState(error = e.localizedMessage, isLoading = false)
            }
        }
        fun onCategorySelected(category: String) {
            // Cập nhật category đang được chọn trong state
            _uiState.value = _uiState.value.copy(selectedCategory = category)
            // TODO: Sau này, bạn có thể gọi lại fetchPosts() ở đây
            // để tải dữ liệu mới tương ứng với category đã chọn.
            Log.d("HomeViewModel", "Selected category: $category")
        }
    }

    fun onLikeClicked(postId: String) { /* TODO: Xử lý logic like */

    }
    fun onCommentClicked(postId: String) { /* TODO: Điều hướng đến màn hình bình luận */

    }
    fun onSaveClicked(postId: String) { /* TODO: Xử lý logic lưu */

    }
    fun onShareClicked(postId: String) { /* TODO: Xử lý logic chia sẻ */

    }
    fun onUserProfileClicked(userId: String) { /* TODO: Điều hướng đến trang cá nhân */

    }
    fun onCategorySelected(category: String) {
        // Cập nhật category đang được chọn trong state
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        // TODO: Sau này, bạn có thể gọi lại fetchPosts() ở đây
        // để tải dữ liệu mới tương ứng với category đã chọn.
        Log.d("HomeViewModel", "Selected category: $category")
    }
}