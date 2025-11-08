package com.example.uth_socials.ui.component.navigation

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.uth_socials.LoginScreen
import com.example.uth_socials.ui.component.logo.HomeTopAppBar
import com.example.uth_socials.ui.component.logo.LogoTopAppBar
import com.example.uth_socials.ui.screen.RegisterScreen
import com.example.uth_socials.ui.screen.ResetPasswordScreen
import com.example.uth_socials.ui.screen.AdminDashboardScreen
import com.example.uth_socials.ui.screen.home.HomeScreen
import com.example.uth_socials.ui.screen.home.MarketScreen
import com.example.uth_socials.ui.screen.home.NotificationsScreen
import com.example.uth_socials.ui.screen.home.ProfileScreen
import com.example.uth_socials.ui.screen.post.PostScreen
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.viewmodel.ProfileViewModel
import com.example.uth_socials.ui.viewmodel.ProfileViewModelFactory

@Composable
fun AppNavGraph(
    viewModel: AuthViewModel,
    launcher: ActivityResultLauncher<Intent>
) {
    val navController = rememberNavController()

    // Dựa vào trạng thái đăng nhập để chọn đồ thị bắt đầu
    val startDestination = if (viewModel.isUserLoggedIn()) Graph.MAIN else Graph.AUTH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Đồ thị cho luồng xác thực (Login, Register, ...)
        authNavGraph(
            navController = navController,
            viewModel = viewModel,
            launcher = launcher
        )

        // Đồ thị cho luồng chính của ứng dụng (Home, Profile, ...)
        mainNavGraph(navController = navController)
    }
}

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    viewModel: AuthViewModel,
    launcher: ActivityResultLauncher<Intent>
) {
    navigation(
        startDestination = Screen.AuthScreen.Login.route,
        route = Graph.AUTH
    ) {
        composable(Screen.AuthScreen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onGoogleLoginClick = {
                    viewModel.loginWithGoogle(activity = navController.context as Activity) {
                        launcher.launch(it)
                    }
                },
                onRegisterClick = { navController.navigate(Screen.AuthScreen.Register.route) },
                onLoginSuccess = {
                    // Chuyển sang đồ thị chính và xóa đồ thị auth khỏi back stack
                    navController.navigate(Graph.MAIN) {
                        popUpTo(Graph.AUTH) { inclusive = true }
                    }
                },
                onResetPasswordClick = { navController.navigate(Screen.AuthScreen.ResetPassword.route) }
            )
        }
        composable(Screen.AuthScreen.Register.route) {
            RegisterScreen(
                viewModel = viewModel,
                onBackToLogin = { navController.popBackStack() },
                onGoogleClick = {
                    viewModel.loginWithGoogle(activity = navController.context as Activity) {
                        launcher.launch(it)
                    }
                }
            )
        }
        composable(Screen.AuthScreen.ResetPassword.route) {
            ResetPasswordScreen(
                viewModel = viewModel,
                onBackToLogin = { navController.popBackStack() },
                onGoogleClick = {
                    viewModel.loginWithGoogle(activity = navController.context as Activity) {
                        launcher.launch(it)
                    }
                }
            )
        }
    }
}

// ===== ĐỒ THỊ CON CHO CÁC MÀN HÌNH CHÍNH =====
fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Home.route,
        route = Graph.MAIN
    ) {
        // Tất cả các màn hình bên trong MainScreen đều được định nghĩa ở đây
        // Điều này cho phép điều hướng từ một tab này sang một màn hình chi tiết khác.
        composable(Screen.Home.route) { MainScreen() } // Chỉ cần gọi MainScreen ở đây
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = when (currentRoute) {
        Screen.Home.route,
        Screen.Market.route,
        Screen.Add.route,
        Screen.Notifications.route,
        Screen.Profile.route -> true

        Screen.AdminDashboard.route,
        Screen.Categories.route -> false // Hide bottom bar for admin dashboard and categories
        else -> false
    }
    Scaffold(
        topBar = {
            when (currentRoute) {
                Screen.Home.route -> HomeTopAppBar(
                    onSearchClick = { /* TODO: Điều hướng đến màn hình tìm kiếm */ },
                    onMessagesClick = { /* TODO: Điều hướng đến màn hình tin nhắn */ },
                    onAdminClick = {
                        navController.navigate(Screen.AdminDashboard.createRoute("reports")) {
                            launchSingleTop = true
                        }
                    }
                )

                Screen.Market.route -> LogoTopAppBar()
                Screen.Add.route -> LogoTopAppBar()
                Screen.Notifications.route -> LogoTopAppBar()
                else -> { /* no app bar */
                }
            }

        },
        bottomBar = {
            if (showBottomBar) {
                HomeBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToProfile = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Market.route) { MarketScreen() }
            composable(Screen.Add.route) { PostScreen(navController = navController) }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(Screen.Categories.route) {
                AdminDashboardScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUser = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = Screen.AdminDashboard.route,
                arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "reports" })
            ) { backStackEntry ->
                AdminDashboardScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUser = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    },
                    backStackEntry = backStackEntry
                )
            }
            composable(
                route = Screen.Profile.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                if (userId != null) {
                    val factory = ProfileViewModelFactory(userId = userId)
                    val profileViewModel: ProfileViewModel = viewModel(
                        key = "profile-$userId",
                        factory = factory
                    )
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onBackClicked = { navController.popBackStack() },
                        onMessageClicked = { targetUserId ->
                            // TODO: Điều hướng đến màn hình chat với user này
                            Log.d("AppNavigation", "Message user: $targetUserId")
                        }
                    )
                } else {
                    // Xử lý trường hợp không có userId (ví dụ: hiển thị lỗi hoặc quay lại)
                    Text("Lỗi: Không tìm thấy ID người dùng.")
                }
            }
        }
    }
}

