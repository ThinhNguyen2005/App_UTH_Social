package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FollowListType {
    FOLLOWERS,
    FOLLOWING
}

data class FollowListUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val listType: FollowListType = FollowListType.FOLLOWERS,
    val title: String = "Người theo dõi"
)

class FollowListViewModel(
    private val userId: String,
    private val listType: FollowListType,
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState(listType = listType))
    val uiState: StateFlow<FollowListUiState> = _uiState

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        title = if (listType == FollowListType.FOLLOWERS)
                            "Người theo dõi" else "Đang theo dõi"
                    )
                }

                val user = userRepository.getUser(userId)
                if (user == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Không tìm thấy người dùng")
                    }
                    return@launch
                }

                val userIds = when (listType) {
                    FollowListType.FOLLOWERS -> user.followers
                    FollowListType.FOLLOWING -> user.following
                }

                val users = userIds.mapNotNull { id ->
                    try {
                        userRepository.getUser(id)
                    } catch (e: Exception) {
                        Log.e("FollowListViewModel", "Error fetching user $id", e)
                        null
                    }
                }

                _uiState.update {
                    it.copy(users = users, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                Log.e("FollowListViewModel", "Error loading users", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Lỗi tải danh sách: ${e.message}")
                }
            }
        }
    }

    fun refresh() {
        loadUsers()
    }
}

class FollowListViewModelFactory(
    private val userId: String,
    private val listType: FollowListType
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FollowListViewModel::class.java)) {
            return FollowListViewModel(userId, listType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}