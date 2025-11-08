package com.example.uth_socials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.ui.component.navigation.AppNavGraph
import com.example.uth_socials.ui.theme.UTH_SocialsTheme
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.viewmodel.AuthViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//
//        val auth = FirebaseAuth.getInstance()
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(getString(R.string.default_web_client_id))
//            .requestEmail()
//            .build()
//        val googleClient = GoogleSignIn.getClient(this, gso)
//        val viewModel = AuthViewModel(auth, googleClient)
//
//        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            viewModel.handleGoogleResult(this, result.data)
//        }
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//
//        setContent {
//            UTH_SocialsTheme {
//                AppNavGraph(viewModel = viewModel, launcher = launcher)
//
//            }
//        }
//    }
//}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Thiết lập các dependency cần thiết ---
        val auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)

        // ✅ SỬA LỖI 1: Khởi tạo UserRepository
        val userRepository = UserRepository()

        // Sử dụng Factory để tạo AuthViewModel
        val authViewModelFactory = AuthViewModelFactory(auth, userRepository, googleClient)
        val viewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        // --- Xử lý kết quả từ Google Sign-In ---
        val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // ✅ SỬA LỖI 2: Chỉ truyền 'result.data'
                viewModel.handleGoogleResult(result.data)
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            UTH_SocialsTheme {
                AppNavGraph(
                    viewModel = viewModel,
                    launcher = launcher
                )
            }
        }
    }
}