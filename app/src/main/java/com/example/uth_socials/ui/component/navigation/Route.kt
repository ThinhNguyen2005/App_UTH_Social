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
    object AdminDashboard : Screen("admin_dashboard/{tab}") {
        fun createRoute(tab: String = "reports") = "admin_dashboard/$tab"
    }
    object Categories : Screen("categories")

    // Các màn hình trong đồ thị xác thực (Auth)
    sealed class AuthScreen(val route: String) {
        object Login : AuthScreen("login")
        object Register : AuthScreen("register")
        object ResetPassword : AuthScreen("reset_password")
    }
}