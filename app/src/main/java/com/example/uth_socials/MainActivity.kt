package com.example.uth_socials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.uth_socials.ui.screen.OnboardingScreen
import com.example.uth_socials.ui.screen.WelcomeScreen
import com.example.uth_socials.ui.theme.UTH_SocialsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UTH_SocialsTheme {
                // 1. Gọi AppNavigator() ở đây để kích hoạt hệ thống điều hướng
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "onboarding" // Đặt màn hình bắt đầu là "onboarding"
    ) {
        composable("onboarding") {
            // 2. Truyền hành động điều hướng vào OnboardingScreen
            // Khi onStartClick được gọi, nó sẽ chuyển đến màn hình "welcome"
            OnboardingScreen(
                onStartClicked = { navController.navigate("welcome") }
            )
        }
        composable("welcome") {
            // Màn hình WelcomeScreen không cần thay đổi
            WelcomeScreen(
                onLoginClick = { /* Xử lý khi nhấn Login */ },
                onRegisterClick = { /* Xử lý khi nhấn Register */ }
            )
        }
    }
}
