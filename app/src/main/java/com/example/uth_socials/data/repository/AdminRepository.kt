package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.post.AdminAction
import com.example.uth_socials.data.post.AdminReport
import com.example.uth_socials.data.post.Report
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.user.AdminStatus
import com.example.uth_socials.data.user.AdminUser
import com.example.uth_socials.data.user.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import com.example.uth_socials.data.util.FirestoreConstants

/**
 * Repository for managing admin users and admin operations
 */
class AdminRepository(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository()
) {
    private val db = FirebaseFirestore.getInstance()
    private val adminCollection = db.collection(FirestoreConstants.ADMIN_USERS_COLLECTION)
    private val auth = FirebaseAuth.getInstance()

    /**
     * Check if user is admin (super admin or firestore admin)
     */
    suspend fun isAdmin(userId: String): Boolean {
        return getAdminStatus(userId).isAdmin
    }

    /**
     * Check if user is super admin
     * @deprecated Use getAdminStatus() for better performance when you need both flags
     */
    suspend fun isSuperAdmin(userId: String): Boolean {
        return getAdminStatus(userId).isSuperAdmin
    }
    suspend fun getAdminStatus(userId: String): AdminStatus {
        val doc = adminCollection.document(userId).get().await()

        if (!doc.exists()) {
            return AdminStatus(isAdmin = false, isSuperAdmin = false)
        }

        val role = doc.getString(FirestoreConstants.FIELD_ROLE) ?: ""

        return AdminStatus(
            isAdmin = true,
            isSuperAdmin = role == "super_admin"
        )
    }


    /**
     * Grant admin role to user (only super admin can do this)
     */
    suspend fun grantAdminRole(
        targetUserId: String,
        role: String,
        grantedBy: String,
        permissions: List<String> = emptyList()
    ): Result<Int> = runCatching {
        //Đầu vào kiểm tra
        require(targetUserId.isNotBlank()) { "Target user ID cannot be blank" }
        require(grantedBy.isNotBlank()) { "GrantedBy cannot be blank" }
        require(role.isNotBlank()) { "Role cannot be blank" }

        // Dữ liệu admin
        val adminData = mapOf(
            FirestoreConstants.FIELD_ROLE to role,
            FirestoreConstants.FIELD_GRANTED_BY to grantedBy,
            FirestoreConstants.FIELD_GRANTED_AT to FieldValue.serverTimestamp(),
            FirestoreConstants.FIELD_PERMISSIONS to permissions
        )

        adminCollection.document(targetUserId).set(adminData).await()

        Log.d("AdminRepository", "Granted admin role '$role' to user $targetUserId by $grantedBy")
        1 // Return success code (e.g., 1 for success)
    }.onFailure { e ->
        Log.e("AdminRepository", "Failed to grant admin role to $targetUserId", e)
    }
    suspend fun revokeAdminRole(userId: String): Result<Unit> = runCatching {
        adminCollection.document(userId).delete().await()
    }

    suspend fun getAllAdmins(): List<AdminUser> {
        return try {
            adminCollection.get().await().documents.mapNotNull { doc ->
                AdminUser(
                    userId = doc.id,
                    role = doc.getString(FirestoreConstants.FIELD_ROLE) ?: "",
                    grantedBy = doc.getString(FirestoreConstants.FIELD_GRANTED_BY) ?: "",
                    grantedAt = doc.getTimestamp(FirestoreConstants.FIELD_GRANTED_AT),
                    permissions = (doc.get(FirestoreConstants.FIELD_PERMISSIONS) as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList())
            }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting all admins", e)
            emptyList()
        }
    }
    
    /**
     * Fetches reports first, then collects all user IDs and post IDs.
     * Uses batch fetching (whereIn) to get all related users and posts in fewer queries.
     */
    suspend fun getPendingReports(): List<AdminReport> {
        return try {
            // 1. Get all pending reports
            val reportsSnapshot = db.collection(FirestoreConstants.REPORTS_COLLECTION)
                .whereEqualTo(FirestoreConstants.FIELD_STATUS, FirestoreConstants.STATUS_PENDING)
                .get()
                .await()

            val reports = reportsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Report::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.timestamp }

            if (reports.isEmpty()) return emptyList()

            // 2. Collect all unique Post IDs and User IDs (from posts)
            val postIds = reports.map { it.postId }.distinct().filter { it.isNotEmpty() }
            
            // 3. Batch fetch Posts (chunked by 10 due to Firestore limit)
            val postsMap = mutableMapOf<String, Post>()
            postIds.chunked(10).forEach { chunk ->
                try {
                    val postsSnapshot = db.collection(FirestoreConstants.POSTS_COLLECTION)
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    
                    postsSnapshot.documents.forEach { doc ->
                        doc.toObject(Post::class.java)?.let { post ->
                            postsMap[doc.id] = post.copy(id = doc.id)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminRepository", "Error fetching posts chunk", e)
                }
            }

            // 4. Collect User IDs from fetched Posts
            val userIds = postsMap.values.map { it.userId }.distinct().filter { it.isNotEmpty() }
            
            // 5. Batch fetch Users (chunked by 10)
            val usersMap = mutableMapOf<String, User>()
            userIds.chunked(10).forEach { chunk ->
                 try {
                    val usersSnapshot = db.collection(FirestoreConstants.USERS_COLLECTION)
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    
                    usersSnapshot.documents.forEach { doc ->
                        doc.toObject(User::class.java)?.let { user ->
                            usersMap[doc.id] = user.apply { 
                                id = doc.id 
                                this.userId = doc.id
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminRepository", "Error fetching users chunk", e)
                }
            }

            // 6. Assemble AdminReports
            reports.map { report ->
                val post = postsMap[report.postId]
                val reportedUser = post?.let {
                    usersMap[it.userId] ?: User().apply {
                        username = "[Deleted User]"
                        id = it.userId
                    }
                }
                AdminReport(
                    report = report,
                    post = post,
                    reportedUser = reportedUser
                )
            }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting pending reports", e)
            emptyList()
        }
    }

    /**
     * Review and take action on a report
     */
    suspend fun reviewReport(
        reportId: String,
        adminId: String,
        action: AdminAction,
        adminNotes: String? = null
    ): Result<Unit> = runCatching {
        val reportRef = db.collection(FirestoreConstants.REPORTS_COLLECTION).document(reportId)
        val reportSnapshot = reportRef.get().await()

        val postId = reportSnapshot.getString(FirestoreConstants.FIELD_POST_ID)

        val post = getPostById(postId ?: "")
        val targetUserId = post?.userId

        if (targetUserId != null && (isAdmin(targetUserId) || isSuperAdmin(targetUserId))) {
            throw IllegalArgumentException("Cannot perform admin actions on admin users")
        }

        val updateData = mutableMapOf(
            FirestoreConstants.FIELD_STATUS to if (action == AdminAction.DISMISS) FirestoreConstants.STATUS_DISMISSED else FirestoreConstants.STATUS_REVIEWED,
            FirestoreConstants.FIELD_REVIEWED_BY to adminId,
            FirestoreConstants.FIELD_REVIEWED_AT to FieldValue.serverTimestamp(),
            FirestoreConstants.FIELD_ADMIN_ACTION to action.name
        ).apply {
            adminNotes?.let { this[FirestoreConstants.FIELD_ADMIN_NOTES] = it }
        }

        reportRef.update(updateData).await()

        when (action) {
            AdminAction.DELETE_POST, AdminAction.BAN_USER -> {
                // Sử dụng post và userId đã lấy ở trên, không cần lấy lại
                targetUserId?.let { userId ->
                    when (action) {
                        AdminAction.DELETE_POST -> {
                            postId?.let { postRepository.deletePost(it) }
                            // Tăng violation count và tự động ban nếu >= 3 vi phạm
                            autoBanUser(userId).onFailure { e ->
                                Log.e("AdminRepository", "Failed to auto-ban user $userId after post deletion", e)
                            }
                        }
                        AdminAction.BAN_USER -> {
                            banUser(userId, adminId, "Bị cấm do báo cáo: ${adminNotes ?: "Không cung cấp lý do"}")
                        }
                        else -> Unit
                    }
                } ?: Log.w("AdminRepository", "Cannot perform action $action: targetUserId is null")
            }
            else -> Unit
        }

        Log.d("AdminRepository", "Report $reportId reviewed by admin $adminId with action: $action")
    }


    suspend fun banUser(userId: String, adminId: String, reason: String): Result<Unit> = runCatching {
        if (adminId != "system") {
            val isAdminUser = isAdmin(adminId)
            if (!isAdminUser) {
                throw SecurityException("User $adminId does not have admin privileges to ban users")
            }
        }

        val userRef = db.collection(FirestoreConstants.USERS_COLLECTION).document(userId)
        val banData = mapOf(
            FirestoreConstants.FIELD_IS_BANNED to true,
            FirestoreConstants.FIELD_BANNED_AT to FieldValue.serverTimestamp(),
            FirestoreConstants.FIELD_BANNED_BY to adminId,
            FirestoreConstants.FIELD_BAN_REASON to reason
        )
        userRef.update(banData).await()

        // Thay vì gọi getUser() có thể trả về dữ liệu cũ do cache
        var retryCount = 0
        val maxRetries = 3
        var isActuallyBanned = false
        
        while (retryCount < maxRetries && !isActuallyBanned) {
            kotlinx.coroutines.delay(100L * (retryCount + 1)) // Tăng delay mỗi lần retry
            
            val updatedDoc = userRef.get(com.google.firebase.firestore.Source.SERVER).await()
            isActuallyBanned = updatedDoc.getBoolean(FirestoreConstants.FIELD_IS_BANNED) == true
            
            if (!isActuallyBanned) {
                retryCount++
                Log.d("AdminRepository", "Ban verification retry $retryCount/$maxRetries for user $userId")
            }
        }
        
        if (!isActuallyBanned) {
            throw Exception("Ban verification failed - user state not updated correctly after $maxRetries retries")
        }
        Log.d("AdminRepository", "🎉 User $userId banned successfully by admin $adminId for: $reason")
    }
    suspend fun unbanUser(userId: String): Result<Unit> = runCatching {
        val userRef = db.collection(FirestoreConstants.USERS_COLLECTION).document(userId)

        val unbanData = mapOf(
            FirestoreConstants.FIELD_IS_BANNED to false,
            FirestoreConstants.FIELD_BANNED_AT to null,
            FirestoreConstants.FIELD_BANNED_BY to null,
            FirestoreConstants.FIELD_BAN_REASON to null
        )

        userRef.update(unbanData).await()
        Log.d("AdminRepository", "User $userId unbanned")
    }

    suspend fun autoBanUser(userId: String): Result<Unit> = runCatching {
        Log.d("AdminRepository", "autoBanUser called for user: $userId")
        
        val userRef = db.collection(FirestoreConstants.USERS_COLLECTION).document(userId)

        // Lấy current user để check violation count
        val currentUser = userRepository.getUser(userId)
        if (currentUser == null) {
            Log.w("AdminRepository", "User $userId not found!")
            throw Exception("User not found: $userId")
        }
        
        val currentViolations = currentUser.violationCount
        val newViolationCount = currentViolations + 1

        Log.d("AdminRepository", "User $userId violations: $currentViolations -> $newViolationCount")

        // Update violation count
        userRef.update(FirestoreConstants.FIELD_VIOLATION_COUNT, newViolationCount).await()
        
        Log.d("AdminRepository", "Violation count updated in Firestore for user: $userId")

        // Kiểm tra và ban nếu cần
        if (newViolationCount >= 3) {
            val updatedUser = userRepository.getUser(userId)
            val isCurrentlyBanned = updatedUser?.isBanned == true
            
            Log.d("AdminRepository", "Checking ban status: isBanned=$isCurrentlyBanned, violations=$newViolationCount")
            
            if (!isCurrentlyBanned) {
                Log.d("AdminRepository", "Auto-banning user $userId (violations: $newViolationCount)")
                banUser(
                    userId = userId,
                    adminId = "system",
                    reason = "Tự động cấm: Quá nhiều vi phạm ($newViolationCount bài viết đã xóa)"
                ).onSuccess {
                    Log.d("AdminRepository", "User $userId auto-banned successfully")
                }.onFailure { e ->
                    Log.e("AdminRepository", "Failed to ban user $userId", e)
                    throw e
                }
            } else {
                Log.d("AdminRepository", "⏭User $userId already banned, skipping")
            }
        } else {
            Log.d("AdminRepository", "User $userId has $newViolationCount violations (need 3+ to ban)")
        }
        
        Log.d("AdminRepository", "autoBanUser completed for user: $userId")
    }

    suspend fun getBannedUsers(): List<User> {
        return try {
            val bannedUsersSnapshot = db.collection(FirestoreConstants.USERS_COLLECTION)
                .whereEqualTo(FirestoreConstants.FIELD_IS_BANNED, true)
                .get()
                .await()

            bannedUsersSnapshot.documents.mapNotNull { doc ->
                User(
                    id = doc.id,
                    username = doc.getString(FirestoreConstants.FIELD_USERNAME) ?: "",
                    bannedAt = doc.getTimestamp(FirestoreConstants.FIELD_BANNED_AT),
                    bannedBy = doc.getString(FirestoreConstants.FIELD_BANNED_BY),
                    banReason = doc.getString(FirestoreConstants.FIELD_BAN_REASON),
                    violationCount = doc.getLong(FirestoreConstants.FIELD_VIOLATION_COUNT)?.toInt() ?: 0,
                    warningCount = doc.getLong(FirestoreConstants.FIELD_WARNING_COUNT)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting banned users", e)
            emptyList()
        }
    }

    /**
     * Realtime listener cho danh sách người dùng bị cấm
     * Tự động cập nhật khi có thay đổi trong Firestore
     */
    fun getBannedUsersFlow(): Flow<List<User>> = callbackFlow {
        val query = db.collection(FirestoreConstants.USERS_COLLECTION)
            .whereEqualTo(FirestoreConstants.FIELD_IS_BANNED, true)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdminRepository", "Error listening to banned users", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val bannedUsers = snapshot.documents.mapNotNull { doc ->
                    try {
                        User(
                            id = doc.id,
                            username = doc.getString(FirestoreConstants.FIELD_USERNAME) ?: "",
                            bannedAt = doc.getTimestamp(FirestoreConstants.FIELD_BANNED_AT),
                            bannedBy = doc.getString(FirestoreConstants.FIELD_BANNED_BY),
                            banReason = doc.getString(FirestoreConstants.FIELD_BAN_REASON),
                            violationCount = doc.getLong(FirestoreConstants.FIELD_VIOLATION_COUNT)?.toInt() ?: 0,
                            warningCount = doc.getLong(FirestoreConstants.FIELD_WARNING_COUNT)?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        Log.e("AdminRepository", "Error parsing banned user ${doc.id}", e)
                        null
                    }
                }
                Log.d("AdminRepository", "Realtime update: ${bannedUsers.size} banned users")
                trySend(bannedUsers)
            }
        }

        awaitClose { 
            Log.d("AdminRepository", "Removing banned users listener")
            listener.remove() 
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getPostById(postId: String): Post? {
        return try {
            val postDoc = db.collection(FirestoreConstants.POSTS_COLLECTION).document(postId).get().await()
            if (postDoc.exists()) {
                postDoc.toObject(Post::class.java)?.copy(id = postDoc.id)
            } else null
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting post $postId", e)
            null
        }
    }

    suspend fun getCurrentUserAdminStatus(): AdminStatus {
        val uid = auth.currentUser?.uid ?: return AdminStatus(isAdmin = false, isSuperAdmin = false)
        return getAdminStatus(uid)
    }
}

