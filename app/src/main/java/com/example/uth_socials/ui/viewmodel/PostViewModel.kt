package com.example.uth_socials.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Category
import com.google.firebase.Firebase
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.getField
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.sql.Timestamp
import java.util.Locale
import java.util.UUID
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.ProductRepository
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow

class PostViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private val PostRepository = PostRepository()
    private val productRepository: ProductRepository = ProductRepository()
    private val userRepository: UserRepository = UserRepository()
    private val CategoryRepository: CategoryRepository = CategoryRepository()

    var showBanDialog by mutableStateOf(false)

    var isLoading by mutableStateOf(false)
    var success by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val hiddenPostIds: Set<String> = emptySet()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categories.value = CategoryRepository.getCategories()
        }
    }

    fun uploadArticle(
        content: String, imageUris: List<Uri>, category: String?,
    ) {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            val user = userRepository.getUser(userId ?: "")

            val imageUrls = mutableListOf<String>()

            for (uri in imageUris) {
                val ref = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                imageUrls.add(url)
            }

            PostRepository.uploadArticle(user, content, category, imageUrls)
        }
    }

    fun uploadProduct(name: String, description: String, type: String, price: Int, imageUris: List<Uri>) {
        viewModelScope.launch {
            // return try {
            val userId = userRepository.getCurrentUserId()
            val user = userRepository.getUser(userId ?: "")

            val imageUrls = mutableListOf<String>()

            for (uri in imageUris) {
                val ref = storage.reference.child("products/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                imageUrls.add(url)
            }

            PostRepository.uploadProduct(user, name, description, type, price, imageUrls)

            // Tạo bài viết

            // true
            //  } catch (e: Exception) {
            // e.printStackTrace()
            //  false
            //}
        }
    }

}