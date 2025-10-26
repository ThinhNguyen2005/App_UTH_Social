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
    private val userId: String
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadUserProfile()
        loadUserPosts()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                val userDoc = db.collection("users").document(userId).get().await()
                val user = userDoc.toObject(User::class.java)

                if (user != null) {
                    _uiState.update { current ->
                        current.copy(
                            username = user.username,
                            userAvatarUrl = user.avatarUrl,
                            followers = user.followers.size,
                            following = user.following.size,
                            bio = user.bio,
                            isOwner = currentUserId == userId,
                            isFollowing = user.followers.contains(currentUserId),
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy người dùng.") }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading user profile", e)
                _uiState.update { it.copy(isLoading = false, error = "Lỗi tải thông tin cá nhân.") }
            }
        }
    }

    private fun loadUserPosts() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("posts")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get().await()

                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }

                _uiState.update { it.copy(posts = posts, postCount = posts.size) }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading posts", e)
            }
        }
    }

    fun onFollowClicked() {
        val currentUserId = auth.currentUser?.uid ?: return
        val isCurrentlyFollowing = _uiState.value.isFollowing

        viewModelScope.launch {
            try {
                val userRef = db.collection("users").document(userId)

                if (isCurrentlyFollowing) {
                    userRef.update("followers", FieldValue.arrayRemove(currentUserId))
                    db.collection("users").document(currentUserId)
                        .update("following", FieldValue.arrayRemove(userId))
                } else {
                    userRef.update("followers", FieldValue.arrayUnion(currentUserId))
                    db.collection("users").document(currentUserId)
                        .update("following", FieldValue.arrayUnion(userId))
                }

                _uiState.update {
                    it.copy(
                        isFollowing = !isCurrentlyFollowing,
                        followers = if (isCurrentlyFollowing) it.followers - 1 else it.followers + 1
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error toggling follow state", e)
            }
        }
    }

    fun onBlockUser() {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                db.collection("users").document(currentUserId)
                    .update("blockedUsers", FieldValue.arrayUnion(userId))
                    .await()

                if (_uiState.value.isFollowing) {
                    onFollowClicked()
                }

                Log.d("ProfileViewModel", "User blocked successfully")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error blocking user", e)
            }
        }
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch {
            db.collection("posts").document(postId).delete().await()
            _uiState.value = _uiState.value.copy(
                posts = _uiState.value.posts.filterNot { it.id == postId }
            )
        }
    }
}

