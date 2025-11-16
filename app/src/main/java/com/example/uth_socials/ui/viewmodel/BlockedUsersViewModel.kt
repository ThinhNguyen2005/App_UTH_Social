package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class BlockedUserItem(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null
)

data class BlockedUsersUiState(
    val blockedUsers: List<BlockedUserItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,
    val dialogType: DialogType = DialogType.None,
    val isProcessing: Boolean = false
)

class BlockedUsersViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState
    
    init {
        loadBlockedUsers()
    }
    
    private fun loadBlockedUsers() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = userRepository.getCurrentUserId() ?: return@launch
                val blockedUserIds = userRepository.getBlockedUsers(currentUserId)
                
                // Load thông tin chi tiết của từng blocked user
                val blockedUsersList = blockedUserIds.mapNotNull { userId ->
                    val user = userRepository.getUser(userId)
                    user?.let {
                        BlockedUserItem(
                            userId = it.id,
                            username = it.username,
                            avatarUrl = it.avatarUrl
                        )
                    }
                }
                
                _uiState.update {
                    it.copy(
                        blockedUsers = blockedUsersList,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("BlockedUsersViewModel", "Error loading blocked users", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Lỗi tải danh sách người dùng đã chặn."
                    )
                }
            }
        }
    }
    
    fun onUnblockClicked(userId: String, username: String) {
        _uiState.update {
            it.copy(
                dialogType = DialogType.UnblockUser(
                    userId = userId,
                    username = username
                )
            )
        }
    }
    
    fun onConfirmDialog() {
        when (val dialog = _uiState.value.dialogType) {
            is DialogType.UnblockUser -> onConfirmUnblock(dialog.userId)
            is DialogType.None -> return
            else -> return
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
    
    private fun onConfirmUnblock(targetUserId: String) {
        val currentUserId = userRepository.getCurrentUserId() ?: return
        
        _uiState.update { it.copy(isProcessing = true) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = userRepository.unblockUser(currentUserId, targetUserId)
                
                if (success) {
                    // Xóa user khỏi danh sách
                    val updatedList = _uiState.value.blockedUsers.filter { it.userId != targetUserId }
                    _uiState.update {
                        it.copy(
                            blockedUsers = updatedList,
                            dialogType = DialogType.None,
                            isProcessing = false,
                            successMessage = "Đã gỡ chặn người dùng thành công."
                        )
                    }
                    Log.d("BlockedUsersViewModel", "User unblocked successfully: $targetUserId")
                } else {
                    _uiState.update {
                        it.copy(
                            dialogType = DialogType.None,
                            isProcessing = false,
                            error = "Lỗi không thể gỡ chặn người dùng."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("BlockedUsersViewModel", "Error unblocking user", e)
                _uiState.update {
                    it.copy(
                        dialogType = DialogType.None,
                        isProcessing = false,
                        error = "Lỗi khi gỡ chặn người dùng. Vui lòng thử lại."
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    fun refresh() {
        loadBlockedUsers()
    }
}

