package com.example.uth_socials.ui.component.navigation


// Các tuyến đường của các đồ thị
object Graph {
    const val AUTH = "auth_graph"
    const val MAIN = "main_graph"
}

// Các màn hình trong đồ thị chính (Main)
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Market : Screen("market")
    object Add : Screen("add")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object SearchResult : Screen("search_results/{query}")

    //Thêm product detail
    object ProductDetail : Screen("productDetail/{productId}") {
        fun createRoute(productId: String) = "productDetail/$productId"
    }

   //Thêm route màn hình phụ ở đây, ví dụ: Search, messenger
    object AdminDashboard : Screen("admin_dashboard/{tab}") {
        fun createRoute(tab: String = "reports") = "admin_dashboard/$tab"
    }
    sealed class AuthScreen(val route: String) {
        object Login : AuthScreen("login")
        object Register : AuthScreen("register")
        object ResetPassword : AuthScreen("reset_password")


    }

    object ChatList : Screen("chat_list")
    object ChatDetail : Screen("chat_detail/{chatId}") {
        fun createRoute(chatId: String) = "chat_detail/$chatId"
    }
    object Setting : Screen("setting")
    object UserInfoScreen : Screen("user_info")
    object BlockedUsers : Screen("blocked_users")
    object SavedPosts : Screen("saved_posts")
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    //Comment route
//    object Comments : Screen("comments/{postId}") {
//        fun createRoute(postId: String) = "comments/$postId"
//    }
}