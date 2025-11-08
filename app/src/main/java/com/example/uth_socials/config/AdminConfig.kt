package com.example.uth_socials.config

import com.example.uth_socials.data.repository.AdminRepository

/**
 * Configuration constants for Admin System
 *
 * This class now only contains constants. All business logic has been moved to AdminRepository.
 * Use AdminRepository for all admin operations.
 *
 * Firebase Structure:
 * Collection: admin_users
 * Document ID: userId
 * Fields: role, grantedBy, grantedAt, permissions
 */
object AdminConfig {
    private val adminRepository = AdminRepository()

    // ============ DELEGATE METHODS TO ADMIN REPOSITORY ============

    /**
     * Check if user is super admin (delegates to AdminRepository)
     */
    suspend fun isSuperAdmin(userId: String?): Boolean {
        if (userId == null) return false

        return try {
            // First check Firebase
            adminRepository.isSuperAdmin(userId) ||
            // Fallback to legacy check during migration
            userId == AdminRepository.LEGACY_SUPER_ADMIN_UID
        } catch (e: Exception) {
            // Fallback to legacy check on error
            userId == AdminRepository.LEGACY_SUPER_ADMIN_UID
        }
    }

    /**
     * Check if user is admin (delegates to AdminRepository)
     */
    suspend fun isAdmin(userId: String?): Boolean {
        if (userId == null) return false

        return try {
            adminRepository.isAdmin(userId ?: "")
        } catch (e: Exception) {
            // Fallback to email check during migration (kept for compatibility)
            val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
            email != null && AdminRepository.LEGACY_ADMIN_EMAILS.contains(email)
        }
    }

    /**
     * Get current user admin status (delegates to AdminRepository)
     */
    suspend fun getCurrentUserAdminStatus(): AdminStatus {
        return adminRepository.getCurrentUserAdminStatus()
    }

    /**
     * Grant admin role to user (delegates to AdminRepository)
     */
    suspend fun grantAdminRole(
        targetUserId: String,
        role: String,
        grantedBy: String,
        permissions: List<String> = emptyList()
    ): Result<Int> {
        return adminRepository.grantAdminRole(targetUserId, role, grantedBy, permissions)
    }

    /**
     * Revoke admin role from user (delegates to AdminRepository)
     */
    suspend fun revokeAdminRole(userId: String): Result<Unit> {
        return adminRepository.revokeAdminRole(userId)
    }

    /**
     * Get all admin users (delegates to AdminRepository)
     */
    suspend fun getAllAdmins() = adminRepository.getAllAdmins()

    /**
     * Initialize super admin (delegates to AdminRepository)
     */
    suspend fun initializeSuperAdmin(): Result<Unit> {
        return adminRepository.initializeSuperAdmin(AdminRepository.LEGACY_SUPER_ADMIN_UID)
    }

    // ============ REPORT MANAGEMENT (delegates to AdminRepository) ============

    suspend fun getPendingReports() = adminRepository.getPendingReports()

    suspend fun reviewReport(
        reportId: String,
        adminId: String,
        action: com.example.uth_socials.data.post.AdminAction,
        adminNotes: String? = null
    ) = adminRepository.reviewReport(reportId, adminId, action, adminNotes)

    // ============ USER MANAGEMENT (delegates to AdminRepository) ============

    suspend fun banUser(userId: String, adminId: String, reason: String) =
        adminRepository.banUser(userId, adminId, reason)

    suspend fun unbanUser(userId: String) = adminRepository.unbanUser(userId)

    suspend fun getBannedUsers() = adminRepository.getBannedUsers()

    /**
     * Get admin role for user (delegates to AdminRepository)
     */
    suspend fun getAdminRole(userId: String?): String? {
        if (userId == null) return null
        return adminRepository.getAdminRole(userId)
    }

    /**
     * Check if super admin has been initialized (delegates to AdminRepository)
     */
    suspend fun isSuperAdminInitialized(): Boolean {
        return adminRepository.isSuperAdminInitialized()
    }

    /**
     * Check if user is any kind of admin (regular admin or super admin)
     */
    suspend fun isAnyAdmin(userId: String?): Boolean {
        if (userId == null) return false
        return isAdmin(userId) || isSuperAdmin(userId)
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
