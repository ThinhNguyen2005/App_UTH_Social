package com.example.uth_socials.ui.component.navigation

import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.foundation.layout.navigationBars
import com.example.uth_socials.ui.viewmodel.NotificationViewModel
import com.example.uth_socials.ui.theme.UthTeal
import com.example.uth_socials.ui.viewmodel.BanStatusViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
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

    val isNotReadNotifications by notificationViewModel.isNotReadNotifications.collectAsState()

    val items = listOf(
        NavItem(Screen.Home.route, "Home", Icons.Rounded.Home, Icons.Outlined.Home, 0),
        NavItem(
            Screen.Market.route,
            "Market",
            Icons.Filled.ShoppingCart,
            Icons.Outlined.ShoppingCart,
            0
        ),
        NavItem(Screen.Add.route, "Add", Icons.Filled.AddCircle, Icons.Outlined.AddCircle, 0),
        NavItem(
            Screen.Notifications.route,
            "Alerts",
            Icons.Filled.Notifications,
            Icons.Outlined.Notifications,
            isNotReadNotifications.size
        ),
        NavItem(
            Screen.Profile.route,
            "Profile",
            Icons.Rounded.AccountCircle,
            Icons.Outlined.AccountCircle,
            0
        )
    )
    val currentUserIdState = remember { mutableStateOf<String?>(null) }
    val banStatusViewModel: BanStatusViewModel = viewModel()
    val banStatus by banStatusViewModel.banStatus.collectAsState()

    LaunchedEffect(Unit) {
        val userRepo = UserRepository()
        currentUserIdState.value = userRepo.getCurrentUserId()
    }
    val currentUserId = currentUserIdState.value
    val isUserBanned = banStatus.isBanned

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (isVisible) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
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