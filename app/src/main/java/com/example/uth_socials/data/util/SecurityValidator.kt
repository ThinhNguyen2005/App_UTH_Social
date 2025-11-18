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
     * Kiểm tra xem người dùng có thể sửa đổi người theo dõi (theo dõi/bỏ theo dõi) hay không
     * Quy tắc: request.auth.uid != null && chỉ trường người theo dõi
     */
    fun canModifyFollowers(currentUserId: String?): Boolean {
        return currentUserId != null
    }

    /**
     * Kiểm tra xem người dùng có thể sửa đổi tương tác bài đăng (thích/lưu) hay không
     * Quy tắc: request.auth.uid != null && chỉ các trường likedBy/likes/savedBy/saveCount
     */
    fun checkCurrentUserId(currentUserId: String?): Boolean {
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
     * Kiểm tra xem người dùng có thể sửa đổi danh mục hay không
     * Quy tắc: isAdmin()
     */
    suspend fun canModifyCategories(currentUserId: String?): Boolean {
        return adminRepository.isAdmin(currentUserId ?: "")
    }

    /**
     * Kiểm tra xem người dùng có thể tạo báo cáo hay không
     * Quy tắc: request.auth.uid != null && request.resource.data.reportedBy == request.auth.uid
     * 
     * Note: Server-side (firestore.rules) sẽ xác thực thêm bất kỳ quy tắc nào về nội dung
     * Client-side chỉ kiểm tra authentication & reporterId
     */
    fun canCreateReport(currentUserId: String?, reporterId: String): Boolean {
        return currentUserId != null && currentUserId == reporterId
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
}

