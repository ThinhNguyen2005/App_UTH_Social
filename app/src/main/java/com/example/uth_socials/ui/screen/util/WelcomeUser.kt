package com.example.uth_socials.ui.screen.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.R
import com.example.uth_socials.ui.component.button.PrimaryButton


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
                .padding(innerPadding).padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Image(
                painter = painterResource(id = R.drawable.ic_useruth),
                contentDescription = "Welcome illustration",
                modifier = Modifier
                    .fillMaxWidth()
            )

            // Tiêu đề + mô tả
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(bottom = 35.dp)
            ) {
                Text(
                    text = "Chia sẻ cùng UTH",
                    color = Color(0xFF007E8F),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Tự do chia sẻ kinh nghiệm, trải nghiệm của bạn cùng UTH",
                    color = Color.Gray,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Hai nút Đăng nhập / Đăng ký
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                PrimaryButton(
                    buttonColor = ButtonDefaults.buttonColors(containerColor = Color(0xFF007E8F)),
                    text = "Đăng nhập",
                    textColor = Color.White,
                    modifier = Modifier.width(140.dp).height(48.dp),
                    onClick = onLoginClick
                )

                PrimaryButton(
                    buttonColor = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF)),
                    text = "Đăng ký",
                    textColor = Color.Black,
                    modifier = Modifier.width(140.dp).height(48.dp),
                    onClick = onRegisterClick
                )
            }
            Spacer(modifier = Modifier.weight(1f))

        }
    }

}

@Preview(name = "Portrait Mode", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun Asd() {
    WelcomeScreen({}, {})
}