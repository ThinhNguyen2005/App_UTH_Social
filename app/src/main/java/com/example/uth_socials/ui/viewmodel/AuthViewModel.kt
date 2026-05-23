package com.example.uth_socials.ui.viewmodel



import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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

                    // ✅ THAY ĐỔI: Cho phép đăng nhập ngay cả khi bị ban
                    // BanStatusViewModel sẽ quản lý trạng thái ban và UI sẽ hiển thị dialog
                    _state.value = AuthState.Success("Đăng nhập thành công")
                    updateUserToken()


                } else {
                    _state.value = AuthState.Error("Không thể xác thực người dùng.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Đăng nhập thất bại", e)
                _state.value = AuthState.Error("Sai tài khoản hoặc mật khẩu.")
            }
        }
    }

    fun register(email: String, password: String,username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    userRepository.createUserProfileIfNotExists(firebaseUser,username)
                    _state.value = AuthState.Success("Đăng ký thành công")
                    updateUserToken()
                } else {
                    _state.value = AuthState.Error("Không thể tạo người dùng.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Đăng ký thất bại", e)
                _state.value = AuthState.Error(e.message ?: "Lỗi đăng ký không xác định")
            }
        }
    }

    fun loginWithGoogle(@Suppress("UNUSED_PARAMETER") activity: Activity, launcher: (Intent) -> Unit) {
        _state.value = AuthState.Loading
        googleClient.signOut().addOnCompleteListener {
            launcher(googleClient.signInIntent)
        }
    }

    fun handleGoogleResult(resultCode: Int, data: Intent?) {
        // RESULT_CANCELED = 0
        if (resultCode != android.app.Activity.RESULT_OK) {
            Log.w("AuthViewModel", "Google sign-in cancelled or failed, resultCode=$resultCode")
            _state.value = AuthState.Error("Bạn đã huỷ đăng nhập Google.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = AuthState.Loading
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account?.idToken
                if (idToken.isNullOrBlank()) {
                    _state.value = AuthState.Error("Không lấy được thông tin từ Google. Vui lòng thử lại.")
                    return@launch
                }
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    userRepository.createUserProfileIfNotExists(firebaseUser)
                    _state.value = AuthState.Success("Đăng nhập Google thành công")
                    updateUserToken()
                } else {
                    _state.value = AuthState.Error("Không lấy được thông tin người dùng.")
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Log.e("AuthViewModel", "Google Sign-In ApiException status=${e.statusCode}", e)
                _state.value = AuthState.Error(mapGoogleApiError(e.statusCode))
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
                _state.value = AuthState.Error("Đăng nhập Google thất bại: ${e.message ?: "Lỗi không xác định"}")
            }
        }
    }

    private fun mapGoogleApiError(statusCode: Int): String = when (statusCode) {
        com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
            "Bạn đã huỷ đăng nhập Google."
        com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_FAILED ->
            "Đăng nhập Google thất bại. Hãy thử lại."
        com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
            "Đang xử lý phiên đăng nhập trước, vui lòng đợi."
        com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.NETWORK_ERROR ->
            "Mất kết nối mạng khi đăng nhập Google."
        com.google.android.gms.common.api.CommonStatusCodes.DEVELOPER_ERROR ->
            "Cấu hình Google Sign-In sai (sai SHA-1 / webClientId). Báo cho quản trị viên."
        com.google.android.gms.common.api.CommonStatusCodes.INTERNAL_ERROR ->
            "Lỗi nội bộ Google Play Services. Hãy thử lại."
        else -> "Đăng nhập Google thất bại (mã $statusCode)."
    }

    private fun updateUserToken() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("AuthViewModel", "Lấy FCM token thất bại", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val db = Firebase.firestore

            db.collection("users")
                .document(user.uid)
                .update("token", token)
                .addOnSuccessListener {
                    Log.d("AuthViewModel", "Token người dùng đã được cập nhật.")
                }
                .addOnFailureListener { e ->
                    Log.e("AuthViewModel", "Lỗi khi lưu token", e)
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
    fun changePassword(oldPassword: String,newPassword: String ){
        viewModelScope.launch(Dispatchers.IO){
            _state.value = AuthState.Loading
            try {
                val user = auth.currentUser
                if (user ==null || user.email ==null){
                    _state.value =AuthState.Error("Không tìm thấy người dùng hiện tại")
                    return@launch
                }
                val credential = EmailAuthProvider.getCredential(user.email!!,oldPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                _state.value =AuthState.Success("Đổi mật khẩu thành công")
            }catch (e: Exception){
                Log.e("AuthViewModel", "Lỗi khi đổi mật khẩu", e)
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Mật khẩu cũ không đúng."
                    is FirebaseAuthWeakPasswordException -> "Mật khẩu mới quá yếu (cần ít nhất 6 ký tự)."
                    else -> "Đã xảy ra lỗi. Vui lòng thử lại."
                }
                _state.value = AuthState.Error(errorMessage)
            }
        }
    }

    // Hàm kiểm tra xem user có đăng nhập bằng mật khẩu không
    fun isEmailPasswordUser(): Boolean {
        val user = auth.currentUser
        return user?.providerData?.any { it.providerId == "password" } == true
    }

}