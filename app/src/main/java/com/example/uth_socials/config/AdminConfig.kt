package com.example.uth_socials.config

import android.util.Log
import com.example.uth_socials.data.repository.AdminRepository
import kotlinx.coroutines.runBlocking

/**
 * Configuration for Admin System
 *
 * Admin management moved to Firebase for better flexibility.
 * Use AdminRepository for all admin operations.
 *
 * Migration Guide:
 * 1. Call AdminConfig.initializeSuperAdmin() once to migrate legacy super admin to Firebase
 * 2. Use AdminConfig.grantAdminRole() to add new admins
 * 3. Use AdminConfig.revokeAdminRole() to remove admin permissions
 * 4. Legacy hard-coded values are kept for backward compatibility during migration
 *
 * Firebase Structure:
 * Collection: admin_users
 * Document ID: userId
 * Fields: role, grantedBy, grantedAt, permissions
 */
object AdminConfig {
    // 🔴 LEGACY: Kept for backward compatibility - will be removed after migration
    private const val LEGACY_SUPER_ADMIN_UID = "vvrTdGbamOPz8wEkSV2kwgMJeG43"
    private val LEGACY_ADMIN_EMAILS = setOf(
        "nguyenthinhk52005@gmail.com"
    )

    private val adminRepository = AdminRepository()

    /**
     * Check if user is super admin (Firebase-based with fallback)
     */
    suspend fun isSuperAdmin(userId: String?): Boolean {
        if (userId == null) return false

        return try {
            // First check Firebase
            adminRepository.isSuperAdmin(userId) ||
            // Fallback to legacy check during migration
            userId == LEGACY_SUPER_ADMIN_UID
        } catch (e: Exception) {
            Log.e("AdminConfig", "Error checking super admin status, using legacy fallback", e)
            // Fallback to legacy check on error
            userId == LEGACY_SUPER_ADMIN_UID
        }
    }

    /**
     * Check if user is admin by email (legacy backup method)
     */
    fun isAdminByEmail(email: String?): Boolean {
        return email != null && LEGACY_ADMIN_EMAILS.contains(email)
    }

    /**
     * Check if user is admin (Firebase-based with fallback)
     */
    suspend fun isAdmin(userId: String?): Boolean {
        if (userId == null) return false

        return try {
            adminRepository.isAdmin(userId) ||
            // Fallback to email check during migration
            isAdminByEmail(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
        } catch (e: Exception) {
            Log.e("AdminConfig", "Error checking admin status, using email fallback", e)
            // Fallback to email check on error
            isAdminByEmail(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
        }
    }

    /**
     * Get current user admin status (Firebase-based with fallback)
     */
    suspend fun getCurrentUserAdminStatus(): AdminStatus {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        return when {
            isSuperAdmin(userId) -> AdminStatus.SUPER_ADMIN
            isAdmin(userId) -> AdminStatus.ADMIN
            else -> AdminStatus.USER
        }
    }

    /**
     * Get admin role for user (Firebase-based)
     */
    suspend fun getAdminRole(userId: String?): String? {
        if (userId == null) return null
        return adminRepository.getAdminRole(userId)
    }

    /**
     * Grant admin role to user (only super admin can do this)
     */
    suspend fun grantAdminRole(
        targetUserId: String,
        role: String,
        grantedBy: String,
        permissions: List<String> = emptyList()
    ): Result<Unit> {
        return adminRepository.grantAdminRole(targetUserId, role, grantedBy, permissions)
    }

    /**
     * Revoke admin role from user
     */
    suspend fun revokeAdminRole(userId: String): Result<Unit> {
        return adminRepository.revokeAdminRole(userId)
    }

    /**
     * Get all admin users (for super admin only)
     */
    suspend fun getAllAdmins() = adminRepository.getAllAdmins()

    /**
     * Initialize super admin (call this once during app setup)
     * This migrates the legacy hard-coded super admin to Firebase
     */
    suspend fun initializeSuperAdmin(): Result<Unit> {
        return adminRepository.initializeSuperAdmin(LEGACY_SUPER_ADMIN_UID)
    }

    /**
     * Check if super admin has been initialized in Firebase
     */
    suspend fun isSuperAdminInitialized(): Boolean {
        return adminRepository.isSuperAdminInitialized()
    }

    // ============ REPORT MANAGEMENT ============

    /**
     * Get all pending reports for admin review
     */
    suspend fun getPendingReports() = adminRepository.getPendingReports()

    /**
     * Review and take action on a report
     */
    suspend fun reviewReport(
        reportId: String,
        adminId: String,
        action: com.example.uth_socials.data.post.AdminAction,
        adminNotes: String? = null
    ) = adminRepository.reviewReport(reportId, adminId, action, adminNotes)

    // ============ USER MANAGEMENT ============

    /**
     * Ban a user
     */
    suspend fun banUser(userId: String, adminId: String, reason: String) =
        adminRepository.banUser(userId, adminId, reason)

    /**
     * Unban a user
     */
    suspend fun unbanUser(userId: String) = adminRepository.unbanUser(userId)

    /**
     * Get all banned users
     */
    suspend fun getBannedUsers() = adminRepository.getBannedUsers()

    /**
     * LEGACY METHOD - Use getCurrentUserAdminStatus() instead
     * Kept for backward compatibility during migration
     */
    @Deprecated("Use getCurrentUserAdminStatus() instead", ReplaceWith("getCurrentUserAdminStatus()"))
    fun getCurrentUserAdminStatusSync(): AdminStatus {
        return runBlocking {
            getCurrentUserAdminStatus()
        }
    }
}

/**
 * Admin status enum
 */
enum class AdminStatus {
    USER,       // Regular user
    ADMIN,      // Regular admin
    SUPER_ADMIN // Super admin with all permissions
}
