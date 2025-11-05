package com.example.uth_socials.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.uth_socials.ui.screen.shop.PostProductScreen
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
        // ===== MÀN HÌNH DANH SÁCH SẢN PHẨM (ShopScreen) =====
        composable(route = "shop") {
            ShopScreen(
                onProductClick = { productId ->
                    // Điều hướng đến trang chi tiết với productId
                    navController.navigate("productDetail/$productId")
                },
                onPostClick = {
                    // Điều hướng đến trang thêm sản phẩm (không có productId)
                    navController.navigate("post")
                }
            )
        }

        // ===== MÀN HÌNH CHI TIẾT SẢN PHẨM (ProductDetailScreen) =====
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
                onEditProduct = { id ->
                    // Điều hướng đến trang sửa sản phẩm với productId
                    navController.navigate("post/$id")
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

        // ===== MÀN HÌNH THÊM/SỬA SẢN PHẨM (PostProductScreen) =====
        // Route không có tham số = Thêm mới
        composable(route = "post") {
            PostProductScreen(
                productId = null, // null = thêm mới
                onPostSuccess = {
                    // Sau khi thêm thành công, quay lại trang shop
                    navController.navigate("shop") {
                        // Xóa các trang trước đó khỏi back stack
                        popUpTo("shop") { inclusive = false }
                    }
                }
            )
        }

        // Route có tham số productId = Chỉnh sửa
        composable(
            route = "post/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            // Lấy productId từ route
            val productId = backStackEntry.arguments?.getString("productId")

            PostProductScreen(
                productId = productId, // có giá trị = chỉnh sửa
                onPostSuccess = {
                    // Sau khi sửa thành công, quay lại trang chi tiết
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Sử dụng trong Activity hoặc Fragment:
 *
 * @Composable
 * fun ShopApp() {
 *     val navController = rememberNavController()
 *
 *     ShopNavigation(
 *         navController = navController,
 *         startDestination = "shop"
 *     )
 * }
 */