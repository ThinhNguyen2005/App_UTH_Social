package com.example.uth_socials.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class UserInfoViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    fun updateUserProfile(
        imageUri: Uri?,
        username: String,
        campus: String,
        phone: String,
        major: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                var finalAvatarUrl: String? = null

                // 3. (Bất đồng bộ) Tải ảnh lên NẾU CÓ ẢNH MỚI
                if (imageUri != null) {
                    finalAvatarUrl = userRepository.uploadProfileImage(imageUri)
                }

                // 4. (Bất đồng bộ) Cập nhật thông tin (với link ảnh mới, hoặc null)
                //    'finalAvatarUrl' sẽ là null nếu không có ảnh mới
                //    và hàm Repository (mới sửa) sẽ biết cách bỏ qua nó.
                userRepository.updateUserProfile(
                    avatarUrl = finalAvatarUrl,
                    username = username,
                    campus = campus,
                    phone = phone,
                    major = major
                )

                // 5. Báo thành công
                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Lỗi khi cập nhật thông tin")
            }
        }
    }

}
