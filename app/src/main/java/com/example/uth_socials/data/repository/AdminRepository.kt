package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.post.AdminAction
import com.example.uth_socials.data.post.AdminReport
import com.example.uth_socials.data.post.Report
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.user.AdminUser
import com.example.uth_socials.data.user.User
import com.example.uth_socials.data.user.User as UserEntity
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
        return try {
            val adminDoc = adminCollection.document(userId).get().await()
            val exists = adminDoc.exists()
            Log.d("AdminRepository", "isAdmin check: userId=$userId, adminDocExists=$exists")
            exists
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error checking admin status for $userId", e)
            false
        }
    }

    /**
     * Get admin role of user
     */
    suspend fun getAdminRole(userId: String): String? {
        return try {
            adminCollection.document(userId).get().await()
                .getString("role")
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting admin role", e)
            null
        }
    }

    /**
     * Check if user is super admin
     */
    suspend fun isSuperAdmin(userId: String): Boolean {
        return getAdminRole(userId) == "super_admin"
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
        // Kiểm tra đầu vào cơ bản
        require(targetUserId.isNotBlank()) { "Target user ID cannot be blank" }
        require(grantedBy.isNotBlank()) { "GrantedBy cannot be blank" }

        // Kiểm tra người được cấp quyền có tồn tại (nếu có collection users)
        val userExists = db.collection("users").document(targetUserId).get().await().exists()
        if (!userExists) throw IllegalArgumentException("User with ID $targetUserId does not exist")

        // Dữ liệu admin
        val adminData = mapOf(
            "role" to role,
            "grantedBy" to grantedBy,
            "grantedAt" to FieldValue.serverTimestamp(),
            "permissions" to permissions
        )

        // Cập nhật hoặc tạo mới admin document
        adminCollection.document(targetUserId).set(adminData).await()

        Log.d("AdminRepository", "Granted admin role '$role' to user $targetUserId by $grantedBy")
    }.onFailure { e ->
        Log.e("AdminRepository", "Failed to grant admin role to $targetUserId", e)
    }


    /**
     * Revoke admin role from user
     */
    suspend fun revokeAdminRole(userId: String): Result<Unit> = runCatching {
        adminCollection.document(userId).delete().await()
        Log.d("AdminRepository", "Revoked admin role from user $userId")
    }
    /**
     * Get all admin users (for super admin only)
     */
    suspend fun getAllAdmins(): List<AdminUser> {
        return try {
            adminCollection.get().await().documents.mapNotNull { doc ->
                AdminUser(
                    userId = doc.id,
                    role = doc.getString("role") ?: "",
                    grantedBy = doc.getString("grantedBy") ?: "",
                    grantedAt = doc.getTimestamp("grantedAt"),
                    permissions = (doc.get("permissions") as? List<*>)
                        ?.filterIsInstance<String>()  // Chỉ lấy phần tử là String
                        ?: emptyList()                )
            }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting all admins", e)
            emptyList()
        }
    }

    /**
     * Initialize super admin (call this once during app setup)
     * This should be called by the developer to set up the initial super admin
     */
    suspend fun initializeSuperAdmin(superAdminUserId: String): Result<Unit> = runCatching {
        val superAdminData = mapOf(
            "role" to "super_admin",
            "grantedBy" to "system",
            "grantedAt" to FieldValue.serverTimestamp(),
            "permissions" to listOf("all") // Super admin has all permissions
        )

        adminCollection.document(superAdminUserId).set(superAdminData).await()
        Log.d("AdminRepository", "Initialized super admin: $superAdminUserId")
    }

    /**
     * Check if super admin has been initialized
     */
    suspend fun isSuperAdminInitialized(): Boolean {
        return try {
            val admins = getAllAdmins()
            admins.any { it.role == "super_admin" }
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error checking super admin initialization", e)
            false
        }
    }

    // ============ ADMIN REPORT MANAGEMENT ============

    /**
     * Get all pending reports for admin review
     */
    suspend fun getPendingReports(): List<AdminReport> {
        return try {
            // Get all pending reports
            val reportsSnapshot = db.collection("reports")
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val reports = reportsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Report::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.timestamp }

            // Enrich reports with post and user data
            reports.map { report ->
                val post = getPostById(report.postId)
                
                // ✅ FIX: Lấy reporter user data, fallback với ID nếu user không tồn tại
                val reporter = userRepository.getUser(report.reportedBy)?.toPostUser()
                    ?: User().apply {
                        username = "[Deleted User]"
                        id = report.reportedBy
                    }

                // ✅ FIX: Lấy reported user data, fallback nếu post bị xóa hoặc user không tồn tại
                val reportedUser = post?.let {
                    userRepository.getUser(it.userId)?.toPostUser()
                        ?: User().apply {
                            username = "[Deleted User]"
                            id = it.userId
                        }
                }

                AdminReport(
                    report = report,
                    post = post,
                    reporter = reporter,
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

        // Get report data once
        val reportSnapshot = reportRef.get().await()
        val postId = reportSnapshot.getString("postId")
        val reporterId = reportSnapshot.getString("reportedBy")

        // ✅ VALIDATION: Check if target user is admin (cannot ban/report admin)
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
            AdminAction.DELETE_POST, AdminAction.BAN_USER, AdminAction.WARN_USER -> {
                postId?.let { pid ->
                    val post = getPostById(pid)
                    val userId = post?.userId
                    when (action) {
                        AdminAction.DELETE_POST -> {
                            deletePost(pid)
                            userId?.let { incrementUserViolation(it) }
                        }
                        AdminAction.BAN_USER -> {
                            userId?.let {
                                banUser(it, adminId, "Banned due to report: ${adminNotes ?: "No reason provided"}")
                            }
                        }
                        AdminAction.WARN_USER -> {
                            userId?.let { incrementUserWarning(it) }
                        }
                        else -> Unit
                    }
                }
            }

            AdminAction.BAN_REPORTER -> {
                reporterId?.let {
                    banUser(it, adminId, "Banned for invalid reports: ${adminNotes ?: "No reason provided"}")
                }
            }

            AdminAction.DISMISS, AdminAction.NONE -> Unit
        }

        Log.d("AdminRepository", "Report $reportId reviewed by admin $adminId with action: $action")
    }


    // ============ USER MANAGEMENT ============

    /**
     * Ban a user (delegates to UserRepository for consistency)
     */
    suspend fun banUser(userId: String, adminId: String, reason: String): Result<Unit> = runCatching {
        Log.d("AdminRepository", "🔄 Starting ban process: userId=$userId, adminId=$adminId, reason=$reason")

        // Check if admin has permission
        val isAdminUser = isAdmin(adminId)
        Log.d("AdminRepository", "Admin permission check: adminId=$adminId, isAdmin=$isAdminUser")

        if (!isAdminUser) {
            throw SecurityException("User $adminId does not have admin privileges to ban users")
        }

        val userRef = db.collection("users").document(userId)
        Log.d("AdminRepository", "Updating user document: $userId")

        val banData = mapOf(
            "isBanned" to true,
            "bannedAt" to FieldValue.serverTimestamp(),
            "bannedBy" to adminId,
            "banReason" to reason
        )

        Log.d("AdminRepository", "Ban data: $banData")
        userRef.update(banData).await()

        // ✅ Verify the update was successful
        val updatedUser = userRepository.getUser(userId)
        val isActuallyBanned = updatedUser?.isBanned == true
        Log.d("AdminRepository", "✅ Ban completed. Verification: userId=$userId, isBanned=$isActuallyBanned")

        if (!isActuallyBanned) {
            Log.e("AdminRepository", "❌ BAN VERIFICATION FAILED: User $userId should be banned but isBanned=$isActuallyBanned")
            throw Exception("Ban verification failed - user state not updated correctly")
        }

        Log.d("AdminRepository", "🎉 User $userId banned successfully by admin $adminId for: $reason")
    }

    /**
     * Increment user violation count
     */
    suspend fun incrementUserViolation(userId: String): Result<Unit> = runCatching {
        val userRef = db.collection("users").document(userId)

        // Get current violation count
        val currentUser = userRepository.getUser(userId)
        val currentViolations = currentUser?.violationCount ?: 0
        val newViolationCount = currentViolations + 1

        // Update violation count
        userRef.update("violationCount", newViolationCount).await()

        // Auto-ban if violations >= 3
        if (newViolationCount >= 3 && (currentUser?.isBanned != true)) {
            banUser(userId, "system", "Automatic ban: Too many violations ($newViolationCount posts deleted)")
        }

        Log.d("AdminRepository", "User $userId violation count: $newViolationCount")
    }

    /**
     * Increment user warning count
     */
    suspend fun incrementUserWarning(userId: String): Result<Unit> = runCatching {
        val userRef = db.collection("users").document(userId)

        // Get current warning count
        val currentUser = userRepository.getUser(userId)
        val newWarningCount = (currentUser?.warningCount ?: 0) + 1

        // Update warning count
        userRef.update("warningCount", newWarningCount).await()

        Log.d("AdminRepository", "User $userId warning count: $newWarningCount")
    }

    /**
     * Unban a user
     */
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

    /**
     * Get all banned users
     */
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
                    avatarUrl = doc.getString("avatarUrl"),
                    isBanned = true,
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

    // ============ POST MANAGEMENT ============

    /**
     * Get post by ID (delegates to PostRepository)
     */
    suspend fun getPostById(postId: String): Post? {
        // Use PostRepository for consistency and to avoid code duplication
        return try {
            // Note: Assuming PostRepository has getPostById method
            // If not, we'll need to add it or keep this implementation
            val postDoc = db.collection("posts").document(postId).get().await()
            if (postDoc.exists()) {
                postDoc.toObject(Post::class.java)?.copy(id = postDoc.id ?: postId)
            } else null
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting post $postId", e)
            null
        }
    }

    /**
     * Delete post by ID (admin action) - delegates to PostRepository
     */
    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        // Use PostRepository's deletePost method for consistency
        val success = postRepository.deletePost(postId)
        if (success) {
            Log.d("AdminRepository", "Post $postId deleted by admin")
        } else {
            throw Exception("Failed to delete post")
        }
    }

    // ============ HELPER FUNCTIONS ============

    /**
     * Convert UserEntity to User (for AdminReport compatibility)
     */
    private fun UserEntity.toPostUser(): User {
        return User().apply {
            username = this@toPostUser.username
            avatarUrl = this@toPostUser.avatarUrl
            id = this@toPostUser.id
            isBanned = this@toPostUser.isBanned
            bannedAt = this@toPostUser.bannedAt
            bannedBy = this@toPostUser.bannedBy
            banReason = this@toPostUser.banReason
            violationCount = this@toPostUser.violationCount
            warningCount = this@toPostUser.warningCount
        }
    }

    // ============ ADMIN STATUS MANAGEMENT ============

    /**
     * Get current user admin status
     */
    suspend fun getCurrentUserAdminStatus(): AdminStatus {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        return when {
            isSuperAdmin(userId ?: "") -> AdminStatus.SUPER_ADMIN
            isAdmin(userId ?: "") -> AdminStatus.ADMIN
            else -> AdminStatus.USER
        }
    }

    /**
     * Check if current user is super admin
     */
    suspend fun isCurrentUserSuperAdmin(): Boolean {
        val userId = auth.currentUser?.uid
        return isSuperAdmin(userId ?: "")
    }

    /**
     * Check if current user is admin
     */
    suspend fun isCurrentUserAdmin(): Boolean {
        val userId = auth.currentUser?.uid
        return isAdmin(userId ?: "")
    }
    suspend fun initializeLegacySuperAdmin(): Result<Unit> = runCatching {
        Log.w("AdminRepository", "EMERGENCY: Initializing legacy super admin")

        // Double-check: Chỉ tạo nếu thực sự không có super admin
        if (isSuperAdminInitialized()) {
            Log.d("AdminRepository", "Super admin already exists, skipping legacy initialization")
            return@runCatching
        }

        // Tạo super admin từ legacy UID
        val superAdminData = mapOf(
            "role" to "super_admin",
            "grantedBy" to "legacy_system_emergency",
            "grantedAt" to FieldValue.serverTimestamp(),
            "permissions" to listOf("all"),
            "isLegacy" to true, // Mark as legacy admin
            "legacyUid" to LEGACY_SUPER_ADMIN_UID
        )

        adminCollection.document(LEGACY_SUPER_ADMIN_UID).set(superAdminData).await()

        Log.d("AdminRepository", "Legacy super admin initialized: $LEGACY_SUPER_ADMIN_UID")



    }.onFailure { e ->
        Log.e("AdminRepository", "Failed to initialize legacy super admin", e)
    }
    companion object {
        const val LEGACY_SUPER_ADMIN_UID = "vvrTdGbamOPz8wEkSV2kwgMJeG43"
    }
    enum class AdminStatus {
    USER,       // Regular user
    ADMIN,      // Regular admin
    SUPER_ADMIN // Super admin with all permissions
}
}
