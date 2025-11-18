package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý danh sách bài viết đã lưu
 *
 * Features:
 * - Load danh sách posts đã save của user hiện tại
 * - Toggle save/unsave status
 * - Toggle like status
 * - Real-time updates khi có thay đổi
 * - Loading states và error handling
 */
class SavedPostsViewModel : ViewModel() {
    private val postRepository = PostRepository()
    private val auth = FirebaseAuth.getInstance()

    // UI States
    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSavedPosts()
    }

    /**
     * Load danh sách bài viết đã lưu
     * Lắng nghe real-time updates từ Firestore
     */
    private fun loadSavedPosts() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            _isLoading.value = false
            _errorMessage.value = "Bạn cần đăng nhập để xem bài viết đã lưu"
            return
        }

        viewModelScope.launch {
            try {
                // Lấy tất cả posts và filter những post mà user đã save
                postRepository.getPostsFlow("all")
                    .map { posts ->
                        // Filter chỉ lấy posts mà user hiện tại đã save
                        posts.filter { post ->
                            post.savedBy.contains(currentUserId)
                        }
                    }
                    .catch { e ->
                        Log.e("SavedPostsVM", "Error loading saved posts", e)
                        _errorMessage.value = "Không thể tải bài viết đã lưu"
                        _isLoading.value = false
                    }
                    .collect { filteredPosts ->
                        _savedPosts.value = filteredPosts
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("SavedPostsVM", "Error setting up saved posts flow", e)
                _errorMessage.value = "Đã xảy ra lỗi"
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle trạng thái save của bài viết
     * Khi unsave, bài viết sẽ tự động biến mất khỏi danh sách
     */
    fun toggleSaveStatus(postId: String, isCurrentlySaved: Boolean) {
        viewModelScope.launch {
            try {
                postRepository.toggleSaveStatus(postId, isCurrentlySaved)
                //Không cần update UI manually vì Flow sẽ tự động update
            } catch (e: Exception) {
                Log.e("SavedPostsVM", "Error toggling save status", e)
                _errorMessage.value = "Không thể thực hiện thao tác này"
            }
        }
    }

    /**
     * Toggle trạng thái like của bài viết
     */
    fun toggleLikeStatus(postId: String, isCurrentlyLiked: Boolean) {
        viewModelScope.launch {
            try {
                postRepository.toggleLikeStatus(postId, isCurrentlyLiked)
            } catch (e: Exception) {
                Log.e("SavedPostsVM", "Error toggling like status", e)
                _errorMessage.value = "Không thể thích bài viết"
            }
        }
    }

    /**
     * Ẩn bài viết khỏi feed
     */
    fun hidePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = postRepository.hidePost(postId)
                if (!success) {
                    _errorMessage.value = "Không thể ẩn bài viết"
                }
            } catch (e: Exception) {
                Log.e("SavedPostsVM", "Error hiding post", e)
                _errorMessage.value = "Không thể ẩn bài viết"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Refresh danh sách
     */
    fun refresh() {
        _isLoading.value = true
        loadSavedPosts()
    }
}