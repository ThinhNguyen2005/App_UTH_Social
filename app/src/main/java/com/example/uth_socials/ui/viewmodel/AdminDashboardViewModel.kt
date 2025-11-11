package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.AdminAction
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.repository.AdminRepository
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.user.AdminDashboardUiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminDashboardViewModel : ViewModel() {
    private val categoryRepository = CategoryRepository()
    private val adminRepository = AdminRepository()


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
            val reports = adminRepository.getPendingReports()
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
            val bannedUsers = adminRepository.getBannedUsers()
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
            val admins = adminRepository.getAllAdmins()
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
    // Xem xét báo cáo
    fun reviewReport(reportId: String, action: AdminAction, adminNotes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = adminRepository.reviewReport(reportId, currentUserId, action, adminNotes)
                if (result.isSuccess) {
                    Log.d("AdminDashboardViewModel", "Report reviewed successfully.")
                    loadPendingReportsBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to review report: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to review report: ${e.message}")
                }
            }
        }
    }

    //chặn user
    fun banUser(userId: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = adminRepository.banUser(userId, currentUserId, reason)
                if (result.isSuccess) {
                    Log.d("AdminDashboardViewModel", "Chặn user thành công: ${userId}.")
                    //Làm mới danh sách người bị chặn
                    loadBannedUsersBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Chặn thất bại: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Chặn thất bại: ${e.message}")
                }
            }
        }
    }

    // Bỏ chặn user
    fun unbanUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = adminRepository.unbanUser(userId)
                if (result.isSuccess) {
                    // Log success message
                    Log.d("AdminDashboardViewModel", "Gỡ chặn thành công: ${userId}.")

                    // Refresh banned users list
                    loadBannedUsersBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Gỡ chặn thất bại: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Gỡ chặn thất bại: ${e.message}")
                }
            }
        }
    }

    //Cấp quyền admin
    fun grantAdminRole(userId: String, role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = adminRepository.grantAdminRole(
                    targetUserId = userId,
                    role = role,
                    grantedBy = currentUserId
                )

                if (result.isSuccess) {
                    // Log success message
                    Log.d("AdminDashboardViewModel", "Cấp quyền admin thành công: ${userId}.")

                    loadAdminsBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Cấp quyền admin thất bại: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Cấp quyền admin thất bại: ${e.message}")
                }
            }
        }
    }

    // Thu hồi quyền admin
    fun revokeAdminRole(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = adminRepository.revokeAdminRole(userId)
                if (result.isSuccess) {
                    // Log success message
                    Log.d("AdminDashboardViewModel", "Đã cấp quyền cho admin: ${userId}.")
                    loadAdminsBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Thu hồi quyền admin thất bại: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Thu hồi quyền admin thất bại: ${e.message}")
                }
            }
        }
    }

    // Category add/edit/delete
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
            val nameExists = categories.any {
                it.name.equals(cleanName, ignoreCase = true)
            }

            if (nameExists) {
                _uiState.update {
                    it.copy(error = "Tên danh mục '$cleanName' đã tồn tại. Hãy tạo danh mục khác.")
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
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val result = categoryRepository.addCategory(newCategory, currentUserId)

            result.onSuccess {
                loadCategoriesBackground()
            }.onFailure { e ->
                handleFailure(e, "add category")
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
