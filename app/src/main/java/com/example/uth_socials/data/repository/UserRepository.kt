package com.example.uth_socials.data.repository

import android.net.Uri
import android.util.Log
import com.example.uth_socials.data.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
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
    suspend fun createUserProfileIfNotExists(firebaseUser: FirebaseUser, username: String? = null) {
        val userRef = usersCollection.document(firebaseUser.uid)
        if (userRef.get().await().exists()) {
            // Nếu user đã tồn tại -> bỏ qua
            return
        }

        val newUser = User(
            id = firebaseUser.uid,
            userId = firebaseUser.uid,
            username = username ?: firebaseUser.displayName ?: "Người dùng mới",
            avatarUrl = firebaseUser.photoUrl?.toString()
                ?: "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
            bio = "",
            followers = emptyList(),
            following = emptyList(),
            hiddenPosts = emptyList(),
            blockedUsers = emptyList()
        )

        try {
            userRef.set(newUser).await()
            Log.d("UserRepository", "✅ Created user profile for ${firebaseUser.uid}")
        } catch (e: Exception) {
            Log.e("UserRepository", "❌ Error creating user profile", e)
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
    suspend fun toggleFollow(
        currentUserId: String,
        targetUserId: String,
        isCurrentlyFollowing: Boolean
    ): Boolean {
        ensureUserDocument(currentUserId)
        ensureUserDocument(targetUserId)
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
    suspend fun uploadProfileImage(imageUri: Uri): String {
        val user =auth.currentUser?: throw Exception("Chưa đăng nhập")
        val storageRef = FirebaseStorage.getInstance().reference

        val imageRef = storageRef.child("profile_images/${user.uid}/avatar.jpg")
        imageRef.putFile(imageUri).await()

        val downloadUrl = imageRef.downloadUrl.await().toString()
        return downloadUrl

    }

    // (Trong file UserRepository.kt)

    suspend fun updateUserProfile(username: String, campus: String, phone: String, major: String, avatarUrl: String?) {
        val user = auth.currentUser ?: throw Exception("Chưa đăng nhập")
        val uid = user.uid // Lấy uid từ 'user'

        val updates = mutableMapOf<String, Any>()
        updates["username"] = username
        updates["campus"] = campus
        updates["phone"] = phone
        updates["major"] = major

        if (avatarUrl != null) {
            updates["avatarUrl"] = avatarUrl
        }

        val profileUpdatesBuilder = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
        if (avatarUrl != null) {
            profileUpdatesBuilder.setPhotoUri(Uri.parse(avatarUrl))
        }


        try {
            // 3. 🔽 CẬP NHẬT FIRESTORE (CHỈ 1 LẦN)
            usersCollection.document(uid).update(updates).await()

            // 4. 🔽 CẬP NHẬT AUTH PROFILE (DÙNG BIẾN 'user' BÊN TRÊN)
            user.updateProfile(profileUpdatesBuilder.build()).await()

            Log.d("UserRepository", "Cập nhật thông tin người dùng thành công cho $uid")

        } catch (e: Exception) {
            Log.e("UserRepository", "Lỗi khi cập nhật thông tin người dùng", e)
            throw e
        }
    }

}