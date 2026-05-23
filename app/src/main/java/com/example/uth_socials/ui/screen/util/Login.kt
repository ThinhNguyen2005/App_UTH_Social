package com.example.uth_socials.ui.screen.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uth_socials.ui.component.button.ComfirmAuthButton
import com.example.uth_socials.ui.component.button.GoogleButton
import com.example.uth_socials.ui.component.common.BannerTone
import com.example.uth_socials.ui.component.common.DividerWithText
import com.example.uth_socials.ui.component.common.FieldHint
import com.example.uth_socials.ui.component.common.InputTextField
import com.example.uth_socials.ui.component.common.PasswordTextField
import com.example.uth_socials.ui.component.common.StatusBanner
import com.example.uth_socials.ui.component.common.rememberConnectivityState
import com.example.uth_socials.ui.component.logo.LogoTopAppBar
import com.example.uth_socials.ui.viewmodel.AuthState
import com.example.uth_socials.ui.viewmodel.AuthViewModel

private val BrandPrimary = Color(0xFF06635A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onGoogleLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    onResetPasswordClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOnline by rememberConnectivityState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginMethod by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val emailLooksValid by remember(email) {
        derivedStateOf {
            email.isEmpty() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }
    }

    LaunchedEffect(state) {
        when (val s = state) {
            is AuthState.Success -> {
                apiError = null
                successMessage = s.message
                viewModel.resetState()
                onLoginSuccess()
            }
            is AuthState.Error -> {
                apiError = mapAuthError(s.message, isOnline)
                successMessage = null
                viewModel.resetState()
            }
            else -> Unit
        }
        if (state !is AuthState.Loading) loginMethod = null
    }

    RequestNotificationPermission()

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = { LogoTopAppBar(onLogoClick = {}) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                text = "Đăng nhập",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Chào mừng trở lại UTH Social",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6B6A),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = !isOnline,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
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
                    title = "Đăng nhập thất bại",
                    message = apiError ?: "",
                    tone = BannerTone.Error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            AnimatedVisibility(visible = successMessage != null) {
                StatusBanner(
                    icon = Icons.Filled.CheckCircle,
                    title = "Thành công",
                    message = successMessage ?: "",
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
                },
                label = "Email"
            )
            if (email.isNotEmpty() && !emailLooksValid) {
                FieldHint("Email chưa đúng định dạng", isError = true)
            }

            Spacer(modifier = Modifier.height(12.dp))

            PasswordTextField(
                value = password,
                onValueChange = { newPassword ->
                    password = newPassword.filter { !it.isWhitespace() }
                    if (apiError != null) apiError = null
                    if (formError != null) formError = null
                },
                label = "Mật khẩu"
            )

            if (formError != null) {
                FieldHint(formError ?: "", isError = true)
            }

            TextButton(
                onClick = onResetPasswordClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Quên mật khẩu?", color = BrandPrimary, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            ComfirmAuthButton(
                text = "Đăng nhập",
                isLoading = state is AuthState.Loading && loginMethod == "email",
                enabled = isOnline,
                onClick = {
                    val trimmedEmail = email.trim()
                    when {
                        trimmedEmail.isEmpty() || password.isEmpty() ->
                            formError = "Vui lòng nhập email và mật khẩu."
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() ->
                            formError = "Email chưa đúng định dạng."
                        !isOnline ->
                            formError = "Bạn đang ngoại tuyến. Hãy kết nối Internet trước."
                        else -> {
                            formError = null
                            apiError = null
                            successMessage = null
                            loginMethod = "email"
                            viewModel.login(trimmedEmail, password)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onRegisterClick) {
                Text("Chưa có tài khoản? Đăng ký", color = Color(0xFF5C6B6A), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(20.dp))

            DividerWithText("hoặc")

            Spacer(modifier = Modifier.height(20.dp))

            GoogleButton(
                isLoading = state is AuthState.Loading && loginMethod == "google",
                onClick = {
                    if (!isOnline) {
                        formError = "Bạn đang ngoại tuyến. Hãy kết nối Internet trước."
                    } else {
                        loginMethod = "google"
                        apiError = null
                        onGoogleLoginClick()
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun mapAuthError(raw: String, isOnline: Boolean): String {
    if (!isOnline) return "Bạn đang ngoại tuyến. Hãy kết nối Internet rồi thử lại."
    val low = raw.lowercase()
    return when {
        "no internet" in low || "network" in low || "unable to resolve host" in low ->
            "Mất kết nối tới máy chủ. Kiểm tra mạng và thử lại."
        "password is invalid" in low || "wrong-password" in low || "sai" in low ->
            "Sai email hoặc mật khẩu."
        "no user record" in low || "user-not-found" in low ->
            "Tài khoản này chưa được đăng ký."
        "too many requests" in low ->
            "Bạn thử quá nhiều lần. Vui lòng đợi vài phút."
        "user-disabled" in low ->
            "Tài khoản đã bị vô hiệu hoá."
        "badly formatted" in low || "invalid-email" in low ->
            "Email chưa đúng định dạng."
        raw.isBlank() -> "Đã xảy ra lỗi. Vui lòng thử lại."
        else -> raw
    }
}
