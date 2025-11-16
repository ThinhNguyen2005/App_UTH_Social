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

/**
 * UserRepository - Quản lý tất cả thao tác với dữ liệu người dùng
 *
 * Chức năng chính:
 * - Tạo và quản lý user profiles
 * - Xử lý follow/unfollow relationships
 * - Blocking users
 * - Validation cho commenting
 *
 * Kiến trúc: Repository pattern với Firebase Firestore
 * Bảo mật: Client-side validation với SecurityValidator
 *
 * Collections used:
 * - "users": User profiles và relationships
 * - Firebase Auth: Authentication state
 */
class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    // ==========================================
    // AUTHENTICATION HELPERS (Trợ giúp xác thực)
    // ==========================================

    /**
     * Lấy ID của user hiện tại đang đăng nhập
     *
     * @return String? - userId hoặc null nếu chưa đăng nhập
     *
     * Use case: Kiểm tra authentication state trong toàn app
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // ==========================================
    // USER PROFILE MANAGEMENT (Quản lý profile)
    // ==========================================

    /**
     * Tạo user profile trong Firestore nếu chưa tồn tại
     *
     * @param firebaseUser FirebaseUser object từ Google Sign-In
     *
     * Logic:
     * - Check xem user document đã tồn tại chưa
     * - Nếu chưa: tạo document với thông tin cơ bản từ Google
     * - Nếu có rồi: skip (để tránh ghi đè data)
     *
     * Use case: Gọi sau khi Google Sign-In thành công
     * Fields: username, avatarUrl, bio, followers/following arrays
     */
    suspend fun createUserProfileIfNotExists(firebaseUser: FirebaseUser,username: String? = null) {
        val userRef = usersCollection.document(firebaseUser.uid)
        if (userRef.get().await().exists()) {
            // Người dùng đã tồn tại, không cần làm gì thêm
            return
        }

        val newUser = User(
            id = firebaseUser.uid,
            userId = firebaseUser.uid,

            usernameFormat = firebaseUser.displayName.toString().trim().lowercase() ?: "người dùng mới",

            username = username?:firebaseUser.displayName ?: "User",
            avatarUrl = firebaseUser.photoUrl?.toString() ?:"https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
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

    /**
     * Đảm bảo user document tồn tại với dữ liệu tối thiểu
     *
     * @param userId ID của user cần ensure
     *
     * Logic:
     * - Check document exists
     * - Nếu không: tạo với bootstrap data
     * - Sử dụng SetOptions.merge() để không ghi đè data hiện có
     *
     * Use case: Bootstrap user data khi cần thiết
     */
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
     * Lấy thông tin chi tiết của một người dùng từ Firestore
     *
     * @param userId ID của user muốn lấy info
     * @return User? - Object User hoặc null nếu không tìm thấy
     *
     * Logic:
     * - Query user document từ Firestore
     * - Convert to User object
     * - Set userId từ document ID nếu thiếu
     * - Return null nếu có error
     *
     * Use case: Hiển thị profile, check user info
     */
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

    // ==========================================
    // SOCIAL RELATIONSHIPS (Mối quan hệ xã hội)
    // ==========================================

    /**
     * Toggle trạng thái follow/unfollow giữa 2 users
     *
     * @param currentUserId ID của user thực hiện action
     * @param targetUserId ID của user bị follow/unfollow
     * @param isCurrentlyFollowing Trạng thái hiện tại (true = đang follow)
     * @return Boolean - true nếu thành công
     *
     * Logic:
     * - Client-side validation (không follow chính mình)
     * - Sử dụng batch write để đảm bảo consistency
     * - Update cả 2 documents: currentUser.following và targetUser.followers
     * - Nếu follow: add to arrays, nếu unfollow: remove from arrays
     *
     * Security: Client-side permission checks
     * Atomic: Batch operations đảm bảo data consistency
     */
    suspend fun toggleFollow(currentUserId: String, targetUserId: String, isCurrentlyFollowing: Boolean): Boolean {
        // Client-side permission guard to avoid unnecessary writes
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

    // ==========================================
    // USER MODERATION (Điều hành người dùng)
    // ==========================================

    /**
     * Chặn một người dùng (block user)
     *
     * @param blockerId ID của user thực hiện block
     * @param targetUserId ID của user bị block
     * @return Boolean - true nếu block thành công
     *
     * Logic:
     * - Ensure user document exists (bootstrap if needed)
     * - Add targetUserId vào array "blockedUsers" của blocker
     * - User bị block sẽ không thể interact với blocker
     *
     * Use case: Prevent harassment, unwanted interactions
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
//
//    // ==========================================
//    // COMMENT VALIDATION (Validation cho bình luận)
//    // ==========================================
//
//    /**
//     * Verify và ensure user document có đủ fields để comment
//     *
//     * @param userId ID của user muốn check
//     * @return Boolean - true nếu user có thể comment
//     *
//     * Logic:
//     * - Check user document exists, bootstrap if needed
//     * - Validate required fields (username, avatarUrl)
//     * - Check if user is banned (isBanned field)
//     * - Update missing fields với default values
//     *
//     * Security: Prevent banned users from commenting
//     * Data integrity: Ensure user has required profile data
//     *
//     * Use case: Called before allowing user to add comments
//     */
//    suspend fun ensureUserReadyForCommenting(userId: String): Boolean {
//        return try {
//            val userDoc = usersCollection.document(userId).get().await()
//
//            if (!userDoc.exists()) {
//                Log.w("UserRepository", "User document not found for $userId, creating...")
//                // Tạo document với dữ liệu tối thiểu
//                val bootstrapData = hashMapOf(
//                    "userId" to userId,
//                    "username" to "User",
//                    "avatarUrl" to "",
//                    "isBanned" to false,
//                    "followers" to emptyList<String>(),
//                    "following" to emptyList<String>(),
//                    "bio" to "",
//                    "createdAt" to FieldValue.serverTimestamp()
//                )
//                usersCollection.document(userId).set(bootstrapData, SetOptions.merge()).await()
//                Log.d("UserRepository", "Created minimal user document for $userId")
//                return true
//            }
//
//            // Kiểm tra các trường cần thiết
//            val isBanned = userDoc.getBoolean("isBanned") ?: false
//            val username = userDoc.getString("username") ?: ""
//            val avatarUrl = userDoc.getString("avatarUrl") ?: ""
//
//            Log.d("UserRepository", "User $userId ready for commenting: banned=$isBanned, username='$username'")
//
//            // Nếu bị ban thì không cho comment
//            if (isBanned) {
//                Log.w("UserRepository", "User $userId is banned, cannot comment")
//                return false
//            }
//
//            // Nếu thiếu username, cập nhật
//            if (username.isBlank()) {
//                usersCollection.document(userId)
//                    .update("username", "User")
//                    .await()
//                Log.d("UserRepository", "Updated username for $userId")
//            }
//
//            true
//        } catch (e: Exception) {
//            Log.e("UserRepository", "Error ensuring user ready for commenting", e)
//            false
//        }
//    }
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
        val uid = user.uid

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
            // 3. CẬP NHẬT FIRESTORE (CHỈ 1 LẦN)
            usersCollection.document(uid).update(updates).await()

            // 4. CẬP NHẬT AUTH PROFILE (DÙNG BIẾN 'user' BÊN TRÊN)
            user.updateProfile(profileUpdatesBuilder.build()).await()

            Log.d("UserRepository", "Cập nhật thông tin người dùng thành công cho $uid")

        } catch (e: Exception) {
            Log.e("UserRepository", "Lỗi khi cập nhật thông tin người dùng", e)
            throw e
        }
    }
}