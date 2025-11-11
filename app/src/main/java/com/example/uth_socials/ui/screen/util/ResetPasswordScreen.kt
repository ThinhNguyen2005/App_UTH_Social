package com.example.uth_socials.ui.screen.util

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.component.button.GoogleButton
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel

@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onGoogleClick:() -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

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
            text = "Khôi phục mật khẩu",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF06635A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quay lại cùng UTH Social",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                unfocusedContainerColor = Color(0xFFF1F4FF),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(Modifier.height(48.dp))

        ComfirmAuthButton(
            text = "Xác nhận email",
            onClick = {
                if (email.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.resetPassword(email)
                }
            },
        )



        TextButton(onClick = onBackToLogin) {
            Text("Quay trở lại", color = Color.Gray, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.weight(1f))

        GoogleButton (
            onClick = onGoogleClick
        )

        Spacer(Modifier.height(32.dp))
        when (state) {
            is AuthState.Success -> {
                Toast.makeText(context, (state as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, (state as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }
}
