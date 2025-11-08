package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.repository.AdminRepository

class ViewModelFactory(
    private val postRepository: PostRepository,
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val adminRepository: AdminRepository = AdminRepository()
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(postRepository, categoryRepository, adminRepository) as T
        }
        // Thêm các ViewModel khác ở đây nếu cần
        // if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
        //     return ProfileViewModel(userId, postRepository) as T
        // }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}