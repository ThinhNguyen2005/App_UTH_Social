package com.example.uth_socials.ui.screen.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.component.button.GoogleButton
import com.example.uth_socials.ui.component.common.BannerTone
import com.example.uth_socials.ui.component.common.FieldHint
import com.example.uth_socials.ui.component.common.InputTextField
import com.example.uth_socials.ui.component.common.StatusBanner
import com.example.uth_socials.ui.component.common.rememberConnectivityState
import com.example.uth_socials.ui.component.logo.LogoTopAppBar
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel

private val BrandPrimary = Color(0xFF06635A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onGoogleClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOnline by rememberConnectivityState()

    var email by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var sentToEmail by remember { mutableStateOf<String?>(null) }

    val emailLooksValid by remember(email) {
        derivedStateOf {
            email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is AuthState.Success -> {
                apiError = null
                sentToEmail = email.trim()
                viewModel.resetState()
            }
            is AuthState.Error -> {
                apiError = mapResetError(s.message, isOnline)
                sentToEmail = null
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = { LogoTopAppBar(onLogoClick = {}) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Quên mật khẩu",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Nhập email đã đăng ký, chúng tôi sẽ gửi liên kết đặt lại mật khẩu.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6B6A),
                lineHeight = 20.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(visible = !isOnline, enter = fadeIn(), exit = fadeOut()) {
                StatusBanner(
                    icon = Icons.Outlined.WifiOff,
                    title = "Không có kết nối mạng",
                    message = "Vui lòng kiểm tra Wi-Fi hoặc dữ liệu di động và thử lại.",
                    tone = BannerTone.Warning
                )
            }

            AnimatedVisibility(visible = apiError != null) {
                StatusBanner(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "Không gửi được email",
                    message = apiError ?: "",
                    tone = BannerTone.Error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            AnimatedVisibility(visible = sentToEmail != null) {
                StatusBanner(
                    icon = Icons.Filled.CheckCircle,
                    title = "Đã gửi email đặt lại mật khẩu",
                    message = "Hãy kiểm tra hộp thư của ${sentToEmail ?: ""} (kể cả mục Spam) và làm theo hướng dẫn.",
                    tone = BannerTone.Success,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            InputTextField(
                value = email,
                onValueChange = { newEmail ->
                    email = newEmail.filter { !it.isWhitespace() }
                    if (apiError != null) apiError = null
                    if (formError != null) formError = null
                    if (sentToEmail != null) sentToEmail = null
                },
                label = "Email"
            )
            if (email.isNotEmpty() && !emailLooksValid) {
                FieldHint("Email chưa đúng định dạng", isError = true)
            } else if (formError != null) {
                FieldHint(formError ?: "", isError = true)
            } else {
                FieldHint("Ví dụ: ten.cua.ban@ut.edu.vn")
            }

            Spacer(modifier = Modifier.height(32.dp))

            ComfirmAuthButton(
                text = if (sentToEmail != null) "Gửi lại email" else "Gửi email đặt lại",
                isLoading = state is AuthState.Loading,
                enabled = isOnline,
                onClick = {
                    val trimmedEmail = email.trim()
                    when {
                        trimmedEmail.isEmpty() ->
                            formError = "Vui lòng nhập email."
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() ->
                            formError = "Email chưa đúng định dạng."
                        !isOnline ->
                            formError = "Bạn đang ngoại tuyến. Hãy kết nối Internet trước."
                        else -> {
                            formError = null
                            apiError = null
                            viewModel.resetPassword(trimmedEmail)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quay lại đăng nhập",
                    color = BrandPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            StatusBanner(
                icon = Icons.Outlined.MailOutline,
                title = "Không nhận được email?",
                message = "Vui lòng đợi vài phút, kiểm tra mục Spam, hoặc thử gửi lại. Liên hệ hỗ trợ nếu vẫn không thấy.",
                tone = BannerTone.Info
            )

            Spacer(modifier = Modifier.height(32.dp))

            GoogleButton(onClick = onGoogleClick)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun mapResetError(raw: String, isOnline: Boolean): String {
    if (!isOnline) return "Bạn đang ngoại tuyến. Hãy kết nối Internet rồi thử lại."
    val low = raw.lowercase()
    return when {
        "no internet" in low || "network" in low || "unable to resolve host" in low ->
            "Mất kết nối tới máy chủ. Kiểm tra mạng và thử lại."
        "no user record" in low || "user-not-found" in low ->
            "Email này chưa được đăng ký trong UTH Social."
        "too many requests" in low ->
            "Bạn yêu cầu quá nhiều lần. Vui lòng đợi vài phút."
        "badly formatted" in low || "invalid-email" in low ->
            "Email chưa đúng định dạng."
        raw.isBlank() -> "Đã xảy ra lỗi. Vui lòng thử lại."
        else -> raw
    }
}
