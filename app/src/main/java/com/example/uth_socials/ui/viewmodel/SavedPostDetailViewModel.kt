package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.PostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavedPostDetailViewModel(
    private val postId: String
) : ViewModel() {

    private val postRepository = PostRepository()

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPost()
    }

    private fun loadPost() {
        viewModelScope.launch {
            try {
                postRepository.getPostsFlow("all")
                    .map { posts -> posts.find { it.id == postId } }
                    .collect { foundPost ->
                        _post.value = foundPost
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("PostDetailVM", "Error loading post", e)
                _errorMessage.value = "Không thể tải bài viết"
                _isLoading.value = false
            }
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            _post.value?.let { post ->
                try {
                    postRepository.toggleLikeStatus(post.id, post.isLiked)
                } catch (e: Exception) {
                    Log.e("PostDetailVM", "Error toggling like", e)
                    _errorMessage.value = "Không thể thích bài viết"
                }
            }
        }
    }

    fun toggleSave() {
        viewModelScope.launch {
            _post.value?.let { post ->
                try {
                    postRepository.toggleSaveStatus(post.id, post.isSaved)
                } catch (e: Exception) {
                    Log.e("PostDetailVM", "Error toggling save", e)
                    _errorMessage.value = "Không thể lưu bài viết"
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class PostDetailViewModelFactory(
    private val postId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedPostDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedPostDetailViewModel(postId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}