package com.example.uth_socials.data.util

import com.example.uth_socials.data.repository.AdminRepository


/**
 * Lớp tiện ích để xác thực quyền dựa trên Quy tắc bảo mật Firebase
 *
 * Quy tắc bảo mật đã xử lý việc xác thực hầu hết ở phía máy chủ,
 * lớp này chỉ để tối ưu hóa UX và giảm các lệnh gọi API dư thừa
 */
object SecurityValidator {

    private val adminRepository = AdminRepository()

    // Cache admin status để tránh gọi API nhiều lần
    private val adminCache = mutableMapOf<String, Triple<Boolean, Boolean, Long>>() // userId -> (isAdmin, isSuperAdmin, timestamp)

    private fun isCacheValid(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp < 300000 // 5 phút cache
    }

    /**
     * Kiểm tra xem người dùng có thể sửa đổi hồ sơ của mình hay không
     * Quy tắc: request.auth.uid == userId
     */
    fun canModifyOwnProfile(currentUserId: String?, targetUserId: String): Boolean {
        return currentUserId == targetUserId
    }

    /**
     * Kiểm tra xem người dùng có thể sửa đổi người theo dõi (theo dõi/bỏ theo dõi) hay không
     * Quy tắc: request.auth.uid != null && chỉ trường người theo dõi
     */
    fun canModifyFollowers(currentUserId: String?): Boolean {
        return currentUserId != null
    }

    /**
     * Kiểm tra xem người dùng có thể tạo bài đăng hay không
     * Quy tắc: request.auth.uid != null && request.resource.data.userId == request.auth.uid
     */
    fun canCreatePost(currentUserId: String?, postUserId: String): Boolean {
        return currentUserId != null && currentUserId == postUserId
    }

    /**
     * Kiểm tra xem người dùng có thể sửa đổi nội dung bài đăng hay không
     * Quy tắc: request.auth.uid != null && resource.data.userId == request.auth.uid
     */
    fun canModifyPost(currentUserId: String?, postOwnerId: String): Boolean {
        return currentUserId != null && currentUserId == postOwnerId
    }

    /**
     * Kiểm tra xem người dùng có thể sửa đổi tương tác bài đăng (thích/lưu) hay không
     * Quy tắc: request.auth.uid != null && chỉ các trường likedBy/likes/savedBy/saveCount
     */
    fun canModifyPostInteractions(currentUserId: String?): Boolean {
        return currentUserId != null
    }

    /**
     * Kiểm tra xem người dùng có thể xóa bài đăng hay không
     * Quy tắc: request.auth.uid != null && (resource.data.userId == request.auth.uid || isAdmin())
     */
    suspend fun canDeletePost(currentUserId: String?, postOwnerId: String): Boolean {
        if (currentUserId == null) return false
        return currentUserId == postOwnerId || adminRepository.isAdmin(currentUserId)
    }

    /**
     * Kiểm tra xem người dùng có thể tạo bình luận hay không
     * Quy tắc: request.auth.uid != null && request.resource.data.userId == request.auth.uid
     */
    fun canCreateComment(currentUserId: String?, commentUserId: String): Boolean {
        return currentUserId != null && currentUserId == commentUserId
    }

    /**
     * Kiểm tra xem người dùng có thể sửa đổi tương tác bình luận hay không
     * Quy tắc: request.auth.uid != null && chỉ các trường likedBy/likes
     */
    fun canModifyCommentInteractions(currentUserId: String?): Boolean {
        return currentUserId != null
    }

    /**
     * Kiểm tra xem người dùng có thể xóa bình luận hay không
     * Quy tắc: request.auth.uid != null && (chủ bình luận || bài viết chủ || quản trị viên)
     */
    suspend fun canDeleteComment(
        currentUserId: String?,
        commentOwnerId: String,
        postOwnerId: String
    ): Boolean {
        if (currentUserId == null) return false
        return currentUserId == commentOwnerId ||
               currentUserId == postOwnerId ||
                adminRepository.isAdmin(currentUserId)
    }

    /**
     * Kiểm tra xem người dùng có thể sửa đổi danh mục hay không
     * Quy tắc: isAdmin()
     */
    suspend fun canModifyCategories(currentUserId: String?): Boolean {
        return adminRepository.isAdmin(currentUserId ?: "")
    }

    /**
     * Kiểm tra xem người dùng có thể đọc báo cáo hay không
     * Quy tắc: isAdmin()
     */
    suspend fun canReadReports(currentUserId: String?): Boolean {
        return adminRepository.isAdmin(currentUserId ?: "")
    }

    /**
     * Kiểm tra xem người dùng có thể tạo báo cáo hay không
     * Quy tắc: request.auth.uid != null && request.resource.data.reportedBy == request.auth.uid
     * Bổ sung: Không thể báo cáo người dùng quản trị
     */
    suspend fun canCreateReport(currentUserId: String?, reporterId: String): Boolean {
        if (currentUserId == null || currentUserId != reporterId) return false

        // Cannot report admin users
        return !adminRepository.isAdmin(reporterId) && !adminRepository.isSuperAdmin(reporterId)
    }

    /**
     * Kiểm tra xem người dùng có thể sửa đổi báo cáo hay không (xem lại)
     * Quy tắc: isAdmin()
     */
    suspend fun canModifyReports(currentUserId: String?): Boolean {
        return adminRepository.isAdmin(currentUserId ?: "")
    }

    /**
     * Check if user can delete reports
     * Rules: isSuperAdmin()
     */
    suspend fun canDeleteReports(currentUserId: String?): Boolean {
        return adminRepository.isSuperAdmin(currentUserId ?: "")
    }

    /**
     * Check if user can modify admin roles
     * Rules: isSuperAdmin()
     */
    suspend fun canModifyAdminRoles(currentUserId: String?): Boolean {
        return adminRepository.isSuperAdmin(currentUserId ?: "")
    }

    // ===== CÁC HÀM MỚI CHO COMMENTS =====

    /**
     * Validate comment content
     */
    fun isValidCommentContent(content: String): Boolean {
        return content.trim().isNotEmpty() &&
               content.length <= 500 &&
               !containsProfanity(content)
    }

    /**
     * Check for basic profanity (có thể mở rộng)
     */
    private fun containsProfanity(content: String): Boolean {
        val badWords = listOf("spam", "test", "dummy") // Mở rộng theo nhu cầu
        return badWords.any { content.lowercase().contains(it) }
    }

    /**
     * Rate limiting cho comments (client-side check)
     * Rules: Không thể implement trong Firestore Rules
     */
    fun canCommentBasedOnRateLimit(lastCommentTime: Long?): Boolean {
        if (lastCommentTime == null) return true
        val timeSinceLastComment = System.currentTimeMillis() - lastCommentTime
        return timeSinceLastComment >= 30000 // 30 giây giữa comments
    }

    // ===== CACHING FUNCTIONS =====

    /**
     * Get cached admin status
     */
    suspend fun getCachedAdminStatus(userId: String): Pair<Boolean, Boolean> { // (isAdmin, isSuperAdmin)
        val cache = adminCache[userId]
        if (cache != null && isCacheValid(cache.third)) {
            return Pair(cache.first, cache.second)
        }

        // Fetch fresh data
        val isAdmin = adminRepository.isAdmin(userId)
        val isSuperAdmin = adminRepository.isSuperAdmin(userId)
        val timestamp = System.currentTimeMillis()

        adminCache[userId] = Triple(isAdmin, isSuperAdmin, timestamp)
        return Pair(isAdmin, isSuperAdmin)
    }

    /**
     * Clear cache (gọi khi logout hoặc cần fresh data)
     */
    fun clearCache() {
        adminCache.clear()
    }

    /**
     * Validate user input data
     */
    fun isValidUserInput(input: String, maxLength: Int = 100): Boolean {
        return input.trim().length in 1..maxLength &&
               !input.contains("<script") && // Basic XSS protection
               !input.contains("javascript:")
    }
}

