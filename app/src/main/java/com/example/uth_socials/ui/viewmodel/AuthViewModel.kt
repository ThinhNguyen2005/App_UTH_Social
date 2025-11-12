package com.example.uth_socials.ui.viewmodel



import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.user.User
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.util.SecurityValidator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val googleClient: GoogleSignInClient
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state = _state.asStateFlow()
    val currentUser = FirebaseAuth.getInstance().currentUser

//    fun login(email: String, password: String) {
//        viewModelScope.launch {
//            _state.value = AuthState.Loading
//            auth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener { task ->
//                    _state.value = if (task.isSuccessful)
//                        AuthState.Success("Đăng nhập thành công")
//                    else
//                        AuthState.Error("Sai tài khoản hoặc mật khẩu")
//                }
//        }
//    }

    fun login(email: String, password: String) {
        viewModelScope.launch (Dispatchers.IO){
            _state.value = AuthState.Loading
            try {
                // ✅ Dùng .await() để code tuần tự và sạch sẽ hơn
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    // ✅ Đảm bảo profile tồn tại, có thể là user cũ nhưng bị lỗi tạo doc
                    userRepository.createUserProfileIfNotExists(firebaseUser)


                    val userProfile = userRepository.getUser(firebaseUser.uid)
                    if (userProfile?.isBanned == true) {
                        // Nếu bị cấm, buộc đăng xuất và báo lỗi
                        auth.signOut()
                        SecurityValidator.clearCache()
                        _state.value = AuthState.Error("Tài khoản của bạn đã bị khóa.")
                    } else {
                        // Nếu không, cho phép đăng nhập
                        _state.value = AuthState.Success("Đăng nhập thành công")
                        updateUserToken()
                    }


                } else {
                    _state.value = AuthState.Error("Không thể xác thực người dùng.")
                }
            } catch (e: Exception) {
                // ✅ Xử lý lỗi tập trung tại một nơi
                _state.value = AuthState.Error("Sai tài khoản hoặc mật khẩu.")
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    userRepository.createUserProfileIfNotExists(firebaseUser)
                    _state.value = AuthState.Success("Đăng ký thành công")
                    updateUserToken()
                } else {
                    _state.value = AuthState.Error("Không thể tạo người dùng.")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Lỗi đăng ký không xác định")
            }
        }
    }

    fun loginWithGoogle(activity: Activity, launcher: (Intent) -> Unit) {
        googleClient.signOut().addOnCompleteListener {
            launcher(googleClient.signInIntent)
        }
    }

    fun handleGoogleResult(data: Intent?) {
        viewModelScope.launch (Dispatchers.IO){
            _state.value = AuthState.Loading
            try {
                // Lấy thông tin tài khoản Google
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                // Đăng nhập vào Firebase
                val result = auth.signInWithCredential(credential).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    // ✅ GỌI HÀM TẠO PROFILE NGAY SAU KHI ĐĂNG NHẬP GOOGLE THÀNH CÔNG
                    userRepository.createUserProfileIfNotExists(firebaseUser)
                    // 2. Lấy hồ sơ người dùng từ Firestore
                    val userProfile = userRepository.getUser(firebaseUser.uid)

                    // 3. Kiểm tra trường isBanned
                    if (userProfile?.isBanned == true) {
                        // Nếu bị cấm, đăng xuất (cả Firebase và Google) và báo lỗi
                        auth.signOut()
                        googleClient.signOut() // Đăng xuất khỏi Google
                        SecurityValidator.clearCache()
                        _state.value = AuthState.Error("Tài khoản của bạn đã bị khóa.")
                    } else {
                        // Nếu không, đăng nhập thành công
                        _state.value = AuthState.Success("Đăng nhập Google thành công")
                        updateUserToken()
                    }                } else {
                    _state.value = AuthState.Error("Không lấy được thông tin người dùng.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
                _state.value = AuthState.Error("Đăng nhập Google thất bại: ${e.message}")
            }
        }
    }

    private fun updateUserToken() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                println("❌ Lấy token thất bại: ${task.exception}")
                return@addOnCompleteListener
            }

            val token = task.result
            val db = Firebase.firestore

            db.collection("users")
                .document(user.uid)
                .update("token", token)
                .addOnSuccessListener {
                    println("✅ Token người dùng đã được cập nhật: $token")
                }
                .addOnFailureListener { e ->
                    println("❌ Lỗi khi lưu token: $e")
                }
        }
    }
    fun resetState() {
        _state.value = AuthState.Idle
    }
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    fun resetPassword(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
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