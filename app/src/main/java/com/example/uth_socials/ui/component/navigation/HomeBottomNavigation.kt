package com.example.uth_socials.ui.component.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.uth_socials.data.repository.UserRepository
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import com.example.uth_socials.ui.viewmodel.NotificationViewModel
import com.example.uth_socials.ui.theme.UthTeal
import com.example.uth_socials.ui.viewmodel.BanStatusViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBottomNavigation(
    navController: NavController,
    onBanDialogRequest: (() -> Unit)? = null,
    notificationViewModel: NotificationViewModel,
    isVisible: Boolean = true
) {
    data class NavItem(
        val route: String,
        val label: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector,
        val badgeCount: Int
    )

    val isNotReadNotifications by notificationViewModel.isNotReadNotifications.collectAsStateWithLifecycle()

    val items = listOf(
        NavItem(Screen.Home.route, "Trang chủ", Icons.Filled.Home, Icons.Outlined.Home, 0),
        NavItem(
            Screen.Market.route,
            "Chợ UTH",
            Icons.Filled.Storefront,
            Icons.Outlined.Storefront,
            0
        ),
        NavItem(Screen.Add.route, "Đăng bài", Icons.Filled.AddCircle, Icons.Outlined.AddCircle, 0),
        NavItem(
            Screen.Notifications.route,
            "Thông báo",
            Icons.Filled.Notifications,
            Icons.Outlined.NotificationsNone,
            isNotReadNotifications.size
        ),
        NavItem(
            Screen.Profile.route,
            "Cá nhân",
            Icons.Filled.Person,
            Icons.Outlined.Person,
            0
        )
    )
    val currentUserIdState = remember { mutableStateOf<String?>(null) }
    val banStatusViewModel: BanStatusViewModel = viewModel()
    val banStatus by banStatusViewModel.banStatus.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val userRepo = UserRepository()
        currentUserIdState.value = userRepo.getCurrentUserId()
    }
    val currentUserId = currentUserIdState.value
    val isUserBanned = banStatus.isBanned

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (isVisible) {
        NavigationBar(
            modifier = Modifier
                .shadow(elevation = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationBarItem(
                    icon = {
                        BadgedBox(badge = {
                            if (item.badgeCount > 0) {
                                Badge { Text(text = item.badgeCount.toString()) }
                            }
                        }) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    selected = isSelected,

                    onClick = {
                        // Check ban status trước khi navigate (trừ Home route)
                        if (isUserBanned && item.route != Screen.Home.route) {
                            onBanDialogRequest?.invoke()
                            return@NavigationBarItem
                        }

                        val destinationRoute = if (item.route.contains("{userId}")) {
                            currentUserId?.let { Screen.Profile.createRoute(it) }
                                ?: return@NavigationBarItem
                        } else {
                            item.route
                        }

                        navController.navigate(destinationRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = UthTeal,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = UthTeal,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}