package com.example.uth_socials.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Category
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.getField
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.util.SecurityValidator

class PostViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    var content by mutableStateOf("")

    var category by mutableStateOf("")
    var imageUris: List<Uri> = emptyList()
    var isLoading by mutableStateOf(false)
    var success by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    //private val _categories_id = MutableStateFlow<List<String>>(emptyList())
    val categories = _categories.asStateFlow()
    //val categories_id = _categories_id.asStateFlow()
    var selectedCategory = MutableStateFlow<String?>(null)

    init {
        loadCategories()
    }

    var showBanDialog by mutableStateOf(false)

    fun postArticle(userId: String) {
        viewModelScope.launch {
            // Check ban status trước khi đăng
            val userRepository = UserRepository()
            val user = userRepository.getUser(userId)
            if (user?.isBanned == true) {
                showBanDialog = true
                return@launch
            }
            
            isLoading = true
            val result = uploadPost(content, imageUris, selectedCategory.value,userId)
            isLoading = false

            if (result) {
                success = true
                content = ""
            } else {
                error = "Đăng bài thất bại, vui lòng thử lại."
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            db.collection("categories")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.documents.map { doc ->
                        Category(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            order = doc.getField("order") ?: 0
                        )
                    }
                    _categories.value = list
                }
                .addOnFailureListener { e ->
                    println("❌ Lỗi khi load categories: ${e.message}")
                }
        }
    }

    suspend fun uploadPost(
        content: String, imageUris: List<Uri>, category: String?,
        userId: String
    ): Boolean {
        return try {
            if (!SecurityValidator.canCreatePost(userId, userId)) {
                error = "Bạn không có quyền đăng bài."
                return false
            }
            val userRepository = UserRepository()
            val user = userRepository.getUser(userId)
            val imageUrl = mutableListOf<String>()

            for (uri in imageUris) {
                val ref = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                imageUrl.add(url)
            }

            // Tạo bài viết
            val post = Post(
                id = UUID.randomUUID().toString(),
                username = user?.username ?: "",
                userAvatarUrl = user?.avatarUrl ?: "",
                userId = userId,
                textContent = content,
                imageUrls = imageUrl,
                category  = category
            )

            // Lưu lên Firestore
            db.collection("posts").document(post.id).set(post).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}