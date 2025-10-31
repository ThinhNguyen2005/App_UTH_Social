package com.example.uth_socials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.uth_socials.ui.screen.OnboardingScreen
import com.example.uth_socials.ui.theme.UTH_SocialsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Dòng này có thể giữ hoặc xóa tùy ý
        setContent {
            // Sử dụng Theme của bạn để bao bọc màn hình
            UTH_SocialsTheme {
                // Ở đây, thay vì gọi Greeting, chúng ta gọi OnboardingScreen()
                // OnboardingScreen() chính là giao diện bạn đã tạo trong file HelloUser.kt
                OnboardingScreen()
            }
        }
    }
}