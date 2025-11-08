package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.post.*
import com.example.uth_socials.data.user.AdminUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing admin users
 */
class AdminRepository {
    private val db = FirebaseFirestore.getInstance()
    private val adminCollection = db.collection("admin_users")

    /**
     * Check if user is admin (super admin or firestore admin)
     */
    suspend fun isAdmin(userId: String): Boolean {
        return try {
            adminCollection.document(userId).get().await().exists()
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error checking admin status", e)
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
     * Get admin user details
     */
    suspend fun getAdminUser(userId: String): AdminUser? {
        return try {
            val doc = adminCollection.document(userId).get().await()
            if (doc.exists()) {
                AdminUser(
                    userId = doc.id,
                    role = doc.getString("role") ?: "",
                    grantedBy = doc.getString("grantedBy") ?: "",
                    grantedAt = doc.getTimestamp("grantedAt"),
                    permissions = doc.get("permissions") as? List<String> ?: emptyList()
                )
            } else null
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting admin user", e)
            null
        }
    }

    /**
     * Grant admin role to user (only super admin can do this)
     */
    suspend fun grantAdminRole(
        targetUserId: String,
        role: String,
        grantedBy: String,
        permissions: List<String> = emptyList()
    ): Result<Unit> = runCatching {
        val adminData = mapOf(
            "role" to role,
            "grantedBy" to grantedBy,
            "grantedAt" to FieldValue.serverTimestamp(),
            "permissions" to permissions
        )

        adminCollection.document(targetUserId).set(adminData).await()
        Log.d("AdminRepository", "Granted admin role '$role' to user $targetUserId by $grantedBy")
    }

    /**
     * Revoke admin role from user
     */
    suspend fun revokeAdminRole(userId: String): Result<Unit> = runCatching {
        adminCollection.document(userId).delete().await()
        Log.d("AdminRepository", "Revoked admin role from user $userId")
    }

    /**
     * Update admin permissions
     */
    suspend fun updateAdminPermissions(userId: String, permissions: List<String>): Result<Unit> = runCatching {
        adminCollection.document(userId).update("permissions", permissions).await()
        Log.d("AdminRepository", "Updated permissions for admin $userId")
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
                    permissions = doc.get("permissions") as? List<String> ?: emptyList()
                )
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
                val reporter = getUserById(report.reportedBy)
                val reportedUser = post?.let { getUserById(it.userId) }

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

        val updateData = mutableMapOf<String, Any>(
            "status" to "reviewed",
            "reviewedBy" to adminId,
            "reviewedAt" to FieldValue.serverTimestamp(),
            "adminAction" to action.name
        )

        adminNotes?.let { updateData["adminNotes"] = it }

        // Update report status
        reportRef.update(updateData).await()

        // Execute the admin action
        when (action) {
            AdminAction.DELETE_POST -> {
                // Get post ID from report and delete it
                val report = reportRef.get().await()
                val postId = report.getString("postId")
                postId?.let {
                    deletePost(it)
                    // Get post author and increment violation count
                    val post = getPostById(it)
                    post?.userId?.let { userId ->
                        incrementUserViolation(userId)
                    }
                }
            }
            AdminAction.BAN_USER -> {
                // Get reported user from post and ban them
                val report = reportRef.get().await()
                val postId = report.getString("postId")
                postId?.let { postId ->
                    val post = getPostById(postId)
                    post?.userId?.let { userId ->
                        banUser(userId, adminId, "Banned due to report: ${adminNotes ?: "No reason provided"}")
                    }
                }
            }
            AdminAction.BAN_REPORTER -> {
                // Ban the reporter for making invalid reports
                val report = reportRef.get().await()
                val reporterId = report.getString("reportedBy")
                reporterId?.let { userId ->
                    banUser(userId, adminId, "Banned for invalid reports: ${adminNotes ?: "No reason provided"}")
                }
            }
            AdminAction.WARN_USER -> {
                // Get reported user from post and warn them
                val report = reportRef.get().await()
                val postId = report.getString("postId")
                postId?.let { postId ->
                    val post = getPostById(postId)
                    post?.userId?.let { userId ->
                        incrementUserWarning(userId)
                    }
                }
            }
            AdminAction.DISMISS -> {
                // Just mark as dismissed, no further action
                reportRef.update("status", "dismissed").await()
            }
            AdminAction.NONE -> {
                // No action, just mark as reviewed
            }
        }

        Log.d("AdminRepository", "Report $reportId reviewed by admin $adminId with action: $action")
    }

    // ============ USER MANAGEMENT ============

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                User(
                    id = userDoc.id,
                    username = userDoc.getString("username") ?: "",
                    email = userDoc.getString("email") ?: "",
                    avatarUrl = userDoc.getString("avatarUrl"),
                    isBanned = userDoc.getBoolean("isBanned") ?: false,
                    bannedAt = userDoc.getTimestamp("bannedAt"),
                    bannedBy = userDoc.getString("bannedBy"),
                    banReason = userDoc.getString("banReason"),
                    violationCount = userDoc.getLong("violationCount")?.toInt() ?: 0,
                    warningCount = userDoc.getLong("warningCount")?.toInt() ?: 0
                )
            } else null
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error getting user $userId", e)
            null
        }
    }

    /**
     * Ban a user
     */
    suspend fun banUser(userId: String, adminId: String, reason: String): Result<Unit> = runCatching {
        val userRef = db.collection("users").document(userId)

        val banData = mapOf(
            "isBanned" to true,
            "bannedAt" to FieldValue.serverTimestamp(),
            "bannedBy" to adminId,
            "banReason" to reason
        )

        userRef.update(banData).await()
        Log.d("AdminRepository", "User $userId banned by admin $adminId for: $reason")
    }

    /**
     * Increment user violation count
     */
    suspend fun incrementUserViolation(userId: String): Result<Unit> = runCatching {
        val userRef = db.collection("users").document(userId)

        // Get current violation count
        val currentUser = getUserById(userId)
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
        val currentUser = getUserById(userId)
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
                    email = doc.getString("email") ?: "",
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
     * Get post by ID
     */
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

    /**
     * Delete post by ID (admin action)
     */
    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        db.collection("posts").document(postId).delete().await()
        Log.d("AdminRepository", "Post $postId deleted by admin")
    }
}
