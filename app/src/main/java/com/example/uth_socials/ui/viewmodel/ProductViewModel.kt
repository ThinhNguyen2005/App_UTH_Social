package com.example.uth_socials.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.market.Product
import com.example.uth_socials.data.repository.ProductRepository
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.sql.Timestamp
import java.util.UUID

class ProductViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    private val userRepository: UserRepository = UserRepository()
    private val productRepository: ProductRepository = ProductRepository()

    var isLoading by mutableStateOf(false)
    var success by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var showBanDialog by mutableStateOf(false)

    fun uploadPost(
        user: User?,
        name: String,
        description: String,
        type: String,
        price: Int,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            // return try {
            val userId = userRepository.getCurrentUserId()
            // Check ban status trước khi đăng
            val userRepository = UserRepository()
            if (user?.isBanned == true) {
                showBanDialog = true
                return@launch
            }

            val imageUrls = mutableListOf<String>()

            for (uri in imageUris) {
                val ref = storage.reference.child("posts/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                imageUrls.add(uri.toString())
            }

            productRepository.uploadPost(user, name, description, type, price, imageUrls)

            // Tạo bài viết

            // true
            //  } catch (e: Exception) {
            // e.printStackTrace()
            //  false
            //}
        }
    }

}