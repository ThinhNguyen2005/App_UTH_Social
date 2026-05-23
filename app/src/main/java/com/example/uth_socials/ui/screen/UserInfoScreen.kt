package com.example.uth_socials.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.viewmodel.UserInfoViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    viewModel: UserInfoViewModel = viewModel(),
    onSaveSuccess: () -> Unit,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val username by viewModel.username.collectAsStateWithLifecycle()
    val campus by viewModel.campus.collectAsStateWithLifecycle()
    val phoneNumber by viewModel.phone.collectAsStateWithLifecycle()
    val major by viewModel.major.collectAsStateWithLifecycle()
    val bio by viewModel.bio.collectAsStateWithLifecycle()
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()


    var expanded by remember { mutableStateOf(false) }
    val campusList = listOf("Cơ sở 1 - Bình Thạnh", "Cơ sở 2 - Thủ Đức", "Cơ sở 3 - Quận 12")


    var newImageUri by remember { mutableStateOf<Uri?>(null) }


    // 🔹 Load username hiện tại từ Firestore (nếu có)
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    val imagePickerLaucher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            newImageUri = uri
        }
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Thông tin tài khoản",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0)
            )

        },

        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {






                Spacer(modifier = Modifier.height(32.dp))
// ----- AVATAR DISPLAY + PICKER -----
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // Ảnh avatar (hiển thị uri mới hoặc url cũ)
                    AsyncImage(
                        model = newImageUri ?: avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nút chọn ảnh
                    Button(
                        onClick = { imagePickerLaucher.launch("image/*") },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Đổi ảnh đại diện")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 🔹 Username (hiển thị, cho phép chỉnh)
                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.onUsernameChange(it) },
                    label = { Text("Tên hiển thị") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF06635A),
                        unfocusedIndicatorColor = Color(0xFFB0BEC5),
                        focusedContainerColor = Color(0xFFF1F4FF),
                        unfocusedContainerColor = Color(0xFFF1F4FF),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { viewModel.onBioChange(it) },
                    label = { Text("Giới thiệu bản thân") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF06635A),
                        unfocusedIndicatorColor = Color(0xFFB0BEC5),
                        focusedContainerColor = Color(0xFFF1F4FF),
                        unfocusedContainerColor = Color(0xFFF1F4FF),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 🔹 Dropdown chọn cơ sở
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = campus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cơ sở (tuỳ chọn)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFF06635A),
                            unfocusedIndicatorColor = Color(0xFFB0BEC5),
                            focusedContainerColor = Color(0xFFF1F4FF),
                            unfocusedContainerColor = Color(0xFFF1F4FF),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        campusList.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    viewModel.onCampusChange(selection)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🔹 Số điện thoại
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { viewModel.onPhoneChange(it) },
                    label = { Text("Số điện thoại (tuỳ chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF06635A),
                        unfocusedIndicatorColor = Color(0xFFB0BEC5),
                        focusedContainerColor = Color(0xFFF1F4FF),
                        unfocusedContainerColor = Color(0xFFF1F4FF),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 🔹 Chuyên ngành
                OutlinedTextField(
                    value = major,
                    onValueChange = { viewModel.onMajorChange(it) },
                    label = { Text("Chuyên ngành (tuỳ chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF06635A),
                        unfocusedIndicatorColor = Color(0xFFB0BEC5),
                        focusedContainerColor = Color(0xFFF1F4FF),
                        unfocusedContainerColor = Color(0xFFF1F4FF),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                ComfirmAuthButton(
                    text = "Hoàn tất",
                    enabled = !isSaving,
                    onClick = {
                        viewModel.updateUserProfile(
                            imageUri = newImageUri,
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Cập nhật thành công",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSaveSuccess()
                            },
                            onError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    }


}