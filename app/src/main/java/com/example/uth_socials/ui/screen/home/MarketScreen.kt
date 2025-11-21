package com.example.uth_socials.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.uth_socials.R
import com.example.uth_socials.ui.screen.market.ProductItem
import com.example.uth_socials.ui.screen.market.SearchBar
import com.example.uth_socials.ui.viewmodel.MarketViewModel

@Composable
fun MarketScreen(
    navController: NavHostController,
    viewModel: MarketViewModel = viewModel(),
    onProductClick: (String) -> Unit, //Điều hướng đến trang chi tiết.
) {
    // 1. Lấy state từ ViewModel - BAO GỒM cả danh sách đã filter
    val listUiState by viewModel.listUiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
    ) {
        //Header with gradient background
        Box(
            Modifier
                .height(239.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFB6FDFF),
                            Color(0xFF00F8FF)
                        )
                    )
                )
        ) {
            //Logo
            Image(
                painter = painterResource(R.drawable.lg_uth),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 160.dp, height = 38.dp)
                    .offset(x = 17.dp, y = 40.dp)
            )

            // Title + Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 106.dp)
            ) {
                Text(
                    text = "Trang bán hàng",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Mua thì hời, bán thì lời",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W400,
                    color = Color.DarkGray.copy(alpha = 0.6f)
                )
            }
        }

        // 2. Search Bar - KẾT NỐI VỚI VIEWMODEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp)
        ) {
            SearchBar(
                modifier = Modifier.align(Alignment.TopCenter),
                query = listUiState.searchQuery, // Lấy query từ state
                hint = "Tìm sản phẩm...",
                onQueryChange = { query ->
                    // Cập nhật query trong ViewModel (real-time search)
                    viewModel.updateSearchQuery(query)
                },
                onSearch = { query ->
                    // Optional: có thể thêm analytics hoặc log
                    // Search đã được thực hiện real-time ở onQueryChange
                },
                onClear = {
                    // Xóa search query
                    viewModel.clearSearch()
                }
            )
        }

        // 3. Hiển thị thông tin search
        if (listUiState.searchQuery.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-25).dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tìm thấy ${listUiState.filteredProducts.size} sản phẩm",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )

                // Hiển thị loại search
                Text(
                    text = if (listUiState.searchQuery.toDoubleOrNull() != null) {
                        "📊 Tìm theo giá"
                    } else {
                        "🔤 Tìm theo tên"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF00A8B0),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Products Grid - SỬ DỤNG filteredProducts thay vì products
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Loading state
            if (listUiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE5FEFF),
                                    Color(0xFF2CC3C9)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            // Error state
            else if (listUiState.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE5FEFF),
                                    Color(0xFF2CC3C9)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Lỗi: ${listUiState.error}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            // Empty state
            else if (listUiState.filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE5FEFF),
                                    Color(0xFF2CC3C9)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (listUiState.searchQuery.isEmpty()) {
                                "Chưa có sản phẩm nào"
                            } else {
                                "Không tìm thấy sản phẩm"
                            },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (listUiState.searchQuery.isNotEmpty()) {
                            Text(
                                text = "Không tìm thấy \"${listUiState.searchQuery}\"",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Thử tìm kiếm khác hoặc xóa bộ lọc",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            // Product list
            else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFE5FEFF),
                                        Color(0xFF2CC3C9)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(listUiState.filteredProducts.size) { index ->
                            val product = listUiState.filteredProducts[index]
                            val id = product.id
                            if (id != null) {
                                ProductItem(
                                    product = product,
                                    onClick = { onProductClick(id) }
                                )
                            } else {
                                ProductItem(product = product, onClick = { /* disabled */ })
                            }
                        }
                    }
                }
            }
        }
    }
}