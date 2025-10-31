package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.UserRepository
import kotlinx.coroutines.async

data class ProfileUiState(
    val posts: List<Post> = emptyList(),
    val isOwner: Boolean = false,
    val username: String = "",
    val userAvatarUrl: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val bio: String = "",
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val postCount: Int = 0
)

class ProfileViewModel(
    private val userId: String,
    // ✅ Sử dụng 2 repository đã được phân tách
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // Chạy song song để tải nhanh hơn
                val userDeferred = async { userRepository.getUser(userId) }
                val postsDeferred = async { postRepository.getPostsForUser(userId) }

                val user = userDeferred.await()
                val posts = postsDeferred.await()
                val currentUserId = userRepository.getCurrentUserId()

                if (user != null) {
                    _uiState.update {
                        it.copy(
                            username = user.username,
                            userAvatarUrl = user.avatarUrl,
                            followers = user.followers.size,
                            following = user.following.size,
                            bio = user.bio,
                            isOwner = currentUserId == userId,
                            isFollowing = user.followers.contains(currentUserId),
                            posts = posts,
                            postCount = posts.size,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy người dùng.") }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile data", e)
                _uiState.update { it.copy(isLoading = false, error = "Lỗi tải dữ liệu.") }
            }
        }
    }

    fun onFollowClicked() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val isCurrentlyFollowing = _uiState.value.isFollowing
            val success = userRepository.toggleFollow(currentUserId, userId, isCurrentlyFollowing)
            if (success) {
                _uiState.update {
                    it.copy(
                        isFollowing = !isCurrentlyFollowing,
                        followers = if (isCurrentlyFollowing) it.followers - 1 else it.followers + 1
                    )
                }
            }
        }
    }

    fun onBlockUser() {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            userRepository.blockUser(currentUserId, userId)
            // Cân nhắc reload lại trang hoặc điều hướng đi
        }
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch {
            val success = postRepository.deletePost(postId)
            if (success) {
                _uiState.update {
                    it.copy(posts = it.posts.filterNot { post -> post.id == postId })
                }
            }
        }
    }
}
