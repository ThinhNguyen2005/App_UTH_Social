package com.example.uth_socials.ui.screen.util

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.component.button.GoogleButton
import com.example.uth_socials.ui.component.common.InputTextField
import com.example.uth_socials.ui.component.common.PasswordTextField
import com.example.uth_socials.ui.component.logo.LogoTopAppBar
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onGoogleClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            LogoTopAppBar()
        },
    ) { innerpadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerpadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {


            Text(
                text = "Tạo tài khoản",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF06635A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tạo tài khoản để kết nối nhiều bạn bè hơn",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            InputTextField(
                value = username,
                onValueChange = { username = it },
                label = "Tên người dùng"
            )

            Spacer(modifier = Modifier.height(12.dp))

            InputTextField(
                value = email,
                onValueChange = { newEmail ->
                    email = newEmail.filter{!it.isWhitespace()}
                },
                label = "Email"
            )

            Spacer(modifier = Modifier.height(12.dp))

            PasswordTextField(
                value = password,
                onValueChange = { newPassword ->
                    password = newPassword.filter{!it.isWhitespace()}
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            PasswordTextField(
                value = confirmPassword,
                onValueChange = { newPassword ->
                    confirmPassword = newPassword.filter{!it.isWhitespace()}
                },
                label = "Nhập lại mật khẩu"
            )

            Spacer(modifier = Modifier.height(24.dp))

            ComfirmAuthButton(
                text = "Đăng kí",
                onClick = {
                    when {
                        email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || username.isBlank() ->
                            Toast.makeText(
                                context,
                                "Vui lòng nhập đủ thông tin",
                                Toast.LENGTH_SHORT
                            ).show()

                        password != confirmPassword ->
                            Toast.makeText(context, "Mật khẩu không khớp", Toast.LENGTH_SHORT)
                                .show()

                        else -> viewModel.register(email, password, username)
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onBackToLogin) {
                Text(
                    "Bạn đã có tài khoản rồi !",
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(100.dp))

            GoogleButton(
                onClick = onGoogleClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (state) {
                is AuthState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(bottom = 50.dp),
                    )
                }

                is AuthState.Success -> {
                    Toast.makeText(
                        context,
                        (state as AuthState.Success).message,
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetState()
                    onRegisterSuccess()

                }

                is AuthState.Error -> {
                    Toast.makeText(context, (state as AuthState.Error).message, Toast.LENGTH_SHORT)
                        .show()
                    viewModel.resetState()
                }

                else -> {}
            }
        }
    }
}