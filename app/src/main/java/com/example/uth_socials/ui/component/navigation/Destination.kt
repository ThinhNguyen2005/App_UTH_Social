package com.example.uth_socials.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// ✅ Dùng sealed interface làm gốc cho tất cả các destination
@Serializable
sealed interface AppDestination { val route: String }

// ✅ Định nghĩa TẤT CẢ các màn hình ở đây
@Serializable
data object LoginDestination : AppDestination { override val route: String = "login" }

@Serializable
data object RegisterDestination : AppDestination { override val route: String = "register" }
@Serializable
data object HomeDestination : AppDestination { override val route: String = "home" }

@Serializable
data object MarketDestination : AppDestination { override val route: String = "market" }
@Serializable
data object CreatePostDestination : AppDestination { override val route: String = "create" }
@Serializable
data object NotificationsDestination : AppDestination { override val route: String = "notifications" }

@Serializable
data object ProfileDestination : AppDestination { override val route: String = "profile" }

// ✅ "Nguồn chân lý" cho bottom bar: chỉ các tab cần thiết hiện tại
val bottomNavItems = listOf(
    HomeDestination,
    MarketDestination,
    CreatePostDestination,
    NotificationsDestination,
    ProfileDestination
)

// ✅ Tiện ích để map Destination sang Icon, logic UI nằm ở đây
fun AppDestination.toIcon(): Pair<ImageVector, ImageVector> {
    return when (this) {
        HomeDestination -> Icons.Rounded.Home to Icons.Outlined.Home
        MarketDestination -> Icons.Rounded.Storefront to Icons.Outlined.Storefront
        CreatePostDestination -> Icons.Rounded.AddBox to Icons.Outlined.AddBox
        NotificationsDestination -> Icons.Rounded.Notifications to Icons.Outlined.Notifications
        ProfileDestination -> Icons.Rounded.AccountCircle to Icons.Outlined.AccountCircle
        else -> Icons.Rounded.Error to Icons.Outlined.Error // Mặc định
    }
}