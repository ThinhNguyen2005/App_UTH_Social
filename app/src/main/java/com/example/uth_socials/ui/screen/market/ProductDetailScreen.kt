package com.example.uth_socials.ui.screen.market

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.data.market.Product
import com.example.uth_socials.data.user.User
import com.example.uth_socials.ui.component.common.CallConfirmDialog
import com.example.uth_socials.ui.component.common.Call_Chat
import com.example.uth_socials.ui.component.common.formatTimeAgo
import com.example.uth_socials.ui.component.common.formatVND
import com.example.uth_socials.ui.viewmodel.MarketViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProductDetailScreen(
    productId: String?,
    onBack: () -> Unit = {},
    onShare: () -> Unit = {},
    onCall: () -> Unit = {},
    onMessage: (String) -> Unit = {},
    onProductClick: (String) -> Unit = {},
    viewModel: MarketViewModel = viewModel(),
) {
    val context = LocalContext.current
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(productId) { viewModel.getProductById(productId) }

    var showCallDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentUserId = remember {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
    val isOwner = currentUserId != null && currentUserId == detailState.product?.userId

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            detailState.isLoading -> LoadingScaffold(onBack)
            detailState.error != null -> ErrorScaffold(
                message = detailState.error!!,
                onBack = onBack,
                onRetry = { viewModel.getProductById(productId) }
            )
            detailState.product != null -> {
                ProductDetailContent(
                    product = detailState.product!!,
                    seller = detailState.seller,
                    relatedProducts = detailState.relatedProducts,
                    isOwner = isOwner,
                    onBack = onBack,
                    onShare = onShare,
                    onCall = {
                        val phone = detailState.seller?.phone
                        if (!phone.isNullOrBlank()) showCallDialog = true
                        else Toast.makeText(
                            context,
                            "Người bán chưa cập nhật số điện thoại",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onMessage = onMessage,
                    onEdit = { showEditDialog = true },
                    onDelete = { showDeleteDialog = true },
                    onProductClick = onProductClick
                )

                CallConfirmDialog(
                    isVisible = showCallDialog,
                    phoneNumber = detailState.seller?.phone ?: "",
                    sellerName = detailState.seller?.username ?: "Người bán",
                    onDismiss = { showCallDialog = false }
                )

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
                                onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    )
                }

                if (showDeleteDialog) {
                    DeleteConfirmDialog(
                        productName = detailState.product!!.name,
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            viewModel.deleteProduct(
                                productId = detailState.product!!.id,
                                onSuccess = {
                                    showDeleteDialog = false
                                    Toast.makeText(context, "Đã xoá sản phẩm", Toast.LENGTH_SHORT).show()
                                    onBack()
                                },
                                onError = {
                                    showDeleteDialog = false
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScaffold(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        DetailTopBar(onBack = onBack, isOwner = false, overImage = false)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun ErrorScaffold(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DetailTopBar(onBack = onBack, isOwner = false, overImage = false)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Không thể tải sản phẩm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Thử lại") }
        }
    }
}

@Composable
fun ProductDetailContent(
    product: Product,
    seller: User?,
    relatedProducts: List<Product>,
    isOwner: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onCall: () -> Unit,
    onMessage: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onProductClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = if (isOwner) 24.dp else 88.dp)
        ) {
            // Carousel ảnh + gradient overlay đảm bảo topbar đọc được
            Box(modifier = Modifier.fillMaxWidth()) {
                ImageCarousel(imageUrls = product.imageUrls)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Card thông tin nổi lên ảnh (negative margin effect)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Title + price
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = formatVND(product.price),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (product.type.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = product.type,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Đăng ${formatTimeAgo(product.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SectionDivider()

                    SellerInfoSection(seller = seller)

                    SectionDivider()

                    // Mô tả
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            text = "Mô tả",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description.ifBlank { "Người bán chưa thêm mô tả." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }

                    if (relatedProducts.isNotEmpty()) {
                        SectionDivider()
                        RelatedProductsSection(
                            products = relatedProducts,
                            onProductClick = onProductClick
                        )
                    }
                }
            }
        }

        DetailTopBar(
            onBack = onBack,
            isOwner = isOwner,
            onShare = onShare,
            onEdit = onEdit,
            onDelete = onDelete,
            overImage = true
        )

        // Bottom action - chỉ hiển thị khi không phải owner
        if (!isOwner) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 6.dp
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    Call_Chat(
                        onCall = onCall,
                        onMessage = { onMessage(product.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 1.dp
    )
}

@Composable
private fun RelatedProductsSection(
    products: List<Product>,
    onProductClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = "Có thể bạn cũng thích",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id.ifBlank { it.hashCode().toString() } }) { p ->
                Box(modifier = Modifier.width(160.dp)) {
                    ProductItem(
                        product = p,
                        onClick = { if (p.id.isNotBlank()) onProductClick(p.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    isOwner: Boolean,
    onShare: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    overImage: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .zIndex(2f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Quay lại",
            overImage = overImage
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleIconButton(
                onClick = onShare,
                icon = Icons.Outlined.Share,
                contentDescription = "Chia sẻ",
                overImage = overImage
            )
            if (isOwner) {
                Box {
                    CircleIconButton(
                        onClick = { showMenu = true },
                        icon = Icons.Outlined.MoreVert,
                        contentDescription = "Thêm",
                        overImage = overImage
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Chỉnh sửa") },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Xoá",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    overImage: Boolean
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        color = if (overImage) Color.White.copy(alpha = 0.92f)
        else MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFF1C1B1F),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
