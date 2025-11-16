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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.example.uth_socials.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(postViewModel: PostViewModel, productViewModel: ProductViewModel,navController: NavController) {
    val context = LocalContext.current

    val userRepository: UserRepository = remember {UserRepository()}
    val userId = userRepository.getCurrentUserId() ?: return

    var user by remember { mutableStateOf<User?>(null) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var expanded by remember { mutableStateOf(false) }

    val categories by postViewModel.categories.collectAsState()

    var categoryName by remember { mutableStateOf("") }

    var articleContent by remember { mutableStateOf("") }
    var articelCategoryId by remember { mutableStateOf("") }

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

    Scaffold(

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
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
                            if(articleContent.isEmpty()) {
                                Toast.makeText(context, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            postViewModel.uploadPost(articleContent, selectedImageUris, articelCategoryId)
                            navController.navigate("home")
                        }else{
                            if(productViewModel.name.isEmpty() || productViewModel.type.isEmpty()){
                                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            productViewModel.postArticle(userId)
                            navController.navigate("home")
                        }

                        Toast.makeText(context, "Đăng thành công", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> { //Mục đăng bài viết
                    var isFocusedBasicTextField by remember { mutableStateOf(false) }

                    BasicTextField(
                        value = articleContent,
                        onValueChange = { articleContent = it },
                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                            .padding(12.dp).onFocusChanged { focusState ->
                                isFocusedBasicTextField = focusState.isFocused
                            },
                        decorationBox = { innerTextField ->
                            if (articleContent.isEmpty() && !isFocusedBasicTextField){
                                Text("Chia sẻ suy nghĩ của bạn?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            innerTextField()
                        }
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
                            label = { Text("Chọn Category", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                                articelCategoryId = category.id
                                                categoryName = category.name
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> ProductPost(productViewModel = productViewModel)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                // ✅ Hiển thị các ảnh được chọn
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
    }
}