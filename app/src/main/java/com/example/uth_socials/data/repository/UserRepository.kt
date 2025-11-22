package com.example.uth_socials.data.repository

import android.net.Uri
import android.util.Log

import com.example.uth_socials.data.user.User
import com.example.uth_socials.data.util.SecurityValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import androidx.core.net.toUri
import com.example.uth_socials.data.util.FirestoreConstants

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection(FirestoreConstants.USERS_COLLECTION)

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun createUserProfileIfNotExists(firebaseUser: FirebaseUser,username: String? = null) {
        val userRef = usersCollection.document(firebaseUser.uid)
        if (userRef.get().await().exists()) {
            return
        }

        val newUser = User(
            id = firebaseUser.uid,
            userId = firebaseUser.uid,

            usernameFormat = firebaseUser.displayName.toString().trim().lowercase(),

            username = username?:firebaseUser.displayName ?: "User",
            avatarUrl = firebaseUser.photoUrl?.toString() ?:"https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
            bio = "Xin chào!",
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
                FirestoreConstants.FIELD_USERNAME to "",
                FirestoreConstants.FIELD_AVATAR_URL to "",
                FirestoreConstants.FIELD_FOLLOWERS to emptyList<String>(),
                FirestoreConstants.FIELD_FOLLOWING to emptyList<String>(),
                FirestoreConstants.FIELD_HIDDEN_POSTS to emptyList<String>(),
                FirestoreConstants.FIELD_BLOCKED_USERS to emptyList<String>(),
                "bio" to "",
                "userId" to userId
            )
            docRef.set(bootstrap, SetOptions.merge()).await()
            Log.w("UserRepository", "Bootstrap user document for $userId")
        }
    }
    suspend fun getUser(userId: String): User? {
        return try {
            val snapshot = usersCollection.document(userId).get(Source.SERVER).await()
            snapshot.toObject(User::class.java)?.also { user ->
                user.userId = user.userId ?: snapshot.id
                user.id = snapshot.id
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching user", e)
            null
        }
    }

    suspend fun toggleFollow(currentUserId: String, targetUserId: String, isCurrentlyFollowing: Boolean): Boolean {
        if (!SecurityValidator.canModifyFollowers(currentUserId)) {
            Log.w("UserRepository", "toggleFollow denied: currentUserId is null")
            return false
        }
        if (currentUserId == targetUserId) {
            Log.w("UserRepository", "toggleFollow denied: cannot follow yourself")
            return false
        }
        val currentUserRef = usersCollection.document(currentUserId)
        val targetUserRef = usersCollection.document(targetUserId)

        return try {
            db.runBatch { batch ->
                if (isCurrentlyFollowing) {
                    batch.update(currentUserRef, FirestoreConstants.FIELD_FOLLOWING, FieldValue.arrayRemove(targetUserId))
                    batch.update(targetUserRef, FirestoreConstants.FIELD_FOLLOWERS, FieldValue.arrayRemove(currentUserId))
                } else {
                    batch.update(currentUserRef, FirestoreConstants.FIELD_FOLLOWING, FieldValue.arrayUnion(targetUserId))
                    batch.update(targetUserRef, FirestoreConstants.FIELD_FOLLOWERS, FieldValue.arrayUnion(currentUserId))
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error toggling follow", e)
            false
        }
    }
    suspend fun blockUser(blockerId: String, targetUserId: String): Boolean {
        ensureUserDocument(blockerId)
        return try {
            usersCollection.document(blockerId)
                .update(FirestoreConstants.FIELD_BLOCKED_USERS, FieldValue.arrayUnion(targetUserId))
                .await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error blocking user", e)
            false
        }
    }

    suspend fun unblockUser(blockerId: String, targetUserId: String): Boolean {
        ensureUserDocument(blockerId)
        return try {
            usersCollection.document(blockerId)
                .update(FirestoreConstants.FIELD_BLOCKED_USERS, FieldValue.arrayRemove(targetUserId))
                .await()
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unblocking user", e)
            false
        }
    }

    suspend fun getBlockedUsers(userId: String): List<String> {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            (snapshot.get(FirestoreConstants.FIELD_BLOCKED_USERS) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting blocked users", e)
            emptyList()
        }
    }

    suspend fun isUserBlocked(blockerId: String, targetUserId: String): Boolean {
        val blockedUsers = getBlockedUsers(blockerId)
        return blockedUsers.contains(targetUserId)
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

    suspend fun updateUserProfile(username: String, campus: String, phone: String, major: String, avatarUrl: String?,bio: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Chưa đăng nhập")
            val uid = user.uid

            val updates = mutableMapOf<String, Any>()
            updates[FirestoreConstants.FIELD_USERNAME] = username
            updates["campus"] = campus
            updates["phone"] = phone
            updates["major"] = major
            updates["bio"] = bio

            if (avatarUrl != null) {
                updates[FirestoreConstants.FIELD_AVATAR_URL] = avatarUrl
            }

            val profileUpdatesBuilder = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
            if (avatarUrl != null) {
                profileUpdatesBuilder.photoUri = avatarUrl.toUri()
            }


            usersCollection.document(uid).update(updates).await()

            user.updateProfile(profileUpdatesBuilder.build()).await()

            Log.d("UserRepository", "Cập nhật thông tin người dùng thành công cho $uid")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("UserRepository", "Lỗi khi cập nhật thông tin người dùng", e)
            Result.failure(e)
        }
    }
}