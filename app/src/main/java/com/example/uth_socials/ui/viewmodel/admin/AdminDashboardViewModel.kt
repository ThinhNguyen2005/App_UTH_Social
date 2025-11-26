package com.example.uth_socials.ui.viewmodel.admin

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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// Chặn bỏ chặn user, làm mới khi có thay đổi
// Cấp, thu quyền admin
// Category load, add, update, delete
//xem báo cáo, load admin, load danh mục, load người dùng bị chặn
class AdminDashboardViewModel : ViewModel() {
    private val categoryRepository = CategoryRepository()
    private val adminRepository = AdminRepository()

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("AdminDashboardViewModel", "Loading data started...")
            val reportsJob = launch { loadPendingReportsBackground() }
            val usersJob = launch { loadBannedUsersBackground() }
            val adminsJob = launch { loadAdminsBackground() }
            val categoriesJob = launch { loadCategoriesBackground() }

            reportsJob.join()
            categoriesJob.join()
            usersJob.join()
            adminsJob.join()
        }
    }

    private suspend fun loadPendingReportsBackground() {
        _uiState.update { it.copy(isLoadingReports = true) }
        try {
            val reports = adminRepository.getPendingReports()
            _uiState.update {
                Log.d("AdminDashboardViewModel", "Loaded reports: $reports")
                it.copy(
                    pendingReports = reports,
                    isLoadingReports = false,
                    successMessage = "Cập nhật thành công", error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                Log.e("AdminDashboardViewModel", "Load reports failed: ${e.message}", e)
                it.copy(
                    error = "Load reports thất bại: ${e.message}",
                    isLoadingReports = false
                )
            }
        }
    }
    private suspend fun loadBannedUsersBackground() {
        _uiState.update { it.copy(isLoadingUsers = true) }
        try {
            val bannedUsers = adminRepository.getBannedUsers()
            _uiState.update {
                Log.d("AdminDashboardViewModel", "Loaded banned users: $bannedUsers")
                it.copy(
                    bannedUsers = bannedUsers,
                    isLoadingUsers = false,
                    successMessage = "Cập nhật thành công", error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                Log.e("AdminDashboardViewModel", "Load banned users failed: ${e.message}", e)
                it.copy(
                    error = "Không lấy được danh sách người dùng bị chặn: ${e.message}",
                    isLoadingUsers = false
                )
            }
        }
    }

    private suspend fun loadAdminsBackground() {
        _uiState.update { it.copy(isLoadingAdmins = true) }
        try {
            val admins = adminRepository.getAllAdmins()
            _uiState.update {
                Log.d("AdminDashboardViewModel", "Loaded admins: $admins")
                it.copy(
                    admins = admins,
                    isLoadingAdmins = false,
                    successMessage = "Cập nhật thành công", error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                Log.e("AdminDashboardViewModel", "Load admins failed: ${e.message}", e)
                it.copy(
                    error = "Load danh sách admin thất bại: ${e.message}",
                    isLoadingAdmins = false
                )
            }
        }
    }
    // Xem xét báo cáo
    fun reviewReport(reportId: String, action: AdminAction, adminNotes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                ?: run {
                    _uiState.update { it.copy(error = "Không có người dùng đăng nhập") }
                    return@launch
                }

            adminRepository.reviewReport(reportId, currentUserId, action, adminNotes)
                .onSuccess {
                    Log.d("AdminDashboardViewModel", "Report reviewed successfully")
                    _uiState.update {
                        it.copy(successMessage = "Xử lý báo cáo thành công")
                    }
                    loadPendingReportsBackground()
                }
                .onFailure { e ->
                    Log.e("AdminDashboardViewModel", "Report review failed: ${e.message}", e)
                    _uiState.update {
                        it.copy(error = "Xử lý báo cáo thất bại: ${e.message}")
                    }
                }
        }
    }
    fun refreshBannedUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            loadBannedUsersBackground()
        }
    }

    /**
     * Khởi động realtime listener cho danh sách người dùng bị cấm
     * Tự động cập nhật UI khi có thay đổi trong Firestore
     */
    fun startBannedUsersRealtimeListener() {
        adminRepository.getBannedUsersFlow()
            .onEach { bannedUsers ->
                _uiState.update {
                    it.copy(
                        bannedUsers = bannedUsers,
                        isLoadingUsers = false
                    )
                }
                Log.d("AdminDashboardViewModel", "Realtime update: ${bannedUsers.size} banned users")
            }
            .launchIn(viewModelScope)
    }
    //chặn user
    fun banUser(userId: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                ?: run {
                    _uiState.update { it.copy(error = "Không có người dùng đăng nhập") }
                    return@launch
                }
            adminRepository.banUser(userId, currentUserId, reason)
                .onSuccess {
                    Log.d("AdminDashboardViewModel", "Ban successful: $userId")
                    _uiState.update {
                        it.copy(successMessage = "Chặn người dùng thành công")
                    }
                    loadBannedUsersBackground()
                }
                .onFailure { e ->
                    Log.e("AdminDashboardViewModel", "Ban failed: ${e.message}", e)
                    _uiState.update { it.copy(error = "Chặn thất bại: ${e.message}") }
                }
        }
    }

    // Bỏ chặn user
    fun unbanUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            adminRepository.unbanUser(userId)
                .onSuccess {
                    Log.d("AdminDashboardViewModel", "Gỡ chặn thành công: $userId")
                    _uiState.update {
                        it.copy(successMessage = "Gỡ chặn người dùng thành công")
                    }
                    loadBannedUsersBackground()
                }
                .onFailure { e ->
                    Log.e("AdminDashboardViewModel", "Gỡ chặn thất bại: ${e.message}", e)
                    _uiState.update {
                        it.copy(error = "Gỡ chặn thất bại: ${e.message}")
                    }
                }
        }
    }

    //Cấp quyền admin
    fun grantAdminRole(userId: String, role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                ?: run {
                    _uiState.update { it.copy(error = "Không có người dùng này") }
                    return@launch
                }

            adminRepository.grantAdminRole(
                targetUserId = userId,
                role = role,
                grantedBy = currentUserId
            )
                .onSuccess {
                    Log.d("AdminDashboardViewModel", "Cấp quyền admin thành công: $userId")
                    _uiState.update {
                        it.copy(successMessage = "Cấp quyền admin thành công")
                    }
                    loadAdminsBackground()
                }
                .onFailure { e ->
                    Log.e("AdminDashboardViewModel", "Cấp quyền admin thất bại: ${e.message}", e)
                    _uiState.update {
                        it.copy(error = "Cấp quyền admin thất bại: ${e.message}")
                    }
                }
        }
    }

    // Thu hồi quyền admin
    fun revokeAdminRole(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            adminRepository.revokeAdminRole(userId)
                .onSuccess {
                    Log.d("AdminDashboardViewModel", "Thu hồi quyền admin thành công: $userId")
                    _uiState.update {
                        it.copy(successMessage = "Thu hồi quyền admin thành công")
                    }
                    loadAdminsBackground()
                }
                .onFailure { e ->
                    Log.e("AdminDashboardViewModel", "Thu hồi quyền admin thất bại: ${e.message}", e)
                    _uiState.update {
                        it.copy(error = "Thu hồi quyền admin thất bại: ${e.message}")
                    }
                }
        }
    }

    // Category load, add, update, delete
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
                    error = "Load categories thất bại: ${e.message}",
                    isLoadingCategories = false
                )
            }
        }
    }

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
            val categories = _uiState.value.categories
            val nameExists = categories.any {
                it.name.equals(cleanName, ignoreCase = true)
            }
            if (nameExists) {
                _uiState.update { it ->
                    it.copy(error = "Tên danh mục '$cleanName' đã tồn tại. Hãy tạo danh mục khác.")
                }
                return@launch
            }
            // Kiểm tra Category đã tồn tại
            val newCategory = createAutoCategory(cleanName)
            val idExists = categories.any { it.id == newCategory.id }

            if (idExists) {
                _uiState.update {
                    it.copy(error = "ID danh mục '${newCategory.id}' đã tồn tại.")
                }
                return@launch
            }
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val result = categoryRepository.addCategory(newCategory, currentUserId)

            result.onSuccess {
                Log.d("AdminDashboardViewModel", "Category Add successfully.")
                _uiState.update {it ->
                    it.copy(successMessage = "Thêm category thành công")
                }
                loadCategoriesBackground()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = "Thêm category thất bại: ${e.message}")
                }
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
                Log.d("AdminDashboardViewModel", "Category edit successfully.")

                _uiState.update {
                    it.copy(successMessage = "Sửa category thành công")
                }
                loadCategoriesBackground()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = "Sửa category thất bại: ${e.message}")
                }
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
                _uiState.update {
                    it.copy(successMessage = "Xóa category thành công")
                }
                loadCategoriesBackground()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = "Xóa category thất bại: ${e.message}")
                }
            }
        }
    }

    fun refreshAdminStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val status = adminRepository.getCurrentUserAdminStatus()
            _uiState.update { it.copy(
                isCurrentUserAdmin = status.isAdmin,
                currentUserRole = when {
                    status.isSuperAdmin -> "super_admin"
                    status.isAdmin -> "admin"
                    else -> null
                }
            ) }
        }
    }
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

}