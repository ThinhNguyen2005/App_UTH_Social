package com.example.uth_socials.ui.screen.market

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.R
import com.example.uth_socials.data.market.Product
import com.example.uth_socials.data.user.User
import com.example.uth_socials.ui.component.common.CallConfirmDialog
import com.example.uth_socials.ui.component.common.Call_Chat
import com.example.uth_socials.ui.component.common.formatTimeAgo
import com.example.uth_socials.ui.component.common.formatVND
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

    //State để hiển thị dialog
    var showCallDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentUserId = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
    val isOwner = currentUserId == detailState.product?.userId

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
                isOwner = isOwner,
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
                onEdit = { showEditDialog = true },
                onDelete = { showDeleteDialog = true },
                viewModel = viewModel
            )
            //Hiển thị dialog xác nhận gọi điện
            CallConfirmDialog(
                isVisible = showCallDialog,
                phoneNumber = detailState.seller?.phone ?: "",
                sellerName = detailState.seller?.username ?: "Người bán",
                onDismiss = { showCallDialog = false }
            )
            //Dialog chỉnh sửa
            if (showEditDialog) {
                EditProductDialog(
                    product = detailState.product!!,
                    onDismiss = { showEditDialog = false },
                    onConfirm = { name, price, description, type ->
                        viewModel.updateProduct(
                            productId = detailState.product!!.id,
                            name = name,
                            price = price,
                            description = description,
                            type = type,
                            onSuccess = {
                                showEditDialog = false
                                Toast.makeText(context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
            //Dialog xóa
            if (showDeleteDialog) {
                DeleteConfirmDialog(
                    productName = detailState.product!!.name,
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        viewModel.deleteProduct(
                            productId = detailState.product!!.id,
                            onSuccess = {
                                showDeleteDialog = false
                                Toast.makeText(context, "Xóa thành công", Toast.LENGTH_SHORT).show()
                                onBack() //Quay lại màn hình trước
                            },
                            onError = { error ->
                                showDeleteDialog = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ProductDetailContent(
    product: Product,
    seller: User?,
    isOwner: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit = {},
    onCall: () -> Unit = {},
    onMessage: (String) -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    viewModel: MarketViewModel
) {
    var showMenu by remember { mutableStateOf(false) }
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
                ImageCarousel(imageUrls = product.imageUrls)
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
                            text = "Đăng ${formatTimeAgo(product.timestamp)}",
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

            //Right: Share & Menu icon
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
                // Hiển thị menu nếu là chủ sản phẩm
                if (isOwner) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.7f), CircleShape)
                                .padding(4.dp)
                                .size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Chỉnh sửa") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                }
                            )
                        }
                    }
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
//Image Carousel
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCarousel(imageUrls: List<String>) {
    // Nếu không có ảnh, hiển thị ảnh mặc định
    val images = imageUrls.ifEmpty { listOf("") }
    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color(0xFF262525))
    ) {
        //HorizontalPager để vuốt ngang ảnh
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = images[page].ifEmpty { R.drawable.default_image },
                contentDescription = "Product image ${page + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        //Nếu có nhiều hơn 1 ảnh
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(images.size) { index ->
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = CircleShape,
                        color = if (pagerState.currentPage == index)
                            Color.White
                        else
                            Color.White.copy(alpha = 0.4f)
                    ) {}
                }
            }
        }

        //Số thứ tự ảnh (vd: 1/5)
        if (images.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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
@Composable
fun EditProductDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Int, description: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var priceText by remember { mutableStateOf(product.price.toString()) }
    var description by remember { mutableStateOf(product.description) }
    var type by remember { mutableStateOf(product.type) }

    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = "Chỉnh sửa sản phẩm",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tên sản phẩm
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Tên sản phẩm", color = Color(0xFF49454F)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text("Tên không được để trống")
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1C1B1F), // ✅ Màu text tối hơn
                        unfocusedTextColor = Color(0xFF1C1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Giá
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        priceError = it.toIntOrNull() == null || it.toIntOrNull()!! <= 0
                    },
                    label = { Text("Giá (VNĐ)", color = Color(0xFF49454F)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = priceError,
                    supportingText = {
                        if (priceError) {
                            Text("Giá phải là số dương")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1C1B1F),
                        unfocusedTextColor = Color(0xFF1C1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mô tả
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả", color = Color(0xFF49454F)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    placeholder = { Text("Nhập mô tả chi tiết...", color = Color(0xFF79747E)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1C1B1F),
                        unfocusedTextColor = Color(0xFF1C1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Loại sản phẩm", color = Color(0xFF49454F)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Sách | Quần áo | Đồ điện tử | Khác", color = Color(0xFF79747E)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1C1B1F),
                        unfocusedTextColor = Color(0xFF1C1B1F)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Hủy", color = Color(0xFF1C1B1F))
                    }

                    // Confirm
                    Button(
                        onClick = {
                            if (name.isNotBlank() && priceText.toIntOrNull() != null && priceText.toInt() > 0) {
                                onConfirm(name, priceText.toInt(), description, type)
                            } else {
                                nameError = name.isBlank()
                                priceError = priceText.toIntOrNull() == null || priceText.toIntOrNull()!! <= 0
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0E9397)
                        )
                    ) {
                        Text("Lưu", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    productName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Xác nhận xóa",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F)
            )
        },
        text = {
            Text("Bạn có chắc chắn muốn xóa sản phẩm \"$productName\"? Hành động này không thể hoàn tác.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                )
            ) {
                Text("Xóa", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}