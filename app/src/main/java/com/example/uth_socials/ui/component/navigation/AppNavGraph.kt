package com.example.uth_socials.ui.component.navigation


import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.uth_socials.LoginScreen
import com.example.uth_socials.ui.navigation.*
import com.example.uth_socials.ui.screen.RegisterScreen
import com.example.uth_socials.ui.screen.home.HomeScreen
import com.example.uth_socials.ui.screen.user.ProfileScreen
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    launcher: ActivityResultLauncher<Intent>,
    modifier: Modifier = Modifier
) {
    val start: AppDestination = if (FirebaseAuth.getInstance().currentUser != null) HomeDestination else LoginDestination
    val authState by authViewModel.state.collectAsState()
    LaunchedEffect(authState) {
        // Chỉ điều hướng khi trạng thái là Authenticated
        if (authState is AuthState.Authenticated) {
            navController.navigate(HomeDestination.route) {
                // Xóa tất cả các màn hình trước đó (Login, Register) khỏi back stack
                popUpTo(LoginDestination.route) { inclusive = true }
                // Đảm bảo không tạo lại màn hình Home nếu nó đã ở trên cùng
                launchSingleTop = true
            }
            // Reset lại state để không bị điều hướng lại khi có thay đổi cấu hình (vd: xoay màn hình)
            authViewModel.resetState()
        }
    }
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val current = backStackEntry?.destination?.route
            // Show bottom bar only on bottom destinations
            if (current in bottomNavItems.map { it.route }) {
                HomeBottomNavigation(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start.route,
            modifier = modifier.padding(innerPadding)
        ) {
            // Auth
            composable(LoginDestination.route) {
            LoginScreen(
                viewModel = authViewModel,
                onGoogleLoginClick = {
                    authViewModel.loginWithGoogle(activity = navController.context as Activity) {
                        launcher.launch(it)
                    }
                },
                onRegisterClick = { navController.navigate(RegisterDestination.route) },
                onLoginSuccess = {
//                    navController.navigate(HomeDestination.route) {
//                        popUpTo(LoginDestination.route) { inclusive = true }
//                    }
                }
            )
            }

            composable(RegisterDestination.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onBackToLogin = { navController.popBackStack() }
            )
            }

            // Main Tabs
            composable(HomeDestination.route) { HomeScreen(navController) }
            composable(MarketDestination.route) { PlaceholderScreen("Market") }
            composable(CreatePostDestination.route) { PlaceholderScreen("Create") }
            composable(NotificationsDestination.route) { PlaceholderScreen("Notifications") }
            composable(ProfileDestination.route) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                // Nếu chưa đăng nhập, quay về màn Login
                LoginDestination.route.let { navController.navigate(it) { popUpTo(0) } }
            } else {
                val vm: com.example.uth_socials.ui.viewmodel.ProfileViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return com.example.uth_socials.ui.viewmodel.ProfileViewModel(userId) as T
                        }
                    }
                )
                ProfileScreen(viewModel = vm)
            }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    androidx.compose.material3.Text(text = name)
}