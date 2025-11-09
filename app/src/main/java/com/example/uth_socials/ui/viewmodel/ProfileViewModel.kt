package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.UserRepository

import kotlinx.coroutines.Dispatchers

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
    val postCount: Int = 0,
    val currentUserId: String? = null,
    val profileUserId: String = "",
)

class ProfileViewModel(
    private val userId: String,
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

        viewModelScope.launch (Dispatchers.IO){

            try {
                // Chạy song song để tải nhanh hơn
                val userDeferred = async { userRepository.getUser(userId) }
                val postsDeferred = async { postRepository.getPostsForUser(userId) }

                val user = userDeferred.await()
                val posts = postsDeferred.await()
                val currentUserId = userRepository.getCurrentUserId()

                if (user != null) {
                    val isOwner = currentUserId == userId
                    val isFollowing = currentUserId?.let(user.followers::contains) == true
                    _uiState.update {
                        it.copy(
                            profileUserId = userId,
                            username = user.username,
                            userAvatarUrl = user.avatarUrl,
                            followers = user.followers.size,
                            following = user.following.size,
                            bio = user.bio,
                            currentUserId = currentUserId,
                            isOwner = isOwner,
                            isFollowing = isFollowing,
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
        val state = _uiState.value
        if (state.isOwner) return
        val resolvedCurrentUserId = state.currentUserId ?: userRepository.getCurrentUserId() ?: return
        if (state.currentUserId == null) {
            _uiState.update { it.copy(currentUserId = resolvedCurrentUserId) }
        }

        viewModelScope.launch (Dispatchers.IO){

            val isCurrentlyFollowing = _uiState.value.isFollowing
            val success = userRepository.toggleFollow(resolvedCurrentUserId, userId, isCurrentlyFollowing)
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
        val state = _uiState.value
        if (state.isOwner) return
        val resolvedCurrentUserId = state.currentUserId ?: userRepository.getCurrentUserId() ?: return
        if (state.currentUserId == null) {
            _uiState.update { it.copy(currentUserId = resolvedCurrentUserId) }
        }

        viewModelScope.launch (Dispatchers.IO){

            userRepository.blockUser(resolvedCurrentUserId, userId)
            // Cân nhắc reload lại trang hoặc điều hướng đi
        }
    }

    fun onDeletePost(postId: String) {
        if (!_uiState.value.isOwner) return

        viewModelScope.launch (Dispatchers.IO){
            val success = postRepository.deletePost(postId)
            if (success) {
                _uiState.update {
                    it.copy(posts = it.posts.filterNot { post -> post.id == postId })
                }
            }
        }
    }
    private val chatRepository = ChatRepository()

    fun createChatWithUser(targetUserId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val chatId = chatRepository.getOrCreateChatId(targetUserId)
                if (chatId != null) {
                    onChatReady(chatId)
                } else {
                    Log.e("ProfileViewModel", "Không thể tạo cuộc trò chuyện")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi tạo chat", e)
            }
        }
    }
}
