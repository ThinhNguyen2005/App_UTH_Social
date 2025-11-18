package com.example.uth_socials.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserInfoViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {
    // 1. Tạo State để giữ dữ liệu
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()



    private val _campus = MutableStateFlow("")
    val campus = _campus.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _major = MutableStateFlow("")
    val major = _major.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio = _bio.asStateFlow()

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl = _avatarUrl.asStateFlow()


    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    // Lấy user ID từ Auth (an toàn hơn)
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // 2. Tạo hàm để tải dữ liệu
    fun loadInitialData() {
        if (userId == null) return

        viewModelScope.launch {
            _isLoading.value = true
            val user = userRepository.getUser(userId)
            if (user != null) {
                _username.value = user.username
                _campus.value = user.campus ?: ""
                _phone.value = user.phone ?: ""
                _major.value = user.major ?: ""
                _avatarUrl.value = user.avatarUrl ?: ""
                _bio.value = user.bio ?: ""
            }
            _isLoading.value = false
        }
    }
    fun onUsernameChange(newText: String) { _username.value = newText }
    fun onCampusChange(newText: String) { _campus.value = newText }
    fun onPhoneChange(newText: String) { _phone.value = newText }
    fun onMajorChange(newText: String) { _major.value = newText }
    fun onBioChange(newText: String) { _bio.value = newText }
    fun updateUserProfile(
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (_username.value.isBlank()) {
            onError("Tên hiển thị không được để trống")
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                var finalAvatarUrl: String? = null
                if (imageUri != null) {
                    finalAvatarUrl = userRepository.uploadProfileImage(imageUri)
                }

                userRepository.updateUserProfile(
                    avatarUrl = finalAvatarUrl,
                    username = _username.value,
                    campus = _campus.value,
                    phone = _phone.value,
                    major = _major.value,
                    bio = _bio.value
                )

                _isSaving.value = false
                onSuccess()

            } catch (e: Exception) {
                _isSaving.value = false
                onError(e.message ?: "Lỗi khi cập nhật thông tin")
            }
        }
    }

}
