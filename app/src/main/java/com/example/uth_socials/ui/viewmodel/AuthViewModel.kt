package com.example.uth_socials.ui.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    object Authenticated : AuthState()

    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val googleClient: GoogleSignInClient,
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state = _state.asStateFlow()

    // ... các hàm login và register giữ nguyên ...
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    _state.value = if (task.isSuccessful)
                        AuthState.Success("Đăng nhập thành công")
                    else
                        AuthState.Error("Sai tài khoản hoặc mật khẩu")
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    _state.value = if (task.isSuccessful)
                        AuthState.Success("Đăng ký thành công")
                    else
                        AuthState.Error(task.exception?.message ?: "Lỗi đăng ký")
                }
        }
    }


    fun loginWithGoogle(activity: Activity, launcher: (Intent) -> Unit) {
        googleClient.signOut().addOnCompleteListener {
            launcher(googleClient.signInIntent)
        }
    }

    // ✅ HÀM NÀY ĐÃ ĐƯỢC CẬP NHẬT
    fun handleGoogleResult(activity: Activity, data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            viewModelScope.launch {
                _state.value = AuthState.Loading
                try {
                    val authResult = auth.signInWithCredential(credential).await()
                    val firebaseUser = authResult.user

                    if (firebaseUser != null) {
                        // Đợi cho hàm này chạy xong
                        userRepository.createUserProfileIfNotExists(firebaseUser)
                        // ✅ Sau khi hoàn tất, chuyển sang trạng thái Authenticated
                        _state.value = AuthState.Authenticated
                    } else {
                        _state.value = AuthState.Error("Không lấy được thông tin người dùng từ Google")
                    }
                } catch (e: Exception) {
                    _state.value = AuthState.Error("Đăng nhập Google thất bại: ${e.message}")
                }
            }
        } catch (e: Exception) {
            _state.value = AuthState.Error("Lỗi khi lấy thông tin Google: ${e.message}")
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }
}