package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.config.AdminConfig
import com.example.uth_socials.data.post.AdminAction
import com.example.uth_socials.data.post.AdminReport
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.user.AdminDashboardUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class AdminDashboardViewModel : ViewModel() {

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

            // Chờ tất cả hoàn thành
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
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = AdminConfig.reviewReport(reportId, currentUserId, action, adminNotes)
                if (result.isSuccess) {
                    // Refresh data after successful action
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

    fun banUser(userId: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = AdminConfig.banUser(userId, currentUserId, reason)
                if (result.isSuccess) {
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
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = AdminConfig.grantAdminRole(
                    targetUserId = userId,
                    role = role,
                    grantedBy = currentUserId
                )

                if (result.isSuccess) {
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
}
