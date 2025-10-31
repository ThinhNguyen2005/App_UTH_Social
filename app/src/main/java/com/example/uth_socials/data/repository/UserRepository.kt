package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * ✅ HÀM QUAN TRỌNG: Tạo hồ sơ người dùng nếu chưa tồn tại.
     * Gọi hàm này ngay sau khi người dùng đăng nhập bằng Google thành công.
     */
    suspend fun createUserProfileIfNotExists(firebaseUser: FirebaseUser) {
        val userRef = usersCollection.document(firebaseUser.uid)
        if (userRef.get().await().exists()) {
            // Người dùng đã tồn tại, không cần làm gì thêm
            return
        }

        // Tạo đối tượng User mới từ thông tin của FirebaseUser
        val newUser = User(
            id = firebaseUser.uid,
            username = firebaseUser.displayName ?: "Người dùng mới",
            avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
            bio = "Xin chào!", // Bio mặc định
            followers = emptyList(),
            following = emptyList()
        )

        try {
            userRef.set(newUser).await()
            Log.d("UserRepository", "Created new user profile for ${firebaseUser.uid}")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error creating user profile", e)
        }
    }

    /**
     * Lấy thông tin chi tiết của một người dùng.
     */
    suspend fun getUser(userId: String): User? {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.toObject(User::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user", e)
            null
        }
    }

    /**
     * Xử lý logic theo dõi/bỏ theo dõi.
     */
    suspend fun toggleFollow(currentUserId: String, targetUserId: String, isCurrentlyFollowing: Boolean): Boolean {
        val currentUserRef = usersCollection.document(currentUserId)
        val targetUserRef = usersCollection.document(targetUserId)

        return try {
            db.runBatch { batch ->
                if (isCurrentlyFollowing) {
                    // Bỏ theo dõi
                    batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
                    batch.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))
                } else {
                    // Theo dõi
                    batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetUserId))
                    batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUserId))
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error toggling follow", e)
            false
        }
    }

    /**
     * Chặn một người dùng.
     */
    suspend fun blockUser(blockerId: String, targetUserId: String): Boolean {
        return try {
            usersCollection.document(blockerId)
                .update("blockedUsers", FieldValue.arrayUnion(targetUserId))
                .await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error blocking user", e)
            false
        }
    }



}