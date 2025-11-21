package com.example.uth_socials.ui.component.navigation

import SearchResultScreen
import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.example.uth_socials.ui.screen.home.MarketScreen
import com.example.uth_socials.ui.screen.home.NotificationsScreen
import com.example.uth_socials.ui.screen.profile.ProfileScreen
import com.example.uth_socials.ui.screen.market.ProductDetailScreen
import com.example.uth_socials.ui.screen.post.PostScreen
//import com.example.uth_socials.ui.screen.search.SearchScreen
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.viewmodel.PostViewModel
import com.example.uth_socials.ui.viewmodel.ProductViewModel
import com.example.uth_socials.ui.viewmodel.ProfileViewModel
import com.example.uth_socials.ui.viewmodel.ProfileViewModelFactory
import com.example.uth_socials.ui.viewmodel.BanStatusViewModel
import com.example.uth_socials.ui.component.common.BannedUserDialog
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import com.example.uth_socials.ui.screen.UserInfoScreen
import com.example.uth_socials.ui.screen.setting.UserSettingScreen
import com.example.uth_socials.ui.viewmodel.SearchViewModel
import com.example.uth_socials.ui.viewmodel.NotificationViewModel
import com.example.uth_socials.ui.screen.setting.BlockedUsersScreen
import com.example.uth_socials.ui.viewmodel.HomeViewModel
import com.example.uth_socials.ui.screen.saved.SavedPostsScreen
import com.example.uth_socials.ui.screen.saved.SavedPostDetail
import com.example.uth_socials.ui.screen.setting.FollowListScreen
import com.example.uth_socials.ui.viewmodel.FollowListType
import com.example.uth_socials.ui.viewmodel.FollowListViewModel
import com.example.uth_socials.ui.viewmodel.FollowListViewModelFactory
import com.example.uth_socials.ui.screen.util.HelloUserScreen
import com.example.uth_socials.ui.screen.util.WelcomeScreen

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
        mainNavGraph(navController = navController,authViewModel=viewModel)
    }
}

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    viewModel: AuthViewModel,
    launcher: ActivityResultLauncher<Intent>
) {
    navigation(
        startDestination = Screen.AuthScreen.HelloUser.route,
        route = Graph.AUTH
    ) {
        composable (Screen.AuthScreen.HelloUser.route){
            HelloUserScreen(
                onStartClicked = {
                    navController.navigate(Screen.AuthScreen.WelcomeUser.route) {
                        popUpTo(Screen.AuthScreen.HelloUser.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AuthScreen.WelcomeUser.route) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Screen.AuthScreen.Login.route) },
                onRegisterClick = { navController.navigate(Screen.AuthScreen.Register.route) }
            )
        }


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
                    navController.navigate(Graph.MAIN) {
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
                },
            )
        }
    }
}

// Điều hướng chính, tất cả đều hướng chỉ nên thay đổi và cập nhật trong MainScreen
fun NavGraphBuilder.mainNavGraph(navController: NavHostController,authViewModel: AuthViewModel) {
    navigation(
        startDestination = Screen.Home.route,
        route = Graph.MAIN
    ) {
        composable(Screen.Home.route) {

            MainScreen(rootNavController = navController,authViewModel=authViewModel) // <-- THAY ĐỔI Ở ĐÂY

        }    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(rootNavController: NavHostController,authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Scroll state for top/bottom bar visibility
    var isScrollingUp by remember { mutableStateOf(false) }
    var isAtTop by remember { mutableStateOf(true) }

    val onLogout: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
        rootNavController.navigate(Graph.AUTH) {
            popUpTo(Graph.MAIN) { inclusive = true } // Xóa toàn bộ backstack của MAIN
        }
        Log.d("AppNavigation", "Logout triggered! Navigating to AUTH graph.")
    }

   // val notificationViewModel : NotificationViewModel = viewModel()

    val showBottomBar = when (currentRoute) {
        Screen.Home.route,
        Screen.Market.route,
        Screen.Add.route,
        Screen.Notifications.route,
        Screen.Profile.route -> true

        Screen.AdminDashboard.route,
//        Screen.Categories.route -> false // Hide bottom bar for admin dashboard and categories
        Screen.ProductDetail.route -> false  // THÊM: Ẩn bottom bar cho ProductDetail
        //true là gì, là sẽ show là bottomBar fasle ngược lại khỏi
        // Nói chung là đừng đụng vào
        else -> false
    }

    // Top app bar scroll behavior for collapsing/expanding
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Determine if bottom bar should be visible based on scroll state (only for Home screen)
    val shouldShowBottomBar = showBottomBar && (currentRoute != Screen.Home.route || isAtTop || isScrollingUp)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Nếu đang ở Market -> không vẽ topBar
            if (currentRoute != Screen.Market.route) {
                when (currentRoute) {
                    Screen.Home.route -> HomeTopAppBar(
                        onSearchClick = { query ->
                            navController.navigate("search_results/$query")
                        },
                        onMessagesClick = { navController.navigate(Screen.ChatList.route) },
                        onAdminClick = {
                            navController.navigate(Screen.AdminDashboard.createRoute("reports")) {
                                launchSingleTop = true
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                    Screen.Add.route -> LogoTopAppBar()
                    Screen.Notifications.route -> LogoTopAppBar()
                    Screen.SearchResult.route -> HomeTopAppBar(
                        onSearchClick = { query ->
                            navController.navigate("search_results/$query")
                        },
                        onMessagesClick = { navController.navigate(Screen.ChatList.route) },
                        onAdminClick = {
                            navController.navigate(Screen.AdminDashboard.createRoute("reports")) {
                                launchSingleTop = true
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                    else -> { /* no app bar */ } // mấy trang không được định nghĩa thì không có logo UTH
                }
                // else: nothing -> Market không có topBar
            }
        },
        bottomBar = {
            //Cái này coi tao show cái này ở những trang nào, định nghĩa ở trên
            if (shouldShowBottomBar) {
                var showBanDialog by remember { mutableStateOf(false) }
                val banStatusViewModel: BanStatusViewModel = viewModel()
                val banStatus by banStatusViewModel.banStatus.collectAsState()

                // Show ban dialog when ban status changes
                LaunchedEffect(banStatus.isBanned) {
                    if (banStatus.isBanned && currentRoute != Screen.Home.route) {
                        showBanDialog = true
                    }
                }

                val notificationViewModel : NotificationViewModel = viewModel()


                HomeBottomNavigation(
                    navController = navController,
                    onBanDialogRequest = { showBanDialog = true },
                    notificationViewModel = notificationViewModel,
                    isVisible = true
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
                    onLogout = onLogout,
                    onScrollStateChanged = { isScrollingUpState, isAtTopState ->
                        isScrollingUp = isScrollingUpState
                        isAtTop = isAtTopState
                    },
                    scrollBehavior = scrollBehavior
                )
            }
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
            //Market
            composable(Screen.Market.route) { MarketScreen(
                navController = navController,
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                }
            ) }
            composable(Screen.Add.route) {
                val postViewModel : PostViewModel = viewModel()
                val productViewModel : ProductViewModel = viewModel()
                PostScreen(postViewModel,productViewModel,navController = navController)
            }
            composable(Screen.Notifications.route) {
                val notificationViewModel : NotificationViewModel = viewModel()
                NotificationsScreen(notificationViewModel,navController)
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
            //Create post - product
            composable(Screen.Add.route) {
                val postViewModel : PostViewModel = viewModel()
                val productViewModel : ProductViewModel = viewModel()
                PostScreen(postViewModel,productViewModel,navController = navController)
            }
            //Notifications                 - Trang test
            composable(Screen.Notifications.route) {
                val notificationViewModel : NotificationViewModel = viewModel()
                NotificationsScreen(notificationViewModel,navController)
            }

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

                    val homeViewModel: HomeViewModel = viewModel()
                    ProfileScreen(
                        viewModel = profileViewModel,
                        homeViewModel = homeViewModel,
                        onBackClicked = { navController.popBackStack() },
                        onMessageClicked = { targetUserId ->
                            profileViewModel.openChatWithUser(targetUserId) { chatId ->
                                navController.navigate(Screen.ChatDetail.createRoute(chatId))
                            }
                        },
                        onSettingClicked = {
                            navController.navigate(Screen.Setting.route)
                        },
                        onProductClick = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId))
                        }
                    )
                } else {
                    Text("Lỗi: Không tìm thấy ID người dùng.")
                }
            }

            composable(Screen.SearchResult.route) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query") ?: ""
                val searchViewModel : SearchViewModel = viewModel()
                searchViewModel.searchPosts(query)
                searchViewModel.searchUsers(query)
                SearchResultScreen(searchViewModel, navController)
            }

            composable(
                route = Screen.AdminDashboard.route,
                arguments = listOf(navArgument("tab") {
                    type = NavType.StringType; defaultValue = "reports"
                })
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
                ChatScreen(chatId = chatId, onBack = { navController.popBackStack() })
            }
            composable(Screen.Setting.route) {
                val currentUserId = remember {
                    FirebaseAuth.getInstance().currentUser?.uid ?: ""
                }
                UserSettingScreen(
                    authViewModel=authViewModel,
                    onBackClicked = { navController.popBackStack() },
                    onNavigateToUserInfo = {
                        navController.navigate(Screen.UserInfoScreen.route)
                    },
                    onNavigateToBlockedUsers = {
                        Log.d("AppNavigation", "Navigating to BlockedUsers: ${Screen.BlockedUsers.route}")
                        try {
                            navController.navigate(Screen.BlockedUsers.route) {
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            Log.e("AppNavigation", "Error navigating to BlockedUsers", e)
                        }
                    },
                    onNavigateToSavedPosts = {
                        navController.navigate(Screen.SavedPosts.route)
                    },
                    //Navigation đến Followers
                    onNavigateToFollowers = {
                        navController.navigate(Screen.FollowersList.createRoute(currentUserId)) {
                            launchSingleTop = true
                        }
                    },
                    //Navigation đến Following
                    onNavigateToFollowing = {
                        navController.navigate(Screen.FollowingList.createRoute(currentUserId)) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = onLogout
                )
            }
            composable(Screen.UserInfoScreen.route) {
                UserInfoScreen(

                    onSaveSuccess = { navController.popBackStack() },
                    onBackClicked = { navController.popBackStack() }

                )
            }
            composable(Screen.BlockedUsers.route) {
                val homeViewModel: HomeViewModel = viewModel()
                BlockedUsersScreen(
                    onBackClicked = { navController.popBackStack() },
                    onUserUnblocked = {
                        homeViewModel.refreshBlockedUsers()
                    }
                )
            }
            // Màn hình Followers List
            composable(
                route = Screen.FollowersList.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val factory = FollowListViewModelFactory(userId, FollowListType.FOLLOWERS)
                val viewModel: FollowListViewModel = viewModel(
                    key = "followers-$userId",
                    factory = factory
                )

                FollowListScreen(
                    viewModel = viewModel,
                    onBackClicked = { navController.popBackStack() },
                    onUserClicked = { clickedUserId ->
                        navController.navigate(Screen.Profile.createRoute(clickedUserId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Màn hình Following List
            composable(
                route = Screen.FollowingList.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val factory = FollowListViewModelFactory(userId, FollowListType.FOLLOWING)
                val viewModel: FollowListViewModel = viewModel(
                    key = "following-$userId",
                    factory = factory
                )

                FollowListScreen(
                    viewModel = viewModel,
                    onBackClicked = { navController.popBackStack() },
                    onUserClicked = { clickedUserId ->
                        navController.navigate(Screen.Profile.createRoute(clickedUserId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.SavedPosts.route) {
                SavedPostsScreen(
                    onBackClicked = { navController.popBackStack() },
                    onPostClick = { postId ->
                        //Navigate to post detail
                        navController.navigate(Screen.PostDetail.createRoute(postId))
                    },
                    onUserClick = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    },
                    //Navigate to comments from grid
//                    onCommentClicked = { postId ->
//                        navController.navigate(Screen.Comments.createRoute(postId))
//                    }
                )
            }
            composable(
                route = Screen.PostDetail.route,
                arguments = listOf(
                    navArgument("postId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                SavedPostDetail(
                    postId = postId,
                    onBackClicked = { navController.popBackStack() },
                    onUserClick = { userId ->
                        navController.navigate(Screen.Profile.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    },
                    //Navigate to comments from detail
//                    onCommentClicked = { postId ->
//                        navController.navigate(Screen.Comments.createRoute(postId))
//                    }
                )
            }
            //Comment composable
//            composable(
//                route = Screen.Comments.route,
//                arguments = listOf(
//                    navArgument("postId") { type = NavType.StringType }
//                )
//            ) { backStackEntry ->
//                val postId = backStackEntry.arguments?.getString("postId") ?: ""
//                CommentsScreen(
//                    postId = postId,
//                    onBackClicked = { navController.popBackStack() }
//                )
//            }
        }
    }
}

