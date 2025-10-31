package com.example.uth_socials.ui.component.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navOptions
import com.example.uth_socials.ui.navigation.AppDestination
import com.example.uth_socials.ui.navigation.bottomNavItems
import com.example.uth_socials.ui.navigation.toIcon

@Composable
fun HomeBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // Lấy destination hiện tại một cách an toàn
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .height(70.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        // Lặp qua danh sách các destination đã định nghĩa sẵn
        bottomNavItems.forEach { destination ->
            // So sánh an toàn bằng cách sử dụng route được tạo ra từ kotlin serialization
            val isSelected = currentDestination?.route == destination.route

            // Lấy cặp icon (selected/unselected) từ hàm tiện ích
            val (selectedIcon, unselectedIcon) = destination.toIcon()

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(destination.route, navOptions {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        })
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) selectedIcon else unselectedIcon,
                        contentDescription = destination::class.simpleName,
                        modifier = Modifier.size(28.dp)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.DarkGray,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                ),
                alwaysShowLabel = false
            )
        }
    }
}