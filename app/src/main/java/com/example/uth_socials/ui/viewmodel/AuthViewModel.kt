package com.example.uth_socials.ui.viewmodel



import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val googleClient: GoogleSignInClient
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state = _state.asStateFlow()
    val currentUser = FirebaseAuth.getInstance().currentUser

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

    fun handleGoogleResult(activity: Activity, data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task2 ->
                    _state.value = if (task2.isSuccessful)
                        AuthState.Success("Đăng nhập Google thành công")
                    else
                        AuthState.Error("Đăng nhập Google thất bại")
                }
        } catch (e: Exception) {
            _state.value = AuthState.Error("Đăng nhập thất bại: ${e.message}")
        }
    }
    fun resetState() {
        _state.value = AuthState.Idle
    }
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    _state.value = if (task.isSuccessful)
                        AuthState.Success("Email đặt lại mật khẩu đã được gửi đến $email")
                    else
                        AuthState.Error(task.exception?.message ?: "Lỗi khi gửi email đặt lại mật khẩu")
                }
        }
    }


}