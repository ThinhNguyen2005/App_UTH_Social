package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.user.AdminDashboardUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.compareTo

class CategoryModel : ViewModel() {
    private val categoryRepository = CategoryRepository()
    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()
    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {

            val categoriesJob = launch { loadCategoriesBackground() }
            categoriesJob.join()
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

    fun addCategory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = categoryRepository.addCategory(
                    Category(
                        id = name.lowercase().replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), "_").take(20),
                        name = name.trim(),
                        order = uiState.value.categories.size + 1
                    ),
                    currentUserId
                )

                if (result.isSuccess) {
                    loadCategoriesBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to add category: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to add category: ${e.message}")
                }
            }
        }
    }

    fun updateCategory(categoryId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val existingCategory = uiState.value.categories.find { it.id == categoryId }
                    ?: throw IllegalStateException("Category not found")

                val updatedCategory = existingCategory.copy(name = newName.trim())
                val result = categoryRepository.updateCategory(updatedCategory, currentUserId)

                if (result.isSuccess) {
                    loadCategoriesBackground()
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to update category: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to update category: ${e.message}")
                }
            }
        }
    }

    fun deleteCategoryWithConfirmation(categoryId: String, migrateToCategoryId: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("No admin user logged in")

                val result = categoryRepository.deleteCategoryWithConfirmation(categoryId, currentUserId, migrateToCategoryId)

                if (result.isSuccess) {
                    val deletionResult = result.getOrThrow()
                    // Show success message with migration info
                    val message = "Category '${deletionResult.deletedCategory.name}' deleted successfully. " +
                            if (deletionResult.postsMigrated > 0) {
                                "${deletionResult.postsMigrated} posts moved to ${if (deletionResult.migrationTarget.isEmpty()) "'Uncategorized'" else "'${deletionResult.migrationTarget}'"}."
                            } else {
                                "No posts were affected."
                            }

                    // Refresh categories list
                    loadCategoriesBackground()

                    // Note: We could add a success state to show this message in UI
                    // For now, we'll just refresh the data
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to delete category: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to delete category: ${e.message}")
                }
            }
        }
    }

}