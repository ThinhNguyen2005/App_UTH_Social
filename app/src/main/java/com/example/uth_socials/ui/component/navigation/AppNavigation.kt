package com.example.uth_socials.ui.component.navigation

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.uth_socials.LoginScreen
import com.example.uth_socials.ui.component.logo.HomeTopAppBar
import com.example.uth_socials.ui.component.logo.LogoTopAppBar
import com.example.uth_socials.ui.screen.AdminDashboardScreen
import com.example.uth_socials.ui.screen.RegisterScreen
import com.example.uth_socials.ui.screen.ResetPasswordScreen
import com.example.uth_socials.ui.screen.UserInfoScreen
import com.example.uth_socials.ui.screen.chat.ChatListScreen
import com.example.uth_socials.ui.screen.chat.ChatScreen
import com.example.uth_socials.ui.screen.home.HomeScreen
import com.example.uth_socials.ui.screen.home.MarketScreen
import com.example.uth_socials.ui.screen.home.NotificationsScreen
import com.example.uth_socials.ui.screen.home.ProfileScreen
import com.example.uth_socials.ui.screen.post.PostScreen
import com.example.uth_socials.ui.screen.setting.UserSettingScreen
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.viewmodel.ProfileViewModel
import com.example.uth_socials.ui.viewmodel.ProfileViewModelFactory
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph(
    viewModel: AuthViewModel,
    launcher: ActivityResultLauncher<Intent>
) {
    val navController = rememberNavController()
    val startDestination = if (viewModel.isUserLoggedIn()) Graph.MAIN else Graph.AUTH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Đồ thị cho luồng xác thực (Login, Register, ...)
        authNavGraph(
            navController = navController,
            viewModel = viewModel,
            launcher = launcher,
        )
        // Đồ thị cho luồng chính của ứng dụng (Home, Profile, ...)
        // ✅ SỬA LỖI: Sửa tên tham số cho đúng và truyền vào
        mainNavGraph(
            rootNavController = navController,
            authViewModel = viewModel,
        )
    }
}

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    viewModel: AuthViewModel,
    launcher: ActivityResultLauncher<Intent>,
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
                },
                onRegisterSuccess = {
                    navController.navigate(Graph.MAIN){
                        popUpTo(Graph.AUTH){inclusive=true}
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
// ✅ SỬA LỖI: Sửa lại định nghĩa hàm cho đúng
fun NavGraphBuilder.mainNavGraph(
    rootNavController: NavHostController,
    authViewModel: AuthViewModel
) {
    navigation(
        startDestination = Screen.Home.route,
        route = Graph.MAIN
    ) {
        composable(Screen.Home.route) {
            // ✅ SỬA LỖI: Truyền ViewModel và logic đăng xuất vào MainScreen
            MainScreen(
                authViewModel = authViewModel,
                onLogout = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// ✅ SỬA LỖI: Sửa lại định nghĩa hàm MainScreen để nhận tham số
fun MainScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController() // NavController nội bộ cho các tab
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = when (currentRoute) {
        Screen.Home.route,
        Screen.Market.route,
        Screen.Add.route,
        Screen.Notifications.route,
        Screen.Profile.route -> true
        else -> false // Hiển thị mặc định, bạn có thể chỉnh lại nếu cần
    }
    Scaffold(
        topBar = {
            when (currentRoute) {
                Screen.Home.route -> HomeTopAppBar(
                    onSearchClick = { /* TODO: Điều hướng đến màn hình tìm kiếm */ },
                    onMessagesClick = { navController.navigate(Screen.ChatList.route) },
                    onAdminClick = {
                        navController.navigate(Screen.AdminDashboard.createRoute("reports")) {
                            launchSingleTop = true
                        }
                    }
                )

                Screen.Market.route,
                Screen.Add.route,
                Screen.Notifications.route -> LogoTopAppBar()
                // Không hiển thị TopAppBar cho các màn hình khác
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
                        navController.navigate(Screen.Profile.createRoute(userId))
                    }
                )
            }
            composable(Screen.Market.route) { MarketScreen() }
            composable(Screen.Add.route) { PostScreen(navController = navController) }
            composable(Screen.Notifications.route) { NotificationsScreen() }
            composable(
                route = Screen.AdminDashboard.route,
                arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "reports" })
            ) { backStackEntry ->
                AdminDashboardScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUser = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId))
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
                    val profileViewModel: ProfileViewModel = viewModel(key = "profile-$userId", factory = factory)
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onBackClicked = { navController.popBackStack() },
                        onMessageClicked = { targetUserId ->
                            profileViewModel.openChatWithUser(targetUserId) { chatId ->
                                navController.navigate(Screen.ChatDetail.createRoute(chatId))
                            }
                        },
                        onEditProfileClicked = {
                            navController.navigate(Screen.Setting.route)
                        }
                    )
                } else {
                    Text("Lỗi: Không tìm thấy ID người dùng.")
                }
            }
            composable(Screen.Setting.route) {
                // ✅ SỬA LỖI: Truyền viewModel và hàm onLogout đã nhận được
                UserSettingScreen(
                    viewModel = authViewModel,
                    onBackClicked = { navController.popBackStack() },
                    onNavigateToUserInfo = {
                        navController.navigate(Screen.UserInfoScreen.route)
                    },
                    onLogout = onLogout
                )
            }
            composable(Screen.UserInfoScreen.route) {
                UserInfoScreen(
                    onSaveSuccess = { navController.popBackStack() } // Quay lại sau khi lưu
                )
            }
            composable(Screen.ChatList.route) {
                ChatListScreen(
                    onChatSelected = { chatId ->
                        navController.navigate(Screen.ChatDetail.createRoute(chatId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ChatDetail.route) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                ChatScreen(chatId = chatId, onBack = { navController.popBackStack() })
            }
        }
    }
}