package com.example.uth_socials.ui.component.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
//import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uth_socials.ui.screen.UthTeal
import com.example.uth_socials.R


@Composable
fun HomeBottomNavigation() {
    var selectedItem by remember { mutableStateOf("home") }
    data class NavItem(
        val route: String,
        val selectedIcon: Any,
        val unselectedIcon: Any,
        val badgeCount: Int,

        )

    val items = listOf(
        NavItem("home", R.drawable.ic_home5, R.drawable.ic_home4,0),
        NavItem("market", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart,0),
        NavItem("add", R.drawable.ic_create1, R.drawable.ic_create,0),
        NavItem("notifications", Icons.Filled.Notifications, Icons.Outlined.Notifications, 0),
        NavItem("profile", Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle, 0)
    )


    NavigationBar(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .height(70.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = selectedItem == item.route
            val iconToShow = if (isSelected) item.selectedIcon else item.unselectedIcon

            NavigationBarItem(
                icon = {
                    when (iconToShow) {
                        is ImageVector -> Icon(
                            imageVector = iconToShow,
                            contentDescription = item.route,
                            tint = if (isSelected) UthTeal else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )

                        is Int -> Icon(
                            painter = painterResource(id = iconToShow),
                            contentDescription = item.route,
                            tint = if (isSelected) UthTeal else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                selected = isSelected,
                onClick = { selectedItem = item.route },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = UthTeal,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = Color.Transparent
                ),
                label = null
            )
        }
    }

}


@Preview
@Composable
fun HomeBottomNavigationPreview() {
    HomeBottomNavigation()
}