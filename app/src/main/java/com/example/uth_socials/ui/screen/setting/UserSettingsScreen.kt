package com.example.uth_socials.ui.screen.setting

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.ui.component.common.PasswordTextField
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.viewmodel.UserInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingScreen(
    viewModel: UserInfoViewModel = viewModel(),
    authViewModel: AuthViewModel,
    onBackClicked: () -> Unit,
    onNavigateToUserInfo: () -> Unit,
    onLogout: () -> Unit
) {


    val username by viewModel.username.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()

    val isEmailPasswordUser = remember { authViewModel.isEmailPasswordUser() }
    val context = LocalContext.current

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    val authState by authViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }


// 🔹 3. Lắng nghe state từ ViewModel để hiển thị Toast
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                showChangePasswordDialog = false // Đóng dialog khi thành công
                authViewModel.resetState() // Reset lại state
            }
            is AuthState.Error -> {
                // Lỗi sẽ được hiển thị bên trong Dialog, không cần Toast ở đây
            }
            else -> {}
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài đặt tài khoản",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 🔹 Header người dùng
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = avatarUrl.ifBlank { "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da" },
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text =username.ifBlank { "Người dùng" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // 🔹 Danh sách các mục
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Thông Tin Cá Nhân",
                onClick = onNavigateToUserInfo
            )
            SettingsItem(
                icon = Icons.Default.Visibility,
                title = "Chế độ tôi",
                onClick = { /* TODO */ }
            )
            SettingsItem(
                icon = Icons.Default.BookmarkBorder,
                title = "Xem bài viết đã lưu",
                onClick = { /* TODO */ }
            )
            if (isEmailPasswordUser) {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Đổi Mật Khẩu",
                    onClick = {
                        authViewModel.resetState()
                        showChangePasswordDialog = true
                    }
                )
            }
            SettingsItem(
                icon = Icons.Default.People,
                title = "Danh sách người theo dõi bạn",
                onClick = { /* TODO */ }
            )
            SettingsItem(
                icon = Icons.Default.Block,
                title = "Danh sách người dùng đã chặn",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.weight(1f))

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // 🔹 Đăng xuất
            SettingsItem(
                icon = Icons.Default.Logout,
                title = "Đăng Xuất",
                color = MaterialTheme.colorScheme.error,
                onClick = onLogout

            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            authState = authState, // Truyền state vào
            onDismiss = {
                showChangePasswordDialog = false
                authViewModel.resetState() // Đóng thì reset state
            },
            onChangePassword = { old, new ->
                authViewModel.changePassword(old, new)
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
@Composable
private fun ChangePasswordDialog(
    authState: AuthState,
    onDismiss: () -> Unit,
    onChangePassword: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isLoading = authState is AuthState.Loading
    // Lấy lỗi từ API (ViewModel)
    val apiError = if (authState is AuthState.Error) authState.message else null

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Dùng lại Composable PasswordTextField đã có
                PasswordTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; validationError = null },
                    label = "Mật khẩu cũ"
                )
                PasswordTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; validationError = null },
                    label = "Mật khẩu mới"
                )
                PasswordTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; validationError = null },
                    label = "Xác nhận mật khẩu mới"
                )

                // Hiển thị lỗi validation (client-side) hoặc lỗi API (server-side)
                val errorToShow = validationError ?: apiError
                if (errorToShow != null) {
                    Text(
                        text = errorToShow,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 1. Reset lỗi validation
                    validationError = null

                    // 2. Kiểm tra validation phía client
                    if (oldPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                        validationError = "Vui lòng nhập đủ các trường."
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        validationError = "Mật khẩu mới phải có ít nhất 6 ký tự."
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        validationError = "Mật khẩu mới không khớp."
                        return@Button
                    }
                    if(newPassword==oldPassword){
                        validationError="Mật khẩu mới không được trùng mật khẩu cũ"
                        return@Button
                    }

                    // 3. Nếu ổn, gọi ViewModel
                    onChangePassword(oldPassword, newPassword)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Lưu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Hủy")
            }
        }
    )
}