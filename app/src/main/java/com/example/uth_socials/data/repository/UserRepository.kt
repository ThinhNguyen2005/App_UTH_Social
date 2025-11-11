package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Gọi hàm này ngay sau khi người dùng đăng nhập bằng Google thành công.
     */
    suspend fun createUserProfileIfNotExists(firebaseUser: FirebaseUser) {
        val userRef = usersCollection.document(firebaseUser.uid)
        if (userRef.get().await().exists()) {
            // Người dùng đã tồn tại, không cần làm gì thêm
            return
        }

        val newUser = User(
            id = firebaseUser.uid,
            userId = firebaseUser.uid,
            username = firebaseUser.displayName ?: "Người dùng mới",
            avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
            bio = "Xin chào!", // Bio mặc định
            followers = emptyList(),
            following = emptyList(),
            hiddenPosts = emptyList(),
            blockedUsers = emptyList()
        )

        try {
            userRef.set(newUser).await()
            Log.d("UserRepository", "Created new user profile for ${firebaseUser.uid}")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error creating user profile", e)
        }
    }

    private suspend fun ensureUserDocument(userId: String) {
        val docRef = usersCollection.document(userId)
        if (!docRef.get().await().exists()) {
            val bootstrap = mapOf(
                "username" to "",
                "avatarUrl" to "",
                "followers" to emptyList<String>(),
                "following" to emptyList<String>(),
                "hiddenPosts" to emptyList<String>(),
                "blockedUsers" to emptyList<String>(),
                "bio" to "",
                "userId" to userId
            )
            docRef.set(bootstrap, SetOptions.merge()).await()
            Log.w("UserRepository", "Bootstrap user document for $userId")
        }
    }

    /**
     * Lấy thông tin chi tiết của một người dùng.
     */
    suspend fun getUser(userId: String): User? {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.toObject(User::class.java)?.also { user ->
                user.userId = user.userId ?: snapshot.id
                user.id = snapshot.id
            }
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
        ensureUserDocument(blockerId)
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

    /**
     * Verify và ensure user document có đủ fields cho commenting
     */
    suspend fun ensureUserReadyForCommenting(userId: String): Boolean {
        return try {
            val userDoc = usersCollection.document(userId).get().await()

            if (!userDoc.exists()) {
                Log.w("UserRepository", "User document not found for $userId, creating...")
                // Tạo document với dữ liệu tối thiểu
                val bootstrapData = hashMapOf(
                    "userId" to userId,
                    "username" to "User",
                    "avatarUrl" to "",
                    "isBanned" to false,
                    "followers" to emptyList<String>(),
                    "following" to emptyList<String>(),
                    "bio" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                usersCollection.document(userId).set(bootstrapData, SetOptions.merge()).await()
                Log.d("UserRepository", "Created minimal user document for $userId")
                return true
            }

            // Kiểm tra các trường cần thiết
            val isBanned = userDoc.getBoolean("isBanned") ?: false
            val username = userDoc.getString("username") ?: ""
            val avatarUrl = userDoc.getString("avatarUrl") ?: ""

            Log.d("UserRepository", "User $userId ready for commenting: banned=$isBanned, username='$username'")

            // Nếu bị ban thì không cho comment
            if (isBanned) {
                Log.w("UserRepository", "User $userId is banned, cannot comment")
                return false
            }

            // Nếu thiếu username, cập nhật
            if (username.isBlank()) {
                usersCollection.document(userId)
                    .update("username", "User")
                    .await()
                Log.d("UserRepository", "Updated username for $userId")
            }

            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error ensuring user ready for commenting", e)
            false
        }
    }
}