package com.example.uth_socials.data.util

import com.example.uth_socials.config.AdminConfig

/**
 * Utility class để validate permissions dựa trên Firebase Security Rules
 *
 * Security Rules đã handle hầu hết validation ở server-side,
 * class này chỉ để tối ưu UX và giảm redundant API calls
 */
object SecurityValidator {

    /**
     * Check if user can modify their own profile
     * Rules: request.auth.uid == userId
     */
    fun canModifyOwnProfile(currentUserId: String?, targetUserId: String): Boolean {
        return currentUserId == targetUserId
    }

    /**
     * Check if user can modify followers (follow/unfollow)
     * Rules: request.auth.uid != null && chỉ followers field
     */
    fun canModifyFollowers(currentUserId: String?): Boolean {
        return currentUserId != null
    }

    /**
     * Check if user can create post
     * Rules: request.auth.uid != null && request.resource.data.userId == request.auth.uid
     */
    fun canCreatePost(currentUserId: String?, postUserId: String): Boolean {
        return currentUserId != null && currentUserId == postUserId
    }

    /**
     * Check if user can modify post content
     * Rules: request.auth.uid != null && resource.data.userId == request.auth.uid
     */
    fun canModifyPost(currentUserId: String?, postOwnerId: String): Boolean {
        return currentUserId != null && currentUserId == postOwnerId
    }

    /**
     * Check if user can modify post interactions (like/save)
     * Rules: request.auth.uid != null && chỉ likedBy/likes/savedBy/saveCount fields
     */
    fun canModifyPostInteractions(currentUserId: String?): Boolean {
        return currentUserId != null
    }

    /**
     * Check if user can delete post
     * Rules: request.auth.uid != null && (resource.data.userId == request.auth.uid || isAdmin())
     */
    suspend fun canDeletePost(currentUserId: String?, postOwnerId: String): Boolean {
        if (currentUserId == null) return false
        return currentUserId == postOwnerId || AdminConfig.isAdmin(currentUserId)
    }

    /**
     * Check if user can create comment
     * Rules: request.auth.uid != null && request.resource.data.userId == request.auth.uid
     */
    fun canCreateComment(currentUserId: String?, commentUserId: String): Boolean {
        return currentUserId != null && currentUserId == commentUserId
    }

    /**
     * Check if user can modify comment interactions
     * Rules: request.auth.uid != null && chỉ likedBy/likes fields
     */
    fun canModifyCommentInteractions(currentUserId: String?): Boolean {
        return currentUserId != null
    }

    /**
     * Check if user can delete comment
     * Rules: request.auth.uid != null && (chủ comment || chủ post || admin)
     */
    suspend fun canDeleteComment(
        currentUserId: String?,
        commentOwnerId: String,
        postOwnerId: String
    ): Boolean {
        if (currentUserId == null) return false
        return currentUserId == commentOwnerId ||
               currentUserId == postOwnerId ||
               AdminConfig.isAdmin(currentUserId)
    }

    /**
     * Check if user can modify categories
     * Rules: isAdmin()
     */
    suspend fun canModifyCategories(currentUserId: String?): Boolean {
        return AdminConfig.isAdmin(currentUserId ?: "")
    }

    /**
     * Check if user can read reports
     * Rules: isAdmin()
     */
    suspend fun canReadReports(currentUserId: String?): Boolean {
        return AdminConfig.isAdmin(currentUserId ?: "")
    }

    /**
     * Check if user can create report
     * Rules: request.auth.uid != null && request.resource.data.reportedBy == request.auth.uid
     */
    fun canCreateReport(currentUserId: String?, reporterId: String): Boolean {
        return currentUserId != null && currentUserId == reporterId
    }

    /**
     * Check if user can modify reports (review)
     * Rules: isAdmin()
     */
    suspend fun canModifyReports(currentUserId: String?): Boolean {
        return AdminConfig.isAdmin(currentUserId ?: "")
    }

    /**
     * Check if user can delete reports
     * Rules: isSuperAdmin()
     */
    suspend fun canDeleteReports(currentUserId: String?): Boolean {
        return AdminConfig.isSuperAdmin(currentUserId ?: "")
    }

    /**
     * Check if user can modify admin roles
     * Rules: isSuperAdmin()
     */
    suspend fun canModifyAdminRoles(currentUserId: String?): Boolean {
        return AdminConfig.isSuperAdmin(currentUserId ?: "")
    }

    /**
     * Check if admin can ban a user
     * Rules: Cannot ban yourself, cannot ban other admins/super admins
     */
    suspend fun canBanUser(adminId: String?, targetUserId: String): Boolean {
        if (adminId == null) return false

        // Cannot ban yourself
        if (adminId == targetUserId) return false

        // Cannot ban other admins/super admins (only super admin can ban regular admins)
        val targetIsAdmin = AdminConfig.isAdmin(targetUserId)
        val targetIsSuperAdmin = AdminConfig.isSuperAdmin(targetUserId)
        val adminIsSuperAdmin = AdminConfig.isSuperAdmin(adminId)

        if (targetIsSuperAdmin) return false  // Cannot ban super admin
        if (targetIsAdmin && !adminIsSuperAdmin) return false  // Only super admin can ban regular admins

        return true
    }
}
