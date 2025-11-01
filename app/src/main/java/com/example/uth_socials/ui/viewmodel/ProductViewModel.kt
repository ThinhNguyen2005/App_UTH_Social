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
import java.util.UUID

data class Product(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val description: String = "",
    val type: String = "",
    val price: Double = 0.0,
    val comments: Long = 0,
    val shares: Long = 0,
    val saves: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class ProductViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    var name by mutableStateOf("")
    var description by mutableStateOf("")
    var type by mutableStateOf("")
    var price by mutableStateOf(0.0)
    var imageUri by mutableStateOf<Uri?>(null)
    var isLoading by mutableStateOf(false)
    var success by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun postArticle(userId: String) {
        viewModelScope.launch {
            isLoading = true
            val result = uploadPost(name, description, type, price, imageUri, userId)
            isLoading = false

            if (result) {
                success = true
                name = ""
                imageUri = null
            } else {
                error = "Đăng bài thất bại, vui lòng thử lại."
            }
        }
    }

    suspend fun uploadPost(name: String, description: String, type: String, price: Double, imageUri: Uri?, userId: String): Boolean {
        return try {
            var imageUrl: String? = null

            // Nếu có ảnh → upload lên Storage
            if (imageUri != null) {
                val ref = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
                ref.putFile(imageUri as Uri).await()
                imageUrl = ref.downloadUrl.await().toString()
            }

            // Tạo bài viết
            val post = Product(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                description = description,
                type = type,
                price = price,
                imageUrl = imageUrl
            )

            // Lưu lên Firestore
            db.collection("products").document(post.id).set(post).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}