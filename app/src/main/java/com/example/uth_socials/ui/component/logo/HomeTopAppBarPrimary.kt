package com.example.uth_socials.ui.component.logo

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonElevation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.ui.screen.util.UthRed
import com.example.uth_socials.ui.theme.UthTeal


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBarPrimary(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onLogoClick: () -> Unit
) {
    TopAppBar(
        title = {

            var uth_text by remember { mutableStateOf("UTH") }
            var social_text by remember { mutableStateOf("Social") }

            TextButton(
                onClick = onLogoClick,
                colors = ButtonDefaults.buttonColors(
                    //containerColor = Color.Transparent, // nền nhạt
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
            ){
                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = UthTeal,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(uth_text)
                        }
                        withStyle(
                            style = SpanStyle(
                                color = UthRed,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(social_text)
                        }
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        scrollBehavior = scrollBehavior
    )
}
