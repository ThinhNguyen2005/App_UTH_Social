package com.example.uth_socials

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.util.SecurityValidator
import com.example.uth_socials.ui.component.navigation.AppNavGraph
import com.example.uth_socials.ui.theme.UTH_SocialsTheme
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.viewmodel.AuthViewModelFactory
import com.example.uth_socials.ui.viewmodel.BanStatusViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.BuildConfig


class MainActivity : ComponentActivity() {
    private lateinit var banStatusViewModel: BanStatusViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // --- Thiết lập các dependency cần thiết ---
        val auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)

        val userRepository = UserRepository()

        // Sử dụng Factory để tạo AuthViewModel
        val authViewModelFactory = AuthViewModelFactory(auth, userRepository, googleClient)
        val viewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        // ✅ THÊM: Khởi tạo BanStatusViewModel để theo dõi trạng thái ban
        banStatusViewModel = ViewModelProvider(this)[BanStatusViewModel::class.java]

        // --- Xử lý kết quả từ Google Sign-In ---
        val launcher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    // ✅ SỬA LỖI 2: Chỉ truyền 'result.data'
                    viewModel.handleGoogleResult(result.data)
                }
            }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val channel = NotificationChannel(
            "default_channel",
            "Thông báo chung",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)


        FirebaseMessaging.getInstance().subscribeToTopic("allUsers")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("✅ Subscribed to all_users topic")
                } else {
                    println("❌ Failed to subscribe")
                }
            }

        setContent {
            UTH_SocialsTheme {
                // Sử dụng Box để chứa cả AppNavGraph và nút crash
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Nội dung chính của ứng dụng vẫn được giữ nguyên
                    AppNavGraph(
                        viewModel = viewModel,
                        launcher = launcher
                    )

                    // 2. Thêm nút "Test Crash" ở đây
                    // Chỉ hiển thị nút này trong phiên bản DEBUG để tránh người dùng cuối thấy
                    if (BuildConfig.DEBUG) {
                        Button(
                            onClick = {
                                throw RuntimeException("Test Crash") // Hành động gây crash
                            },
                            // Căn chỉnh nút xuống góc dưới bên phải màn hình
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Text("Test Crash")
                        }
                    }
                }
            }
        }
    }
}