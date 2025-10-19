package com.example.uth_socials.ui.navigation1



import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.uth_socials.LoginScreen
import com.example.uth_socials.ui.screen.OnboardingScreen

import com.example.uth_socials.ui.screen.RegisterScreen
import com.example.uth_socials.ui.screen.home.HomeScreen
import com.example.uth_socials.ui.viewmodel.AuthViewModel

@Composable
fun AuthNav(
    viewModel: AuthViewModel,
    launcher: ActivityResultLauncher<Intent>
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // 1️⃣ Onboarding đầu tiên
//        composable("onboarding") {
//            OnboardingScreen(
//                helloUserModel = androidx.lifecycle.viewmodel.compose.viewModel(),
//                // Khi nhấn nút "Bắt đầu" → sang Login
//                onStartClick = { navController.navigate("login") }
//            )
//        }

        // 2️⃣ Màn đăng nhập
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onGoogleLoginClick = {
                    viewModel.loginWithGoogle(activity = navController.context as Activity) {
                        launcher.launch(it)
                    }
                },
                onRegisterClick = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // 3️⃣ Màn đăng ký
        composable("register") {
            RegisterScreen(
                viewModel = viewModel,
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // 4️⃣ Home sau khi đăng nhập
        composable("home") {
            HomeScreen()
        }
    }
}
