//package com.example.uth_socials.ui.component.navigation
//
//import android.app.Activity
//import android.content.Intent
//import androidx.activity.result.ActivityResultLauncher
//import androidx.compose.runtime.Composable
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.example.uth_socials.LoginScreen
//import com.example.uth_socials.ui.screen.RegisterScreen
//import com.example.uth_socials.ui.screen.ResetPasswordScreen
//import com.example.uth_socials.ui.screen.home.HomeScreen
//import com.example.uth_socials.ui.viewmodel.AuthViewModel
//
//
////sealed class Screen(val route: String) {
////    object Home : Screen("home")
////    object Market : Screen("market")
////    object Add : Screen("add")
////    object Notifications : Screen("notifications")
////    object Profile : Screen("profile")
////}
//@Composable
//fun AuthNav(
//    viewModel: AuthViewModel,
//    launcher: ActivityResultLauncher<Intent>
//) {
//    val navController = rememberNavController()
//    val startDestination = if (viewModel.isUserLoggedIn()) "home" else "login"
//    NavHost(navController = navController, startDestination = startDestination) {
//        composable("login") {
//            LoginScreen(
//                viewModel = viewModel,
//                onGoogleLoginClick = {
//                    viewModel.loginWithGoogle(activity = navController.context as Activity) {
//                        launcher.launch(it)
//                    }
//                },
//                onRegisterClick = { navController.navigate("register") },
//                onLoginSuccess = {
//                    navController.navigate("home") {
//                        popUpTo("login") { inclusive = true }
//                    }
//                },
//                onResetPasswordClick = { navController.navigate("reset_password") }
//            )
//        }
//
//        composable("register") {
//            RegisterScreen(
//                viewModel = viewModel,
//                onBackToLogin = { navController.popBackStack() },
//                onGoogleClick = {
//                    viewModel.loginWithGoogle(activity = navController.context as Activity) {
//                        launcher.launch(it)
//                    }
//                }
//            )
//        }
//
//        composable("reset_password") {
//            ResetPasswordScreen(
//                viewModel = viewModel,
//                onBackToLogin = { navController.popBackStack() },
//                onGoogleClick = {
//                    viewModel.loginWithGoogle(activity = navController.context as Activity) {
//                        launcher.launch(it)
//                    }
//                }
//            )
//        }
//
//        composable("home") {
//            HomeScreen()
//        }
////        composable ()
//
//    }
//}