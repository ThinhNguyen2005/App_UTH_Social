package com.example.uth_socials.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.viewmodel.UserInfoViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    viewModel: UserInfoViewModel = viewModel(),
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }

    var username by remember { mutableStateOf("") }
    var campus by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    val campusList = listOf("Cơ sở 1 - Bình Thạnh", "Cơ sở 2 - Thủ Đức", "Cơ sở 3 - Quận 12")

    var avatarUrl by remember { mutableStateOf("") }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // 🔹 Lấy userId hiện tại
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    // 🔹 Load username hiện tại từ Firestore (nếu có)
    LaunchedEffect(userId) {
        if (userId != null) {
            val user = userRepository.getUser(userId)
            if (user != null) {
                username = user.username ?: ""
                campus = user.campus ?: ""
                phoneNumber = user.phone ?: ""
                major = user.major ?: ""
                avatarUrl = user.avatarUrl ?: ""
            }
        }
    }

    val imagePickerLaucher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            newImageUri = uri
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF06635A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    ) {
                        append("UTH")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    ) {
                        append(" Social")
                    }
                },
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Hoàn thiện hồ sơ của bạn",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF06635A)
            )

            Spacer(modifier = Modifier.height(8.dp))



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
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(100))
                        .background(Color.LightGray)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Nút chọn ảnh
                Button(
                    onClick = { imagePickerLaucher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF06635A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Đổi ảnh đại diện")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Username (hiển thị, cho phép chỉnh)
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
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
                                campus = selection
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
                onValueChange = { phoneNumber = it },
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
                onValueChange = { major = it },
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

            // 🔹 Nút Hoàn tất
            ComfirmAuthButton(
                text = "Hoàn tất",
                // isLoading = isUploading, // 👈 (Tốt, nếu nút của bạn hỗ trợ)
                onClick = {
                    if (username.isBlank()) {
                        Toast.makeText(
                            context,
                            "Tên hiển thị không được để trống",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        isUploading = true

                        // 1. 🛑 XÓA scope.launch { ... } ở đây

                        // 2. Gọi thẳng ViewModel
                        viewModel.updateUserProfile( // 👈 3. Gọi hàm đúng tên
                            imageUri = newImageUri, // 👈 4. Truyền Uri vào
                            username = username,
                            campus = campus,
                            phone = phoneNumber,
                            major = major,
                            onSuccess = {
                                isUploading = false
                                Toast.makeText(
                                    context,
                                    "Cập nhật thành công",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSaveSuccess()
                            },
                            onError = { msg ->
                                isUploading = false
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )


        }
        if (isUploading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF06635A)
            )
        }
    }
}
