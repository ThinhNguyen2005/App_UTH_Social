package com.example.uth_socials

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.component.button.GoogleButton
import com.example.uth_socials.ui.screen.util.RequestNotificationPermission


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

    RequestNotificationPermission()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // UTH Social Logo
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFF4DB6AC), fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                    append("UTH")
                }
                withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold, fontSize =24.sp)) {
                    append(" Social")
                }
            },
            modifier = Modifier.align(Alignment.Start)
        )

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

        // Email TextField
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color(0xFF06635A),
                unfocusedIndicatorColor = Color(0xFFB0BEC5),
                focusedContainerColor = Color(0xFFF1F4FF),
                unfocusedContainerColor = Color(0xFFF1F4FF) ,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(16.dp))


        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
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



        TextButton(
            onClick = onResetPasswordClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Quên mật khẩu rồi?", color = Color(0xFF00897B))
        }

        Spacer(modifier = Modifier.height(24.dp))


        ComfirmAuthButton(
            text = "Đăng nhập",
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                } else {
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
            onClick=onGoogleLoginClick
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ViewModel State Handling
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

