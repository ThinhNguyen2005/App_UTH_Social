package com.example.uth_socials.ui.screen.post

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.uth_socials.R
import com.example.uth_socials.ui.component.button.PrimaryButton
import com.example.uth_socials.ui.component.navigation.HomeBottomNavigation
import com.example.uth_socials.ui.logo.HomeTopAppBarPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = { HomeTopAppBarPrimary() },
        bottomBar = { HomeBottomNavigation() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {


            // --- Nút chọn ảnh ---
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(4.dp)
//                ) {
//                    Icon(
//                        imageVector = Icons.Outlined.Image,
//                        contentDescription = null,
//                        tint = Color.Black
//                    )
//                    Text(
//                        text = "Chọn ảnh",
//                        fontSize = 14.sp,
//                        color = Color.Black
//                    )
//                }
            // }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text("SKT_DucT1", fontWeight = FontWeight.Medium)
                }

                PrimaryButton(
                    buttonColor = ButtonDefaults.buttonColors(containerColor = Color(0xFF007E8F)),
                    text = "Đăng",
                    textColor = Color.White,
                    modifier = Modifier
                        .width(100.dp)
                        .height(40.dp),
                    onClick = {})
            }



// --- Upload and tabs ---
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        120.dp,
                        alignment = Alignment.CenterHorizontally
                    )

                ) {
                    TabButton(
                        text = "Bài viết",
                        isSelected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 }
                    )

                    // Tab "Sản phẩm"
                    TabButton(
                        text = "Sản phẩm",
                        isSelected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 }
                    )
                }
            }
        }
    }
}