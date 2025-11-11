package com.example.uth_socials.ui.component.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.uth_socials.ui.screen.shop.ProductDetailScreen
import com.example.uth_socials.ui.screen.shop.ShopScreen

/**
 * Navigation Host cho Shop feature
 * Định nghĩa các route và cách điều hướng giữa các màn hình
 */
@Composable
fun ShopNavigation(
    navController: NavHostController,
    startDestination: String = "shop"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ===== ShopScreen =====
        composable(
            route = "shop",
            exitTransition = {
                slideOutHorizontally (
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            popEnterTransition = {
                slideInHorizontally (
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300))
            }
        ) {
            ShopScreen(
                onProductClick = { productId ->
                    navController.navigate("productDetail/$productId")
                }
            )
        }

        // ===== ProductDetailScreen =====
        // Route có tham số productId
        composable(
            route = "productDetail/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            // Lấy productId từ route
            val productId = backStackEntry.arguments?.getString("productId")

            ProductDetailScreen(
                productId = productId,
                onBack = {
                    // Quay lại trang trước
                    navController.popBackStack()
                },
                onShare = {
                    // TODO: Implement share functionality
                },
                onCall = {
                    // TODO: Implement call functionality
                },
                onMessage = {
                    // TODO: Implement message functionality
                }
            )
        }
    }
}