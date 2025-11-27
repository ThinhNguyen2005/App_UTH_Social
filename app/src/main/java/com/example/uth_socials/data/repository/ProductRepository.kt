package com.example.uth_socials.data.repository

import android.net.Uri
import com.example.uth_socials.data.market.Product
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.user.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()

    private val storage = Firebase.storage
    private val auth = FirebaseAuth.getInstance()
    private val productsCollection = db.collection("products")
    private val reportsCollection = db.collection("reports")
    private val usersCollection = db.collection("users")

    suspend fun uploadPost(
        user: User?, name: String, description: String, type: String, price: Int, imageUrls : List<String>
    ): Boolean {
        return try {
            // Tạo bài viết
            val product = Product(
                id = UUID.randomUUID().toString(),
                name = name,
                userId = user?.id ?: "",
                userName = user?.username ?: "",
                userAvatar = user?.avatarUrl ?: "",
                description = description,
                type = type,
                price = price,
                imageUrls = imageUrls
            )

            // Lưu lên Firestore
            productsCollection.document(product.id).set(product).await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}