// Tạo file mới, ví dụ: di/ViewModelFactory.kt

package com.example.uth_socials.di // hoặc một package phù hợp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.ui.viewmodel.HomeViewModel

class ViewModelFactory(private val postRepository: PostRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(postRepository) as T
        }
        // Thêm các ViewModel khác ở đây nếu cần
        // if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
        //     return ProfileViewModel(userId, postRepository) as T
        // }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}