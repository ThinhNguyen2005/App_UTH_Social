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
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.component.button.GoogleButton
import com.example.uth_socials.ui.component.common.InputTextField
import com.example.uth_socials.ui.component.common.PasswordTextField
import com.example.uth_socials.ui.component.logo.LogoTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onGoogleLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    onResetPasswordClick: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginMethod by remember { mutableStateOf<String?>(null) }

    // Reset loginMethod when state is not Loading
    LaunchedEffect(state) {
        if (state !is AuthState.Loading) {
            loginMethod = null
        }
    }

    RequestNotificationPermission()
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
            Spacer(modifier = Modifier.height(60.dp))



            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Đăng nhập",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF06635A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Chào mừng trở lại UTH",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                fontWeight = FontWeight.Bold

            )

            Spacer(modifier = Modifier.height(32.dp))

            InputTextField(
                value = email,
                onValueChange = { newEmail ->
                    email = newEmail.filter{!it.isWhitespace()}
                },
                label = "Email",
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordTextField(
                value = password,
                onValueChange = { newPassword ->
                    password = newPassword.filter{!it.isWhitespace()}
                },
                label = "Mật khẩu"
            )

            TextButton(
                onClick = onResetPasswordClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Quên mật khẩu rồi?", color = Color(0xFF00897B))
            }

            Spacer(modifier = Modifier.height(24.dp))


            ComfirmAuthButton(
                text = "Đăng nhập",
                isLoading = state is AuthState.Loading && loginMethod == "email",
                onClick = {
                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Vui lòng nhập email và mật khẩu",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        loginMethod = "email"
                        viewModel.login(email, password)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onRegisterClick) {
                Text("Tạo tài khoản mới", color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            GoogleButton(
                isLoading = state is AuthState.Loading && loginMethod == "google",
                onClick = {
                    loginMethod = "google"
                    onGoogleLoginClick()
                }
            )

            Spacer(modifier = Modifier.height(50.dp))

            // ViewModel State Handling
            when (state) {
                is AuthState.Success -> {
                    Toast.makeText(
                        context,
                        (state as AuthState.Success).message,
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetState()
                    onLoginSuccess()
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