package com.example.uth_socials.ui.screen.market

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.R
import com.example.uth_socials.data.market.Product
import com.example.uth_socials.data.user.User
import com.example.uth_socials.ui.component.common.CallConfirmDialog
import com.example.uth_socials.ui.viewmodel.MarketViewModel

@Composable
fun ProductDetailScreen(
    productId: String?,
    onBack: () -> Unit = {},
    onShare: () -> Unit = {},
    onCall: () -> Unit = {},
    onMessage: (String) -> Unit = {},
    viewModel: MarketViewModel = viewModel(),
) {
    val context = LocalContext.current

    //Load san pham theo id
    LaunchedEffect(productId) {
        viewModel.getProductById(productId)
    }

    val detailState by viewModel.detailState.collectAsState()

    //State để hiển thị dialog gọi điện
    var showCallDialog by remember { mutableStateOf(false) }

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
                seller = detailState.seller,
                onBack = onBack,
                onShare = onShare,
                onCall = {
                    //Kiểm tra số điện thoại trước khi hiển thị dialog
                    val phoneNumber = detailState.seller?.phone
                    if (!phoneNumber.isNullOrEmpty()) {
                        showCallDialog = true
                    } else {
                        //Hiển thị thông báo không có số điện thoại
                        Toast.makeText(
                            context,
                            "Người bán chưa cập nhật số điện thoại",
                            Toast.LENGTH_SHORT).show()
                    }
                },
                onMessage = onMessage,
                viewModel = viewModel
            )
            //Hiển thị dialog xác nhận gọi điện
            CallConfirmDialog(
                isVisible = showCallDialog,
                phoneNumber = detailState.seller?.phone ?: "",
                sellerName = detailState.seller?.username ?: "Người bán",
                onDismiss = { showCallDialog = false }
            )
        }
    }
}

@Composable
fun ProductDetailContent(
    product: Product,
    seller: User?,
    onBack: () -> Unit,
    onShare: () -> Unit = {},
    onCall: () -> Unit = {},
    onMessage: (String) -> Unit = {},
    viewModel: MarketViewModel
) {
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
                            text = "Đăng ${getRelativeTimeString(product.timestamp)}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    //Thông tin người bán
                    SellerInfoSection(seller = seller)
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
                            text = product.description,
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
            }
        }
        //BOTTOM BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.White)
        ) {
            Call_Chat(
                onCall = onCall,
                onMessage = {
                    product.userId?.let {sellerId ->
                        onMessage(sellerId)
                    }
                }
            )
        }
    }
}
@Composable
fun SellerInfoSection(seller: User?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        //Header: Avatar + Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar
            if (seller?.avatarUrl != null) {
                AsyncImage(
                    model = seller.avatarUrl,
                    contentDescription = "Avatar người bán",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFF0E9397), CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.default_image),
                    error = painterResource(id = R.drawable.default_image)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.default_image),
                    contentDescription = "Avatar người bán",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF0E9397), CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name
            Text(
                text = seller?.username ?: "Người bán ẩn danh",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        //Info Grid: Campus | Phone
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Campus
            seller?.campus?.let { campus ->
                if (campus.isNotEmpty()) {
                    InfoChip(
                        icon = Icons.Default.LocationOn,
                        text = campus,
                        iconTint = Color(0xFF4CAF50),
                        backgroundColor = Color(0xFFF1F8F4),
                        textColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Phone
            seller?.phone?.let { phone ->
                if (phone.isNotEmpty()) {
                    InfoChip(
                        icon = Icons.Default.Phone,
                        text = phone,
                        iconTint = Color(0xFF0E9397),
                        backgroundColor = Color(0xFFE8F6F7),
                        textColor = Color(0xFF0E9397),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Warning nếu bị ban
        if (seller?.isBanned == true) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFEBEE)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tài khoản đã bị khóa",
                        fontSize = 13.sp,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconTint: Color,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                color = textColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
