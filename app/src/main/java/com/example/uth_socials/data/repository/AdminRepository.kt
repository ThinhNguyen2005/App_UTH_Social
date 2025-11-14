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

/**
 * Repository for managing admin users and admin operations
 */
class AdminRepository(
    private val userRepository: UserRepository = UserRepository(),
    private val postRepository: PostRepository = PostRepository()
) {
    private val db = FirebaseFirestore.getInstance()
    private val adminCollection = db.collection("admin_users")
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

        val role = doc.getString("role") ?: ""

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
            "role" to role,
            "grantedBy" to grantedBy,
            "grantedAt" to FieldValue.serverTimestamp(),
            "permissions" to permissions
        )

        adminCollection.document(targetUserId).set(adminData).await()

        Log.d("AdminRepository", "Granted admin role '$role' to user $targetUserId by $grantedBy")
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
                    role = doc.getString("role") ?: "",
                    grantedBy = doc.getString("grantedBy") ?: "",
                    grantedAt = doc.getTimestamp("grantedAt"),
                    permissions = (doc.get("permissions") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList())
            }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting all admins", e)
            emptyList()
        }
    }
    suspend fun getPendingReports(): List<AdminReport> {
        return try {
            val reportsSnapshot = db.collection("reports")
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val reports = reportsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Report::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.timestamp }
            reports.map { report ->
                val post = getPostById(report.postId)
                val reportedUser = post?.let {
                    userRepository.getUser(it.userId)
                        ?: User().apply {
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
        val reportRef = db.collection("reports").document(reportId)
        val reportSnapshot = reportRef.get().await()

        val postId = reportSnapshot.getString("postId")
        val reporterId = reportSnapshot.getString("reportedBy")

        val post = getPostById(postId ?: "")
        val targetUserId = post?.userId

        if (targetUserId != null && (isAdmin(targetUserId) || isSuperAdmin(targetUserId))) {
            throw IllegalArgumentException("Cannot perform admin actions on admin users")
        }

        // Update report metadata first
        val updateData = mutableMapOf(
            "status" to if (action == AdminAction.DISMISS) "dismissed" else "reviewed",
            "reviewedBy" to adminId,
            "reviewedAt" to FieldValue.serverTimestamp(),
            "adminAction" to action.name
        ).apply {
            adminNotes?.let { this["adminNotes"] = it }
        }

        reportRef.update(updateData).await()

        // Handle action logic
        when (action) {
            AdminAction.DELETE_POST, AdminAction.BAN_USER -> {
                postId?.let { pid ->
                    val targetPost = getPostById(pid)
                    val userId = targetPost?.userId
                    when (action) {
                        AdminAction.DELETE_POST -> {
                            postRepository.deletePost(postId)
                            userId?.let { autoBanUser(it) }
                        }
                        AdminAction.BAN_USER -> {
                            userId?.let {
                                banUser(it, adminId, "Bị cấm do báo cáo: ${adminNotes ?: "Không cung cấp lý do"}")
                            }
                        }
                        else -> Unit
                    }
                }
            }
            else -> Unit
        }

        Log.d("AdminRepository", "Report $reportId reviewed by admin $adminId with action: $action")
    }


    suspend fun banUser(userId: String, adminId: String, reason: String): Result<Unit> = runCatching {
        val isAdminUser = isAdmin(adminId)
        if (!isAdminUser) {
            throw SecurityException("User $adminId does not have admin privileges to ban users")
        }

        val userRef = db.collection("users").document(userId)
        val banData = mapOf(
            "isBanned" to true,
            "bannedAt" to FieldValue.serverTimestamp(),
            "bannedBy" to adminId,
            "banReason" to reason
        )
        userRef.update(banData).await()

        val updatedUser = userRepository.getUser(userId)
        val isActuallyBanned = updatedUser?.isBanned == true
        if (!isActuallyBanned) {
            throw Exception("Ban verification failed - user state not updated correctly")
        }
        Log.d("AdminRepository", "🎉 User $userId banned successfully by admin $adminId for: $reason")
    }
    suspend fun unbanUser(userId: String): Result<Unit> = runCatching {
        val userRef = db.collection("users").document(userId)

        val unbanData = mapOf(
            "isBanned" to false,
            "bannedAt" to null,
            "bannedBy" to null,
            "banReason" to null
        )

        userRef.update(unbanData).await()
        Log.d("AdminRepository", "User $userId unbanned")
    }

    suspend fun autoBanUser(userId: String): Result<Unit> = runCatching {
        val userRef = db.collection("users").document(userId)

        val currentUser = userRepository.getUser(userId)
        val currentViolations = currentUser?.violationCount ?: 0
        val newViolationCount = currentViolations + 1

        userRef.update("violationCount", newViolationCount).await()

        if (newViolationCount >= 3 && (currentUser?.isBanned != true)) {
            banUser(userId, "system", "Tự động cấm: Quá nhiều vi phạm ($newViolationCount bài viết đã xóa)")
        }

        Log.d("AdminRepository", "User $userId violation count: $newViolationCount")
    }

    suspend fun getBannedUsers(): List<User> {
        return try {
            val bannedUsersSnapshot = db.collection("users")
                .whereEqualTo("isBanned", true)
                .get()
                .await()

            bannedUsersSnapshot.documents.mapNotNull { doc ->
                User(
                    id = doc.id,
                    username = doc.getString("username") ?: "",
                    bannedAt = doc.getTimestamp("bannedAt"),
                    bannedBy = doc.getString("bannedBy"),
                    banReason = doc.getString("banReason"),
                    violationCount = doc.getLong("violationCount")?.toInt() ?: 0,
                    warningCount = doc.getLong("warningCount")?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting banned users", e)
            emptyList()
        }
    }

    suspend fun getPostById(postId: String): Post? {
        return try {
            val postDoc = db.collection("posts").document(postId).get().await()
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

    //Gọi khởi tạo SuperAdmin
    suspend fun superAdminIfNeeded() {
        val doc = adminCollection.document(LEGACY_SUPER_ADMIN_UID).get().await()
        if (!doc.exists()) {
            val data = mapOf(
                "role" to "super_admin",
                "grantedBy" to "system_init",
                "grantedAt" to FieldValue.serverTimestamp(),
                "permissions" to listOf("all")
            )
            adminCollection.document(LEGACY_SUPER_ADMIN_UID).set(data).await()
        }
    }
    companion object {
        const val LEGACY_SUPER_ADMIN_UID = "vvrTdGbamOPz8wEkSV2kwgMJeG43"
    }

}

