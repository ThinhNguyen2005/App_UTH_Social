package com.example.uth_socials.ui.screen.post

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.repository.CategoryRepository
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import com.example.uth_socials.ui.component.button.PrimaryButton
import com.example.uth_socials.ui.viewmodel.PostViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.uth_socials.ui.component.common.BannedUserDialog
import com.example.uth_socials.ui.component.common.formatVND
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

    //-------Article Variables-------//
    val categories by postViewModel.categories.collectAsState()
    var categoryName by remember { mutableStateOf("") }
    var articleContent by remember { mutableStateOf("") }
    val maxArticleContentLength by remember { mutableStateOf(500) }
    var isOverArticleContentLimit by remember { mutableStateOf(false) }
    var articleCategoryId by remember { mutableStateOf("") }
    //-------------------------------//

    //-------Product Variables-------//
    var productName by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf(100000) }
    //-------------------------------//

    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            selectedImageUris = uris
        }
    )

    LaunchedEffect(Unit) {
        user = userRepository.getUser(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                    model = user?.avatarUrl,
                    contentScale = ContentScale.Crop
                )
                Text(user?.username ?: "", fontWeight = FontWeight.Medium)
            }

            PrimaryButton(
                buttonColor = ButtonDefaults.buttonColors(containerColor = Color(0xFF007E8F)),
                text = "Đăng",
                textColor = Color.White,
                modifier = Modifier
                    .width(100.dp)
                    .height(40.dp),
                onClick = {
                    if (selectedTabIndex == 0) {
                        if (articleContent.isEmpty()) {
                            Toast.makeText(context, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT)
                                .show()
                            return@PrimaryButton
                        } else if (isOverArticleContentLimit) {
                            Toast.makeText(
                                context,
                                "Nội dung bài viết đã vượt quá giới hạn",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            return@PrimaryButton
                        } else if (selectedImageUris.size > 4) {
                            Toast.makeText(
                                context,
                                "Bạn chỉ chọn được 4 ảnh cho bài viết",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            selectedImageUris = emptyList()
                            return@PrimaryButton
                        }
                        postViewModel.uploadArticle(
                            context,
                            articleContent.trim(),
                            selectedImageUris,
                            articleCategoryId
                        )
                        navController.navigate("home")
                    } else {
                        if (productName.isEmpty() || productType.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Vui lòng nhập đầy đủ thông tin",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@PrimaryButton
                        } else if (selectedImageUris.size > 2) {
                            Toast.makeText(
                                context,
                                "Bạn chỉ chọn được 2 ảnh cho sản phẩm",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            selectedImageUris = emptyList()
                            return@PrimaryButton
                        }
                        postViewModel.uploadProduct(
                            context,
                            productName.trim(),
                            productDescription.trim(),
                            productType,
                            productPrice,
                            selectedImageUris
                        )
                        navController.navigate("home")
                    }

                    Toast.makeText(context, "Đăng thành công", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> ArticlePost(
                articleContent = articleContent,
                maxArticleContentLength = maxArticleContentLength,
                categoryName = categoryName,
                categories = categories,
                onContentChanged = { it ->
                    articleContent = it
                    isOverArticleContentLimit = it.length > maxArticleContentLength
                },
                onCategoryClicked = { category ->
                    articleCategoryId = category.id
                    categoryName = category.name
                }
            )

            1 -> ProductPost(
                productName = productName,
                productDescription = productDescription,
                productType = productType,
                productPrice = productPrice,
                onProductNameChanged = { it -> productName = it },
                onProductDescriptionChanged = { it -> productDescription = it },
                onProductPriceChanged = { it -> productPrice = it.toIntOrNull() ?: 0 },
                onTypeClicked = { type -> productType = type }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            // Hiển thị các ảnh được chọn
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(selectedImageUris) { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Ảnh đã chọn",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, Color.Gray, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Nút chọn nhiều ảnh
            TextButton(
                onClick = {
                    multiplePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Chọn ảnh",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chọn ảnh", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Các tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    120.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                TabButton(
                    text = "Bài viết",
                    isSelected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )

                TabButton(
                    text = "Sản phẩm",
                    isSelected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                )
            }
        }

    }

    // Ban dialogs
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
    // }
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
    var isFocusedBasicTextField by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    BasicTextField(
        value = articleContent,
        onValueChange = { it -> onContentChanged(it) },
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp)
            .onFocusChanged { focusState ->
                isFocusedBasicTextField = focusState.isFocused
            },
        decorationBox = { innerTextField ->
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (articleContent.isEmpty() && !isFocusedBasicTextField) {
                    Text(
                        "Chia sẻ suy nghĩ của bạn?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        //modifier = Modifier.align(Alignment.TopStart)
                    )
                }

                innerTextField()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        "${articleContent.length}/${maxArticleContentLength}",
                        color = if (articleContent.length > maxArticleContentLength) {
                            Color(0xFFFF0000)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)

                    )
                }
            }

        },
    )

    Spacer(modifier = Modifier.height(16.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField(
            value = categoryName,
            onValueChange = {},
            readOnly = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
            label = {
                Text(
                    "Chọn Category",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = TextFieldDefaults.colors(
                //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                // unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                //cursorColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
                //unfocusedTextColor = MaterialTheme.colorScheme.surface,
            ),
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
                    .height(200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                categories.forEach { category ->
                    if (category.name != "Mới nhất" && category.name != "Tất cả") {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    category.name,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
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
    onTypeClicked: (String) -> Unit,
) {

    // Dropdown menu state
    var expanded by remember { mutableStateOf(false) }
    var priceInput by remember { mutableStateOf("") }

    val productTypes = listOf("Sách", "Quần áo", "Đồ điện tử", "Khác")
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Tên sản phẩm ---
        Text(text = "Tên sản phẩm", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextField(
            value = productName,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
            onValueChange = { it -> onProductNameChanged(it) },
            placeholder = {
                Text(
                    "Nhập tên sản phẩm",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                // unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                //cursorColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
                //unfocusedTextColor = MaterialTheme.colorScheme.surface,
                focusedPlaceholderColor = Color.Transparent,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            )
        )

        // --- Mô tả sản phẩm ---
        Text(text = "Mô tả sản phẩm", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextField(
            value = productDescription,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
            onValueChange = { it -> onProductDescriptionChanged(it) },
            placeholder = {
                Text(
                    "Nhập mô tả sản phẩm",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                // unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                //cursorColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
                //unfocusedTextColor = MaterialTheme.colorScheme.surface,
                focusedPlaceholderColor = Color.Transparent,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            )
        )

        // --- Loại sản phẩm ---
        Text(text = "Loại sản phẩm", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = productType,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    label = {
                        Text(
                            "Chọn loại sản phẩm",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        //focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        // unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        //cursorColor = MaterialTheme.colorScheme.surfaceVariant,
                        //focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
                        //unfocusedTextColor = MaterialTheme.colorScheme.surface,
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    productTypes.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    type,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                onTypeClicked(type)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // --- Giá ---
        Text(text = "Giá", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextField(
            value = formatPriceInput(priceInput),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
            onValueChange = { newValue ->

                val numeric = newValue.filter { it.isDigit() }

                priceInput = numeric

                onProductPriceChanged(numeric)
                //numericPrice = formatVND(productPrice)
            },
            placeholder = {
                Text(
                    "Nhập giá",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedPlaceholderColor = Color.Transparent,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            )
        )
    }
}

fun formatPriceInput(input: String): String {
    val numeric = input.filter { it.isDigit() }
    if (numeric.isEmpty()) return ""
    return "%,d".format(numeric.toLong())
}
