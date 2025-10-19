package com.example.uth_socials


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel


@Composable
fun LoginScreen(
    viewModel: AuthViewModel,             // <-- Dùng ViewModel chuẩn
    onGoogleLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState() // <-- Lấy state từ AuthViewModel

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Đăng nhập Firebase", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Nhập email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        // Nhập mật khẩu
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nút đăng nhập
        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.login(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng nhập")
        }

        // Nút đăng ký
        OutlinedButton(
            onClick = onRegisterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng ký tài khoản mới")
        }

        // Nút đăng nhập Google
        OutlinedButton(
            onClick = onGoogleLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng nhập Google")
        }

        // Hiển thị trạng thái từ ViewModel
        when (state) {
            is AuthState.Loading -> CircularProgressIndicator()

            is AuthState.Success -> {
                Toast.makeText(context, (state as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onLoginSuccess()
            }

            is AuthState.Error -> {
                Toast.makeText(context, (state as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }

            else -> {}
        }
    }
}
