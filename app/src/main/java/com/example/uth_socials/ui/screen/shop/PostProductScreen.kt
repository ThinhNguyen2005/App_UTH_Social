package com.example.uth_socials.ui.screen.shop

//import android.graphics.Bitmap
//import android.net.Uri
//import android.widget.Toast
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AddPhotoAlternate
//import androidx.compose.material.icons.filled.Image
//import androidx.compose.material.icons.filled.PhotoCamera
//import androidx.compose.material3.AlertDialog
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TextFieldDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.drawBehind
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.uth_socials.R
//import com.example.uth_socials.ui.viewmodel.ProductViewModel
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import coil.compose.AsyncImage
//import com.example.uth_socials.data.model.Product
//import com.example.uth_socials.ui.viewmodel.OperationUiState
//
//@Composable
//fun PostProductScreen(
//    productId: String? = null,
//    username: String = "TH",
//    onPostSuccess: () -> Unit = {},
//    viewModel: ProductViewModel = viewModel(),
//) {
//    //State cho các trường nhập liệu
//    var productName by remember { mutableStateOf("") }
//    var description by remember { mutableStateOf("") }
//    var price by remember { mutableStateOf("") }
//    var imageUri by remember { mutableStateOf<Uri?>(null) }
//
//    //State để hiển thị dialog chọn nguồn ảnh
//    var showImageSourceDialog by remember { mutableStateOf(false) }
//
//    // ===== LAUNCHER ĐỂ CHỌN ẢNH TỪ THƯ VIỆN =====
//    val galleryLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent(),
//        onResult = { uri: Uri? ->
//            uri?.let { imageUri = it }
//        }
//    )
//
//    // ===== LAUNCHER ĐỂ CHỤP ẢNH TỪ CAMERA =====
//    val cameraLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.TakePicturePreview(),
//        onResult = { bitmap: Bitmap? ->
//            // TODO: Chuyển Bitmap thành Uri và lưu vào storage
//            // Hiện tại chỉ demo, cần implement hàm saveBitmapToUri()
//            bitmap?.let {
//                // imageUri = saveBitmapToUri(context, bitmap)
//                // Hoặc có thể lưu bitmap vào ViewModel để xử lý
//            }
//        }
//    )
//
//    // ===== LOAD DỮ LIỆU NẾU LÀ CHỈNH SỬA =====
//    LaunchedEffect(productId) {
//        productId?.let {
//            viewModel.getProductById(it)
//        }
//    }
//
//    // Lấy dữ liệu từ ViewModel khi load xong
//    val operationState by viewModel.operationState.collectAsState()
//    val detailState by viewModel.detailState.collectAsState()
//
//    LaunchedEffect(detailState.product) {
//        detailState.product?.let { product ->
//            productName = product.name
//            description = product.description ?: ""
//            price = product.price.toString()
//            product.imageUrl?.let { url ->
//                imageUri = Uri.parse(url)
//            }
//        }
//    }
//
//    val context = LocalContext.current
//
//    LaunchedEffect(operationState) {
//        when (operationState) {
//            is OperationUiState.Success -> {
//                Toast.makeText(context, (operationState as OperationUiState.Success).message, Toast.LENGTH_SHORT).show()
//                viewModel.resetOperationState()
//                onPostSuccess()
//            }
//            is OperationUiState.Error -> {
//                Toast.makeText(context, (operationState as OperationUiState.Error).message, Toast.LENGTH_SHORT).show()
//            }
//            else -> {}
//        }
//    }
//
//
//    Column(modifier = Modifier.fillMaxSize()) {
//        Box(
//            Modifier
//                .height(100.dp)
//                .fillMaxWidth()
//                .drawBehind {
//                    val strokeWidth = 2.dp.toPx()  // độ dày viền
//                    val y = size.height - strokeWidth / 2  // nằm sát đáy box
//
//                    drawLine(
//                        color = Color.LightGray,
//                        start = Offset(0f, y),
//                        end = Offset(size.width, y),
//                        strokeWidth = strokeWidth
//                    )
//                }
//        ) {
//            //Logo
//            Image(
//                painter = painterResource(R.drawable.logo_uth),
//                contentDescription = null,
//                modifier = Modifier
//                    .size(width = 160.dp, height = 38.dp)
//                    .offset(x = 17.dp, y = 40.dp)
//            )
//        }
//
//        // Header
//        Row(
//            modifier = Modifier.fillMaxWidth().height(56.dp)
//                .padding(horizontal = 16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Image(
//                    painter = painterResource(id = R.drawable.avatar_thien),
//                    contentDescription = "Avatar",
//                    modifier = Modifier
//                        .size(43.dp)
//                        .shadow(6.dp, CircleShape)
//                        .clip(CircleShape)
//                        .background(Color.White)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = username,
//                    fontSize = 18.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    color = Color.Black
//                )
//            }
//
//            //Nút Đăng hoặc Cập nhật
//            Button(
//                onClick = {
//                    //Validate du lieu
//                    if (productName.isBlank() || description.isBlank()) {
//                        return@Button
//                    }
//                    if (productId == null) {
//                        val product = Product(
//                            name = productName,
//                            description = description,
//                            price = price.toDoubleOrNull() ?: 0.0,
//                            imageUrl = imageUri?.toString(),
//                            userId = username
//                        )
//                        //Add product chỉ nhận 1 đối tượng, khono nhận trường riêng lẻ
//                        viewModel.addProduct(product)
//                    } else {
//                        val updatedProduct = Product(
//                            id = productId,
//                            name = productName,
//                            description = description,
//                            price = price.toDoubleOrNull() ?: 0.0,
//                            imageUrl = imageUri?.toString(),
//                            userId = "demoUserId",
//                            userName = username
//                        )
//                        viewModel.updateProduct(updatedProduct)
//                    }
//                    onPostSuccess()
//                },
//                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
//                modifier = Modifier
//                    .size(100.dp, 40.dp),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Text("Đăng", color = Color.White)
//            }
//        }
//
//        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
//            item {
//                Text(
//                    text = "Tên sản phẩm",
//                    fontWeight = FontWeight.Normal,
//                    fontSize = 18.sp,
//                    color = Color.Black
//                )
//                OutlinedTextField(
//                    value = productName,
//                    onValueChange = { productName = it },
//                    placeholder = { Text("Nhập tên sản phẩm") },
//                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
//                        .shadow(4.dp, RoundedCornerShape(10.dp))
//                        .background(Color.White, RoundedCornerShape(10.dp)),
//                    colors = TextFieldDefaults.colors(
//                        focusedContainerColor = Color(0xFFD0EBEB),
//                        unfocusedContainerColor = Color(0xFFD0EBEB),
//                        focusedIndicatorColor = Color.Transparent,
//                        unfocusedIndicatorColor = Color.Transparent
//                    )
//                )
//            }
//
//            //Mô tả sản phẩm
//            item {
//                Text(
//                    text = "Mô tả sản phẩm",
//                    fontWeight = FontWeight.Normal,
//                    fontSize = 18.sp,
//                    color = Color.Black
//                )
//                OutlinedTextField(
//                    value = description,
//                    onValueChange = { description = it },
//                    placeholder = { Text("Nhập mô tả sản phẩm") },
//                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
//                        .shadow(4.dp, RoundedCornerShape(10.dp))
//                        .background(Color.White, RoundedCornerShape(10.dp)),
//                    colors = TextFieldDefaults.colors(
//                        focusedContainerColor = Color(0xFFD0EBEB),
//                        unfocusedContainerColor = Color(0xFFD0EBEB),
//                        focusedIndicatorColor = Color.Transparent,
//                        unfocusedIndicatorColor = Color.Transparent
//                    )
//                )
//            }
//            //Giá sản phẩm
//            item {
//                Text(
//                    text = "Giá sản phẩm",
//                    fontWeight = FontWeight.Normal,
//                    fontSize = 18.sp,
//                    color = Color.Black
//                )
//                OutlinedTextField(
//                    value = price,
//                    onValueChange = { price = it },
//                    placeholder = { Text("Nhập giá sản phẩm") },
//                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
//                        .shadow(4.dp, RoundedCornerShape(10.dp))
//                        .background(Color.White, RoundedCornerShape(10.dp)),
//                    colors = TextFieldDefaults.colors(
//                        focusedContainerColor = Color(0xFFD0EBEB),
//                        unfocusedContainerColor = Color(0xFFD0EBEB),
//                        focusedIndicatorColor = Color.Transparent,
//                        unfocusedIndicatorColor = Color.Transparent
//                    ),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                )
//            }
//
//            //Chọn ảnh
//            item {
//                Spacer(modifier = Modifier.height(8.dp))
//                //Nút chọn ảnh - click để hiện dialog chọn nguồn
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 24.dp)
//                        .clip(RoundedCornerShape(10.dp))
//                        .background(Color(0xFFE3F2FD))
//                        .clickable { showImageSourceDialog = true }
//                        .padding(16.dp)
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.AddPhotoAlternate,
//                        contentDescription = null,
//                        tint = Color(0xFF009688),
//                        modifier = Modifier.size(24.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = if (imageUri == null) "Chọn ảnh sản phẩm" else "Đã chọn ảnh",
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = Color(0xFF009688)
//                    )
//                }
//                //Hiển thị ảnh đã chọn (nếu có)
//                imageUri?.let { uri ->
//                    Spacer(modifier = Modifier.height(16.dp))
//                    AsyncImage(
//                        model = uri,
//                        contentDescription = "Ảnh đã chọn",
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(200.dp)
//                            .padding(horizontal = 24.dp)
//                            .clip(RoundedCornerShape(12.dp))
//                    )
//                }
//            }
//
//            //Bottom Tabs
//            item {
//                Spacer(modifier = Modifier.height(16.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth().padding(24.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text(
//                        "Bài viết",
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.Black
//                    )
//                    Text(
//                        "Sản phẩm",
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.SemiBold,
//                        color = Color.Black,
//                        modifier = Modifier
//                            .drawBehind {
//                                drawLine(
//                                    color = Color(0xFF009688),
//                                    start = Offset(size.width * 0.1f, size.height + 5f),
//                                    end = Offset(size.width * 0.9f, size.height + 5f),
//                                    strokeWidth = 8f,
//                                    cap = StrokeCap.Round
//                                )
//                            }
//                    )
//                }
//            }
//        }
//    }
//    // ===== DIALOG CHỌN NGUỒN ẢNH =====
//    if (showImageSourceDialog) {
//        AlertDialog(
//            onDismissRequest = { showImageSourceDialog = false },
//            title = {
//                Text(
//                    text = "Chọn nguồn ảnh",
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 20.sp
//                )
//            },
//            text = {
//                Column(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    // Chọn từ thư viện
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clip(RoundedCornerShape(8.dp))
//                            .clickable {
//                                showImageSourceDialog = false
//                                // Launch gallery picker - chọn ảnh từ thư viện
//                                galleryLauncher.launch("image/*")
//                            }
//                            .background(Color(0xFFE3F2FD))
//                            .padding(16.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Image,
//                            contentDescription = "Thư viện",
//                            tint = Color(0xFF009688),
//                            modifier = Modifier.size(32.dp)
//                        )
//                        Spacer(modifier = Modifier.width(16.dp))
//                        Text(
//                            text = "Chọn từ thư viện",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//
//                    // Chụp ảnh từ camera
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clip(RoundedCornerShape(8.dp))
//                            .clickable {
//                                showImageSourceDialog = false
//                                cameraLauncher.launch(null)
//                            }
//                            .background(Color(0xFFE3F2FD))
//                            .padding(16.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.PhotoCamera,
//                            contentDescription = "Camera",
//                            tint = Color(0xFF009688),
//                            modifier = Modifier.size(32.dp)
//                        )
//                        Spacer(modifier = Modifier.width(16.dp))
//                        Text(
//                            text = "Chụp ảnh từ camera",
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//                }
//            },
//            confirmButton = {},
//            dismissButton = {
//                TextButton(onClick = { showImageSourceDialog = false }) {
//                    Text("Hủy")
//                }
//            },
//            containerColor = Color.White,
//            shape = RoundedCornerShape(16.dp)
//        )
//    }
//}
