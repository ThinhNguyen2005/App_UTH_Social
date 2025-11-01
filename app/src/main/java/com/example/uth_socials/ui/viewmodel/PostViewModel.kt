package com.example.uth_socials.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.sql.Timestamp
import java.util.UUID

data class Post(
    val id: String = "",
    val userId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val likes: Long = 0,
    val comments: Long = 0,
    val shares: Long = 0,
    val saves: Long = 0,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
)

class PostViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    var content by mutableStateOf("")
    var imageUri by mutableStateOf<Uri?>(null)
    var isLoading by mutableStateOf(false)
    var success by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun postArticle(userId: String) {
        viewModelScope.launch {
            isLoading = true
            val result = uploadPost(content, imageUri, userId)
            isLoading = false

            if (result) {
                success = true
                content = ""
                imageUri = null
            } else {
                error = "Đăng bài thất bại, vui lòng thử lại."
            }
        }
    }

    suspend fun uploadPost(content: String, imageUri: Uri?, userId: String): Boolean {
        return try {
            var imageUrl: String? = null

            // Nếu có ảnh → upload lên Storage
            if (imageUri != null) {
                val ref = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
                ref.putFile(imageUri as Uri).await()
                imageUrl = ref.downloadUrl.await().toString()
            }

            // Tạo bài viết
            val post = Post(
                id = UUID.randomUUID().toString(),
                userId = userId,
                content = content,
                imageUrl = imageUrl
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