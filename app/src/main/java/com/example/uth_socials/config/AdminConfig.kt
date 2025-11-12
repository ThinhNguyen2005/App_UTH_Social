//package com.example.uth_socials.config
//import com.google.firebase.auth.FirebaseAuth
//import com.example.uth_socials.data.repository.AdminRepository
//
///**
// * Configuration constants for Admin System
// *
// * Lớp này hiện chỉ chứa hằng số. Toàn bộ logic nghiệp vụ đã được chuyển sang AdminRepository.
// * Sử dụng AdminRepository cho tất cả các hoạt động quản trị.
// *
// * Firebase Structure:
// * Collection: admin_users
// * Document ID: userId
// * Fields: role, grantedBy, grantedAt, permissions
// */
//object AdminConfig {
//    private val adminRepository = AdminRepository()
//
//    // ============ DELEGATE METHODS TO ADMIN REPOSITORY ============
//
//    /**
//     * Kiểm tra xem người dùng có phải là quản trị viên cấp cao hay không (ủy quyền cho AdminRepository)
//     */
//    suspend fun isSuperAdmin(userId: String?): Boolean {
//        if (userId == null) return false
//
//        return try {
//            // First check Firebase
//            adminRepository.isSuperAdmin(userId) ||
//            // Fallback to legacy check during migration
//            userId == AdminRepository.LEGACY_SUPER_ADMIN_UID
//        } catch (_: Exception) {
//            // Fallback to legacy check on error
//            userId == AdminRepository.LEGACY_SUPER_ADMIN_UID
//        }
//    }
//
//    /**
//     * Kiểm tra xem người dùng có phải là quản trị viên không (ủy quyền cho AdminRepository)
//     */
//    suspend fun isAdmin(userId: String?): Boolean {
//        if (userId == null) return false
//
//        return try {
//            adminRepository.isAdmin(userId)
//        } catch (_: Exception) {
//            val email = FirebaseAuth.getInstance().currentUser?.email
//            email != null && AdminRepository.LEGACY_ADMIN_EMAILS.contains(email)
//        }
//    }
//
//    /**
//     * Nhận trạng thái quản trị viên người dùng hiện tại (ủy quyền cho AdminRepository)
//     * */
//    suspend fun getCurrentUserAdminStatus(): AdminStatus {
//        return adminRepository.getCurrentUserAdminStatus()
//    }
//
//    /**
//     * Cấp quyền quản trị cho người dùng (ủy quyền cho AdminRepository)
//     */
//    suspend fun grantAdminRole(
//        targetUserId: String,
//        role: String,
//        grantedBy: String,
//        permissions: List<String> = emptyList()
//    ): Result<Int> {
//        return adminRepository.grantAdminRole(targetUserId, role, grantedBy, permissions)
//    }
//
//    /**
//     * Thu hồi vai trò quản trị viên từ người dùng (ủy quyền cho AdminRepository)
//     */
//    suspend fun revokeAdminRole(userId: String): Result<Unit> {
//        return adminRepository.revokeAdminRole(userId)
//    }
//
//    /**
//     * Get all admin users (delegates to AdminRepository)
//     */
//    suspend fun getAllAdmins() = adminRepository.getAllAdmins()
//
//    /**
//     * Khởi tạo siêu quản trị viên (ủy quyền cho AdminRepository)
//     */
//    suspend fun initializeSuperAdmin(): Result<Unit> {
//        return adminRepository.initializeSuperAdmin(AdminRepository.LEGACY_SUPER_ADMIN_UID)
//    }
//
//    // ============ REPORT MANAGEMENT (delegates to AdminRepository) ============
//
//    suspend fun getPendingReports() = adminRepository.getPendingReports()
//
//    suspend fun reviewReport(
//        reportId: String,
//        adminId: String,
//        action: com.example.uth_socials.data.post.AdminAction,
//        adminNotes: String? = null
//    ) = adminRepository.reviewReport(reportId, adminId, action, adminNotes)
//
//    // ============ USER MANAGEMENT (delegates to AdminRepository) ============
//
//    suspend fun banUser(userId: String, adminId: String, reason: String) =
//        adminRepository.banUser(userId, adminId, reason)
//
//    suspend fun unbanUser(userId: String) = adminRepository.unbanUser(userId)
//
//    suspend fun getBannedUsers() = adminRepository.getBannedUsers()
//
//    /**
//     * Lấy quyền quản trị cho người dùng (ủy quyền cho AdminRepository)
//     */
//    suspend fun getAdminRole(userId: String?): String? {
//        if (userId == null) return null
//        return adminRepository.getAdminRole(userId)
//    }
//
//    /**
//     * Kiểm tra xem siêu quản trị viên đã được khởi tạo chưa (ủy quyền cho AdminRepository)
//     */
//    suspend fun isSuperAdminInitialized(): Boolean {
//        return adminRepository.isSuperAdminInitialized()
//    }
//
//    /**
//     * Kiểm tra xem người dùng có phải là quản trị viên hay không (quản trị viên thông thường hoặc quản trị viên cấp cao)
//     */
//    suspend fun isAnyAdmin(userId: String?): Boolean {
//        if (userId == null) return false
//        return isAdmin(userId) || isSuperAdmin(userId)
//    }
//}
//
///**
// * Admin status enum
// */
//enum class AdminStatus {
//    USER,       // Regular user
//    ADMIN,      // Regular admin
//    SUPER_ADMIN // Super admin with all permissions
//}
