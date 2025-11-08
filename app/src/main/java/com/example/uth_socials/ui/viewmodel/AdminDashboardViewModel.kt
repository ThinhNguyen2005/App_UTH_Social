package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.config.AdminConfig
import com.example.uth_socials.data.post.AdminAction
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.user.AdminDashboardUiState
import com.example.uth_socials.data.util.SecurityValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class AdminDashboardViewModel : ViewModel() {
    private val categoryRepository = CategoryRepository()

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val reportsJob = launch { loadPendingReportsBackground() }
            val usersJob = launch { loadBannedUsersBackground() }
            val adminsJob = launch { loadAdminsBackground() }
            val categoriesJob = launch { loadCategoriesBackground() }
            categoriesJob.join()
            reportsJob.join()
            usersJob.join()
            adminsJob.join()
        }
    }

    private suspend fun loadPendingReportsBackground() {
        _uiState.update { it.copy(isLoadingReports = true) }
        try {
            val reports = AdminConfig.getPendingReports()
            _uiState.update {
                it.copy(
                    pendingReports = reports,
                    isLoadingReports = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    error = "Failed to load reports: ${e.message}",
                    isLoadingReports = false
                )
            }
        }
    }

    /**
     * Background version của loadBannedUsers
     */
    private suspend fun loadBannedUsersBackground() {
        _uiState.update { it.copy(isLoadingUsers = true) }
        try {
            val bannedUsers = AdminConfig.getBannedUsers()
            _uiState.update {
                it.copy(
                    bannedUsers = bannedUsers,
                    isLoadingUsers = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    error = "Failed to load banned users: ${e.message}",
                    isLoadingUsers = false
                )
            }
        }
    }

    /**
     * Background version của loadAdmins
     */
    private suspend fun loadAdminsBackground() {
        _uiState.update { it.copy(isLoadingAdmins = true) }
        try {
            val admins = AdminConfig.getAllAdmins()
            _uiState.update {
                it.copy(
                    admins = admins,
                    isLoadingAdmins = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    error = "Failed to load admins: ${e.message}",
                    isLoadingAdmins = false
                )
            }
        }
    }
    fun reviewReport(reportId: String, action: AdminAction, adminNotes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                // ✅ PRE-VALIDATION: Check permissions before executing action
                when (action) {
                    AdminAction.BAN_USER -> {
                        // Get target user ID from the report
                        val reports = AdminConfig.getPendingReports()
                        val targetReport = reports.find { it.report.id == reportId }
                        val targetUserId = targetReport?.post?.userId

                        if (targetUserId != null && !SecurityValidator.canBanUser(currentUserId, targetUserId)) {
                            val errorMessage = when {
                                targetUserId == currentUserId -> "Cannot ban yourself"
                                AdminConfig.isSuperAdmin(targetUserId) -> "Cannot ban super admin"
                                AdminConfig.isAdmin(targetUserId) -> "Only super admin can ban regular admins"
                                else -> "Cannot ban this user"
                            }
                            _uiState.update { it.copy(error = errorMessage) }
                            return@launch
                        }
                    }

                    AdminAction.BAN_REPORTER -> {
                        // Get reporter ID from the report
                        val reports = AdminConfig.getPendingReports()
                        val targetReport = reports.find { it.report.id == reportId }
                        val reporterId = targetReport?.report?.reportedBy

                        if (reporterId != null && !SecurityValidator.canBanUser(currentUserId, reporterId)) {
                            val errorMessage = when {
                                AdminConfig.isSuperAdmin(reporterId) -> "Cannot ban super admin"
                                AdminConfig.isAdmin(reporterId) -> "Only super admin can ban regular admins"
                                else -> "Cannot ban this reporter"
                            }
                            _uiState.update { it.copy(error = errorMessage) }
                            return@launch
                        }
                    }

                    else -> Unit
                }

                val result = AdminConfig.reviewReport(reportId, currentUserId, action, adminNotes)
                if (result.isSuccess) {
                    Log.d("AdminDashboardViewModel", "Report reviewed successfully.")
                    loadPendingReportsBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to review report: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminDashboardViewModel", "Error reviewing report", e)
                _uiState.update {
                    it.copy(error = "Failed to review report: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun banUser(userId: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = AdminConfig.banUser(userId, currentUserId, reason)
                if (result.isSuccess) {
                    // Log success message
                    Log.d("AdminDashboardViewModel", "User banned successfully.")

                    // Refresh banned users list
                    loadBannedUsersBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to ban user: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to ban user: ${e.message}")
                }
            }
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AdminConfig.unbanUser(userId)
                if (result.isSuccess) {
                    // Log success message
                    Log.d("AdminDashboardViewModel", "User unbanned successfully.")

                    // Refresh banned users list
                    loadBannedUsersBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to unban user: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to unban user: ${e.message}")
                }
            }
        }
    }

    fun grantAdminRole(userId: String, role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = AdminConfig.grantAdminRole(
                    targetUserId = userId,
                    role = role,
                    grantedBy = currentUserId
                )

                if (result.isSuccess) {
                    // Log success message
                    Log.d("AdminDashboardViewModel", "Admin role granted successfully.")

                    loadAdminsBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to grant admin role: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to grant admin role: ${e.message}")
                }
            }
        }
    }

    fun revokeAdminRole(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AdminConfig.revokeAdminRole(userId)
                if (result.isSuccess) {
                    // Log success message
                    Log.d("AdminDashboardViewModel", "Admin role revoked successfully.")

                    // Refresh admin list
                    loadAdminsBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to revoke admin role: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to revoke admin role: ${e.message}")
                }
            }
        }
    }
    /**
     * Background version của loadCategories
     */


    private suspend fun loadCategoriesBackground() {
        _uiState.update { it.copy(isLoadingCategories = true) }
        try {
            val categories = categoryRepository.getCategories()
            _uiState.update {
                it.copy(
                    categories = categories,
                    isLoadingCategories = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    error = "Failed to load categories: ${e.message}",
                    isLoadingCategories = false
                )
            }
        }
    }

    // Hàm private để tạo Category (lấy từ CategoryModel)
    private fun createAutoCategory(name: String): Category {
        return Category(
            id = name.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), "")
                .replace(Regex("\\s+"), "_")
                .take(20),
            name = name.trim(),
            // Quan trọng: Phải dùng _uiState, không phải uiState
            order = _uiState.value.categories.size + 1
        )
    }

    fun addCategory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanName = name.trim()
            val categories = _uiState.value.categories // Lấy danh sách hiện tại từ State

            // 1. Kiểm tra trùng tên (case-insensitive)
            val nameExists = categories.any {
                it.name.equals(cleanName, ignoreCase = true)
            }

            if (nameExists) {
                _uiState.update {
                    it.copy(error = "Lỗi: Tên danh mục '$cleanName' đã tồn tại.")
                }
                return@launch
            }

            // 2. Kiểm tra trùng ID (sau khi tạo ID)
            val newCategory = createAutoCategory(cleanName)
            val idExists = categories.any { it.id == newCategory.id }

            if (idExists) {
                _uiState.update {
                    it.copy(error = "Lỗi: ID danh mục '${newCategory.id}' đã tồn tại.")
                }
                return@launch
            }
            // --- KẾT THÚC KIỂM TRA ---


            // Nếu không trùng, mới tiếp tục gọi Repository
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val result = categoryRepository.addCategory(newCategory, currentUserId)

            result.onSuccess {
                loadCategoriesBackground()
            }.onFailure { e ->
                handleFailure(e, "add category") // Giả sử bạn có hàm này
            }
        }
    }

    fun updateCategory(categoryId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val existingCategory = _uiState.value.categories.find { it.id == categoryId }
                    ?: throw IllegalStateException("Category not found")

                val updatedCategory = existingCategory.copy(name = newName.trim())
                categoryRepository.updateCategory(updatedCategory, currentUserId).getOrThrow()
            }

            result.onSuccess {
                loadCategoriesBackground()
            }.onFailure { e ->
                handleFailure(e, "update category")
            }
        }
    }

    fun deleteCategoryWithConfirmation(categoryId: String, migrateToCategoryId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val result = categoryRepository.deleteCategoryWithConfirmation(
                categoryId,
                currentUserId,
                migrateToCategoryId
            )

            result.onSuccess {
                Log.d("AdminDashboardViewModel", "Category deleted successfully.")
                loadCategoriesBackground()
            }.onFailure { e ->
                handleFailure(e, "delete category")
            }
        }
    }

    // Hàm xử lý lỗi chung
    private fun handleFailure(e: Throwable, action: String) {
        Log.e("AdminDashboardViewModel", "Failed to $action", e)
        _uiState.update {
            it.copy(error = "Failed to $action: ${e.message}")
        }
    }

}
