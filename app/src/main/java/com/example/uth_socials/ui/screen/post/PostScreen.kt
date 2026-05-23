package com.example.uth_socials.ui.screen.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import com.example.uth_socials.ui.component.common.BannedUserDialog
import com.example.uth_socials.ui.viewmodel.PostViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    postViewModel: PostViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val userRepository: UserRepository = remember { UserRepository() }
    val userId = userRepository.getCurrentUserId() ?: return
    var user by remember { mutableStateOf<User?>(null) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val categories by postViewModel.categories.collectAsStateWithLifecycle()
    var categoryName by remember { mutableStateOf("") }
    var articleContent by remember { mutableStateOf("") }
    val maxArticleContentLength by remember { mutableStateOf(500) }
    var isOverArticleContentLimit by remember { mutableStateOf(false) }
    var articleCategoryId by remember { mutableStateOf("") }

    var productName by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf(0) }

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> selectedImageUris = uris }
    )

    LaunchedEffect(Unit) { user = userRepository.getUser(userId) }

    val onPost: () -> Unit = {
        if (selectedTabIndex == 0) {
            when {
                articleContent.isEmpty() ->
                    Toast.makeText(context, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show()
                isOverArticleContentLimit ->
                    Toast.makeText(context, "Nội dung đã vượt quá giới hạn", Toast.LENGTH_SHORT).show()
                selectedImageUris.size > 4 -> {
                    Toast.makeText(context, "Tối đa 4 ảnh cho bài viết", Toast.LENGTH_SHORT).show()
                    selectedImageUris = emptyList()
                }
                else -> {
                    postViewModel.uploadArticle(
                        context,
                        articleContent.trim(),
                        selectedImageUris,
                        articleCategoryId
                    )
                    Toast.makeText(context, "Đăng thành công", Toast.LENGTH_SHORT).show()
                    navController.navigate("home")
                }
            }
        } else {
            when {
                productName.isEmpty() || productType.isEmpty() ->
                    Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                selectedImageUris.size > 2 -> {
                    Toast.makeText(context, "Tối đa 2 ảnh cho sản phẩm", Toast.LENGTH_SHORT).show()
                    selectedImageUris = emptyList()
                }
                else -> {
                    postViewModel.uploadProduct(
                        context,
                        productName.trim(),
                        productDescription.trim(),
                        productType,
                        productPrice,
                        selectedImageUris
                    )
                    Toast.makeText(context, "Đăng thành công", Toast.LENGTH_SHORT).show()
                    navController.navigate("home")
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            PostTopBar(
                title = if (selectedTabIndex == 0) "Bài viết mới" else "Sản phẩm mới",
                onBack = { navController.popBackStack() },
                onPost = onPost
            )

            // Author row
            user?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = it.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = it.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Đăng công khai",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Segmented tabs at TOP
            SegmentedTabs(
                selected = selectedTabIndex,
                onSelect = { selectedTabIndex = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> ArticlePost(
                        articleContent = articleContent,
                        maxArticleContentLength = maxArticleContentLength,
                        categoryName = categoryName,
                        categories = categories,
                        onContentChanged = {
                            articleContent = it
                            isOverArticleContentLimit = it.length > maxArticleContentLength
                        },
                        onCategoryClicked = { c ->
                            articleCategoryId = c.id
                            categoryName = c.name
                        }
                    )
                    1 -> ProductPost(
                        productName = productName,
                        productDescription = productDescription,
                        productType = productType,
                        productPrice = productPrice,
                        onProductNameChanged = { productName = it },
                        onProductDescriptionChanged = { productDescription = it },
                        onProductPriceChanged = { productPrice = it.toIntOrNull() ?: 0 },
                        onTypeClicked = { productType = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                MediaPickerSection(
                    images = selectedImageUris,
                    onPick = {
                        multiplePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = { uri ->
                        selectedImageUris = selectedImageUris.filterNot { it == uri }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    BannedUserDialog(
        isVisible = postViewModel.showBanDialog,
        banReason = null,
        onDismiss = { postViewModel.showBanDialog = false },
        onLogout = {
            FirebaseAuth.getInstance().signOut()
            postViewModel.showBanDialog = false
            navController.navigate("home")
        }
    )
}

@Composable
private fun PostTopBar(
    title: String,
    onBack: () -> Unit,
    onPost: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Quay lại")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        Button(
            onClick = onPost,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text("Đăng", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SegmentedTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("Bài viết", "Sản phẩm")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MediaPickerSection(
    images: List<Uri>,
    onPick: () -> Unit,
    onRemove: (Uri) -> Unit
) {
    Column {
        Text(
            text = "Hình ảnh",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onPick),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Thêm ảnh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Thêm ảnh",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(images, key = { it.toString() }) { uri ->
                Box(modifier = Modifier.size(96.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onRemove(uri) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Xoá",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlePost(
    articleContent: String,
    maxArticleContentLength: Int,
    categoryName: String,
    categories: List<Category>,
    onContentChanged: (String) -> Unit,
    onCategoryClicked: (Category) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldLabel("Nội dung")
        BasicTextField(
            value = articleContent,
            onValueChange = onContentChanged,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(14.dp)
                .onFocusChanged { isFocused = it.isFocused },
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (articleContent.isEmpty() && !isFocused) {
                        Text(
                            text = "Chia sẻ suy nghĩ của bạn…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    inner()
                }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${articleContent.length}/$maxArticleContentLength",
                style = MaterialTheme.typography.labelSmall,
                color = if (articleContent.length > maxArticleContentLength)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FieldLabel("Danh mục")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = categoryName,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Chọn danh mục") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = filledFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    categories.forEach { category ->
                        if (category.name != "Mới nhất" && category.name != "Tất cả") {
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onCategoryClicked(category)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPost(
    productName: String,
    productDescription: String,
    productType: String,
    productPrice: Int,
    onProductNameChanged: (String) -> Unit,
    onProductDescriptionChanged: (String) -> Unit,
    onProductPriceChanged: (String) -> Unit,
    onTypeClicked: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val productTypes = listOf("Sách", "Quần áo", "Đồ điện tử", "Khác")
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldLabel("Tên sản phẩm")
        TextField(
            value = productName,
            onValueChange = onProductNameChanged,
            placeholder = { Text("Nhập tên sản phẩm") },
            singleLine = true,
            colors = filledFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        FieldLabel("Mô tả")
        TextField(
            value = productDescription,
            onValueChange = onProductDescriptionChanged,
            placeholder = { Text("Mô tả chi tiết sản phẩm") },
            colors = filledFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        FieldLabel("Loại sản phẩm")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = productType,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Chọn loại sản phẩm") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = filledFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                productTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            onTypeClicked(type)
                            expanded = false
                        }
                    )
                }
            }
        }

        FieldLabel("Giá (VND)")
        TextField(
            value = if (productPrice == 0) "" else productPrice.toString(),
            onValueChange = onProductPriceChanged,
            placeholder = { Text("Nhập giá") },
            singleLine = true,
            colors = filledFieldColors(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number
            ),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun filledFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)
