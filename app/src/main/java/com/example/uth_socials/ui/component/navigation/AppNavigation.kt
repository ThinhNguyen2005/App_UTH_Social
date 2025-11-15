package com.example.uth_socials.ui.component.navigation

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.uth_socials.ui.screen.util.RegisterScreen
import com.example.uth_socials.ui.screen.util.ResetPasswordScreen
import com.example.uth_socials.ui.screen.util.AdminDashboardScreen
import com.example.uth_socials.ui.screen.chat.ChatListScreen
import com.example.uth_socials.ui.screen.chat.ChatScreen
import com.example.uth_socials.ui.screen.home.HomeScreen
import com.example.uth_socials.ui.screen.market.MarketScreen
import com.example.uth_socials.ui.screen.home.NotificationsScreen
import com.example.uth_socials.ui.screen.home.ProfileScreen
import com.example.uth_socials.ui.screen.market.ProductDetailScreen
import com.example.uth_socials.ui.screen.post.PostScreen
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.viewmodel.ProfileViewModel
import com.example.uth_socials.ui.viewmodel.ProfileViewModelFactory
import com.example.uth_socials.ui.viewmodel.BanStatusViewModel
import com.example.uth_socials.ui.component.common.BannedUserDialog
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import com.example.uth_socials.ui.screen.UserInfoScreen
import com.example.uth_socials.ui.screen.setting.UserSettingScreen

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
                },
                onRegisterSuccess = {
                    navController.navigate(Graph.MAIN){
                        popUpTo(Graph.AUTH) { inclusive = true }
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

// Điều hướng chính, tất cả đều hướng chỉ nên thay đổi và cập nhật trong MainScreen
fun NavGraphBuilder.mainNavGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Home.route,
        route = Graph.MAIN
    ) {
        composable(Screen.Home.route) {
            MainScreen(rootNavController = navController) // <-- THAY ĐỔI Ở ĐÂY
        }    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(rootNavController: NavHostController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val onLogout: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
        rootNavController.navigate(Graph.AUTH) {
            popUpTo(Graph.MAIN) { inclusive = true } // Xóa toàn bộ backstack của MAIN
        }
        Log.d("AppNavigation", "Logout triggered! Navigating to AUTH graph.")
    }
    val showBottomBar = when (currentRoute) {
        Screen.Home.route,
        Screen.Market.route,
        Screen.Add.route,
        Screen.Notifications.route,
        Screen.Profile.route -> true

        Screen.AdminDashboard.route,
        Screen.Categories.route -> false // Hide bottom bar for admin dashboard and categories
        Screen.ProductDetail.route -> false  // THÊM: Ẩn bottom bar cho ProductDetail
        //true là gì, là sẽ show là bottomBar fasle ngược lại khỏi
        // Nói chung là đừng đụng vào
        else -> false
    }

    Scaffold(
        topBar = {
            // Nếu đang ở Market -> không vẽ topBar
            if (currentRoute != Screen.Market.route) {
                when (currentRoute) {
                    Screen.Home.route -> HomeTopAppBar(
                        onSearchClick = { /* TODO */ },
                        onMessagesClick = { navController.navigate(Screen.ChatList.route) },
                        onAdminClick = {
                            navController.navigate(Screen.AdminDashboard.createRoute("reports")) {
                                launchSingleTop = true
                            }
                        }
                    )
            //topBar này thì được định nghĩa sẵn trong logo/HomeTopAppBar, logo/LogoTopAppBar
            //Biết sao không back được không, vì chưa composable cụ thể navback làm gì đó
            when (currentRoute) {
                Screen.Home.route -> HomeTopAppBar(
                    onSearchClick = { /* TODO: Điều hướng đến màn hình tìm kiếm */ }, // gắn on Search trong trang đó có gì gắn nav back chẳng hạn chưa đủ, xuống dưới định nghĩa composable nữa
                    onMessagesClick = { navController.navigate(Screen.ChatList.route) },
                    onAdminClick = {
                        navController.navigate(Screen.AdminDashboard.createRoute("reports")) {
                            launchSingleTop = true
                        }
                    }
                )

                    Screen.Add.route -> LogoTopAppBar()
                    Screen.Notifications.route -> LogoTopAppBar()
                    // các trường hợp khác nếu cần
                    else -> {}
                }
                Screen.Market.route -> LogoTopAppBar() // chỉ logo
                Screen.Add.route -> LogoTopAppBar()
                Screen.Notifications.route -> LogoTopAppBar()
                else -> { /* no app bar */ } // mấy trang không được định nghĩa thì không có logo UTH
            }
            // else: nothing -> Market không có topBar
        },
        bottomBar = {
            //Cái này coi tao show cái này ở những trang nào, định nghĩa ở trên
            if (showBottomBar) {
                var showBanDialog by remember { mutableStateOf(false) }
                val banStatusViewModel: BanStatusViewModel = viewModel()
                val banStatus by banStatusViewModel.banStatus.collectAsState()

                // Show ban dialog when ban status changes
                LaunchedEffect(banStatus.isBanned) {
                    if (banStatus.isBanned && currentRoute != Screen.Home.route) {
                        showBanDialog = true
                    }
                }

                HomeBottomNavigation(
                    navController = navController,
                    onBanDialogRequest = { showBanDialog = true }
                )

                // Ban dialog for navigation
                BannedUserDialog(
                    isVisible = showBanDialog,
                    banReason = banStatus.banReason,
                    onDismiss = { showBanDialog = false },
                    onLogout = onLogout
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            //Hướng dẫn sử dụng nha tạo route, rồi
            //Home nav này chuyển đến trang profile của người dùng khi nhấn vào tên
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToProfile = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = onLogout
                )
            }
            //Shop                            - Trang test
            composable(Screen.Market.route) { MarketScreen() }
            //Create post - product
            composable(
                Screen.Market.route,
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))
                }
            ) {
                MarketScreen(
                    navController = navController,
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    }
                )
            }

            // ===== THÊM ProductDetailScreen =====
            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(
                    navArgument("productId") { type = NavType.StringType }
                ),
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                }
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                ProductDetailScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onShare = { /* TODO: Implement share functionality */ },
                    onCall = { /* TODO: Implement call functionality */ },
                    onMessage = { /* TODO: Implement message functionality */ }
                )
            }

            composable(Screen.Add.route) { PostScreen(navController = navController) }
            //Notifications                 - Trang test
            composable(Screen.Notifications.route) { NotificationsScreen() }

            //Profile
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
                            profileViewModel.openChatWithUser(targetUserId) { chatId ->
                                navController.navigate(Screen.ChatDetail.createRoute(chatId))
                            }
                        },
                        onEditProfileClicked = {
                            navController.navigate(Screen.Setting.route)
                        }
                    )
                } else {
                    // Xử lý trường hợp không có userId (ví dụ: hiển thị lỗi hoặc quay lại)
                    Text("Lỗi: Không tìm thấy ID người dùng.")
                }
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
                ChatScreen(chatId = chatId,onBack = { navController.popBackStack()})
            }
            composable(Screen.Setting.route) {
                // ✅ SỬA LỖI: Truyền viewModel và hàm onLogout đã nhận được
                UserSettingScreen(
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
        }
    }
}

