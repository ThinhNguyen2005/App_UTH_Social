package com.example.uth_socials.ui.screen.util

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.component.button.GoogleButton
import com.example.uth_socials.ui.component.common.InputTextField
import com.example.uth_socials.ui.component.common.PasswordTextField
import com.example.uth_socials.ui.component.logo.LogoTopAppBar
import com.example.uth_socials.ui.component.navigation.Graph
import com.example.uth_socials.ui.component.navigation.Screen
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
            LogoTopAppBar(onLogoClick = {})
        },
    ) { innerpadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerpadding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
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

                        else -> { 
                            viewModel.register(email, password, username)
                        }
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

            Spacer(modifier = Modifier.weight(1f))

            when (state) {
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
            
            GoogleButton(
                onClick = onGoogleClick
            )
        }
    }
}