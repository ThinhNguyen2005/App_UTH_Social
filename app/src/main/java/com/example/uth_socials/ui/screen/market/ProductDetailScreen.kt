package com.example.uth_socials.ui.screen.market

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.R
import com.example.uth_socials.data.shop.Product
import com.example.uth_socials.ui.viewmodel.ProductViewModel2
import androidx.compose.runtime.setValue

@Composable
fun ProductDetailScreen(
    productId: String?,
    onBack: () -> Unit = {},
    onShare: () -> Unit = {},
    onCall: () -> Unit = {},
    onMessage: () -> Unit = {},
    viewModel: ProductViewModel2 = viewModel(),
) {
    //Load san pham theo id
    LaunchedEffect(productId) {
        viewModel.getProductById(productId)
    }

    val detailState by viewModel.detailState.collectAsState()

    when {
        detailState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        detailState.error != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Có lỗi: ${detailState.error}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.getProductById(productId) }) {
                        Text("Thử lại")
                    }
                }
            }
        }
        detailState.product != null -> {
            ProductDetailContent(
                product = detailState.product!!,
                onBack = onBack,
                onShare = onShare,
                onCall = onCall,
                onMessage = onMessage,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ProductDetailContent(
    product: Product,
    onBack: () -> Unit,
    onShare: () -> Unit = {},
    onCall: () -> Unit = {},
    onMessage: () -> Unit = {},
    viewModel: ProductViewModel2
) {
    // State để theo dõi trạng thái yêu thích (chỉ local, không lưu database)
    var isFav by remember { mutableStateOf(product.favorite) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        //Nội dung
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp) //Để nội dung không bị che khi cuộn xuống cuối
        ) {
            //Image product
            item {
                AsyncImage(
                    model = product.imageUrl ?: R.drawable.default_image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .background(Color(0xFF262525))
                )
            }

            //Detail product
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = product.name.uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatVND(product.price),
                            fontSize = 20.sp,
                            color = Color(0xFFFF0000),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Đăng ${getRelativeTimeString(product.createdAt)}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    //Thông tin người bán - CHUA XONG
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.default_image),
                            contentDescription = "Avatar người bán",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.LightGray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = product.userId ?: "Người bán ẩn danh",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    //Description product
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1EEEE))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Mô tả chi tiết",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description ?: "Không có mô tả",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        //Thanh TOPBAR điều hướng
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 26.dp, start = 26.dp, end = 26.dp)
                .align(Alignment.TopCenter)
                .zIndex(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Left: Back icon
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.7f), CircleShape)
                    .padding(4.dp)
                    .size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }

            //Right: Share & Favorite icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {onShare()},
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.7f), CircleShape)
                        .padding(4.dp)
                        .size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = {isFav = !isFav},
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color.Red else Color.DarkGray
                    )
                }
            }
        }
        //BOTTOM BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.White)
        ) {
            Call_Chat(onCall = onCall, onMessage = onMessage)
        }
    }
}

//Btn Call & Chat
@Composable
fun Call_Chat(
    onCall: () -> Unit = {},
    onMessage: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCall,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6E0E9))
            ) {
                Text("Gọi",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onMessage,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E9397))
            ) {
                Text("Nhắn tin",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
