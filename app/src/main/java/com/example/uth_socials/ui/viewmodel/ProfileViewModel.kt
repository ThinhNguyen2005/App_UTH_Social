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
    val userAvatarUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val bio: String = "",
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val postCount: Int = 0,
    val currentUserId: String? = null,
    val profileUserId: String = "",
    val isUserBanned: Boolean = false,
    val showBanDialog: Boolean = false,
    val successMessage: String? = null,
    // ✅ Single dialog state thay vì nhiều boolean flags
    val dialogType: DialogType = DialogType.None,
    val isProcessing: Boolean = false,
    // ✅ State để xử lý sau khi block
    val isUserBlocked: Boolean = false,
    val shouldNavigateBack: Boolean = false
    )

class ProfileViewModel(
    private val userId: String,
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {
    private val chatRepository = ChatRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {

        loadData()
    }

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch (Dispatchers.IO){
            try {
                val currentUserId = userRepository.getCurrentUserId()
                
                // ✅ Kiểm tra xem user đã bị block chưa
                val isBlocked = if (currentUserId != null) {
                    userRepository.isUserBlocked(currentUserId, userId)
                } else {
                    false
                }
                
                if (isBlocked) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUserBlocked = true,
                            error = "Bạn đã chặn người dùng này."
                        )
                    }
                    return@launch
                }
                
                // Chạy song song để tải nhanh hơn
                val userDeferred = async { userRepository.getUser(userId) }
                val postsDeferred = async { postRepository.getPostsForUser(userId) }

                val user = userDeferred.await()
                val posts = postsDeferred.await()

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
                            isLoading = false,
                            isUserBlocked = false
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
        
        // ✅ Hiển thị dialog xác nhận
        _uiState.update {
            it.copy(
                dialogType = DialogType.BlockUser(
                    userId = userId,
                    username = state.username
                )
            )
        }
    }
    
    fun onConfirmDialog() {
        when (val dialog = _uiState.value.dialogType) {
            is DialogType.DeletePost -> onConfirmDelete(dialog.postId)
            is DialogType.BlockUser -> onConfirmBlock(dialog.userId)
            is DialogType.UnblockUser -> {
                // ✅ UnblockUser không được sử dụng trong ProfileViewModel
                // Được xử lý ở BlockedUsersViewModel
            }
            is DialogType.None -> return
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
    
    private fun onConfirmBlock(targetUserId: String) {
        val state = _uiState.value
        val currentUserId = state.currentUserId ?: userRepository.getCurrentUserId() ?: return
        
        if (state.currentUserId == null) {
            _uiState.update { it.copy(currentUserId = currentUserId) }
        }
        
        _uiState.update { it.copy(isProcessing = true) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = userRepository.blockUser(currentUserId, targetUserId)
                
                if (success) {
                    _uiState.update {
                        it.copy(
                            dialogType = DialogType.None,
                            isProcessing = false,
                            isUserBlocked = true,
                            shouldNavigateBack = true, // ✅ Flag để navigate back
                            successMessage = "Người dùng đã bị chặn."
                        )
                    }
                    Log.d("ProfileViewModel", "User blocked successfully: $targetUserId")
                } else {
                    _uiState.update {
                        it.copy(
                            dialogType = DialogType.None,
                            isProcessing = false,
                            error = "Lỗi không thể chặn người dùng."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error blocking user", e)
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.None,
                        isProcessing = false,
                        error = "Lỗi khi chặn người dùng. Vui lòng thử lại."
                    )
                }
            }
        }
    }


        fun openChatWithUser(targetUserId: String, onChatReady: (String) -> Unit) {
            viewModelScope.launch {
                try {
                    val currentUserId = userRepository.getCurrentUserId()

                    if (currentUserId == null) {
                        Log.w("ProfileViewModel", "Cannot open chat: User not logged in")
                        return@launch
                    }                    // 🔹 Kiểm tra chat đã tồn tại chưa
                    val existingChatId = chatRepository.getExistingChatId(targetUserId)

                    // 🔹 Nếu có rồi → mở ngay
                    if (existingChatId != null) {
                        onChatReady(existingChatId)
                    } else {
                        // 🔹 Nếu chưa có → tạo chatId tạm để vào ChatScreen trống
                        val newChatId = chatRepository.buildChatId(currentUserId, targetUserId)
                        onChatReady(newChatId)
                    }
                } catch (e: Exception) {
                    Log.e("ProfileViewModel", "Lỗi mở chat", e)
                }
            }
        }

    fun onDeleteClicked(postId: String) {
        // Kiểm tra xem người dùng hiện tại có phải chủ bài không
        val post = _uiState.value.posts.find { it.id == postId }
        if (post != null && post.userId == _uiState.value.currentUserId) {
            _uiState.update {
                it.copy(
                    dialogType = DialogType.DeletePost(postId)
                )
            }
        }
    }

    private fun onConfirmDelete(postId: String) {
        _uiState.update { it.copy(isProcessing = true) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = postRepository.deletePost(postId)
                if (success) {
                    val updatedPosts = _uiState.value.posts.filter { it.id != postId }
                    _uiState.update {
                        it.copy(
                            posts = updatedPosts,
                            postCount = updatedPosts.size,
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
                Log.e("ProfileViewModel", "Error deleting post", e)
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.None,
                        isProcessing = false,
                        error = "Lỗi không thể xóa bài viết."
                    )
                }
            }
        }
    }
}

