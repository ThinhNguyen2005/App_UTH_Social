package com.example.uth_socials.data.user

import com.example.uth_socials.data.post.AdminReport
import com.example.uth_socials.data.post.Category
import com.google.firebase.Timestamp

data class AdminUser(
    val userId: String = "",
    val role: String = "",
    val grantedBy: String = "",
    val grantedAt: Timestamp? = null,
    val permissions: List<String> = emptyList()
)
data class AdminDashboardUiState(
    val pendingReports: List<AdminReport> = emptyList(),
    val bannedUsers: List<User> = emptyList(),
    val admins: List<AdminUser> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoadingReports: Boolean = false,
    val isLoadingUsers: Boolean = false,
    val isLoadingAdmins: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isCurrentUserAdmin: Boolean = false,
    val currentUserRole: String? = null
)
data class AdminStatus(
    val isAdmin: Boolean,
    val isSuperAdmin: Boolean
)
