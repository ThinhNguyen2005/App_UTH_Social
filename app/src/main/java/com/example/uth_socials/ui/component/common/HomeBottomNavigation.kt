package com.example.uth_socials.ui.component.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeBottomNavigation() {
    // Trong một ứng dụng thật, state này sẽ được quản lý bởi Navigation Controller
    var selectedItem by remember { mutableStateOf("home") }

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Trang chủ") },
            selected = selectedItem == "home",
            onClick = { selectedItem = "home" },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Market") },
            label = { Text("Market") },
            selected = selectedItem == "market",
            onClick = { selectedItem = "market" },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Add, contentDescription = "Add Post") },
            label = { Text("Đăng bài") },
            selected = selectedItem == "add",
            onClick = { selectedItem = "add" } ,
            alwaysShowLabel = false
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Notifications, contentDescription = "Notifications") },
            label = { Text("Thông báo") },
            selected = selectedItem == "notifications",
            onClick = { selectedItem = "notifications" },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            // Tạm thời dùng icon, sau này sẽ thay bằng ảnh avatar thật
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text("Cá nhân") },
            selected = selectedItem == "profile",
            onClick = { selectedItem = "profile" },
            alwaysShowLabel = false
        )
    }
}

@Preview
@Composable
fun HomeBottomNavigationPreview() {
    HomeBottomNavigation()
}