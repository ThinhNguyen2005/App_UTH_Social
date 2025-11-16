package com.example.uth_socials.ui.screen.util

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
import androidx.navigation.NavHostController
import com.example.uth_socials.R
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.component.button.GoogleButton
import com.example.uth_socials.ui.component.common.InputTextField
import com.example.uth_socials.ui.component.common.PasswordTextField
import com.example.uth_socials.ui.component.navigation.Screen
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel

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
                withStyle(style = SpanStyle(color = Color(0xFF06635A), fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                    append("UTH")
                }
                withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 24.sp)) {
                    append(" Social")
                }
            },
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(40.dp))

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
            onValueChange = {username=it},
            label = "Tên người dùng"
        )

        Spacer(modifier = Modifier.height(12.dp))

        InputTextField(
            value=email,
            onValueChange = {email=it},
            label = "Email"
        )

        Spacer(modifier = Modifier.height(12.dp))

        PasswordTextField(
            value = password,
            onValueChange={password=it},
        )

        Spacer(modifier = Modifier.height(12.dp))

        PasswordTextField(
            value= confirmPassword,
            onValueChange = {confirmPassword=it},
            label = "Nhập lại mật khẩu"
        )

        Spacer(modifier = Modifier.height(24.dp))

        ComfirmAuthButton(
            text = "Đăng kí",
            onClick = {
                when {
                    email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()||username.isBlank() ->
                        Toast.makeText(context, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                    password != confirmPassword ->
                        Toast.makeText(context, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                    else -> viewModel.register(email, password,username)
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Bạn đã có tài khoản rồi !", color = Color.Gray, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(100.dp))

        GoogleButton(
            onClick=onGoogleClick
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (state) {
            is AuthState.Loading ->{
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom=50.dp),
                )
            }
            is AuthState.Success -> {
                Toast.makeText(context, (state as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onRegisterSuccess()

            }
            is AuthState.Error -> {
                Toast.makeText(context, (state as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }
}
