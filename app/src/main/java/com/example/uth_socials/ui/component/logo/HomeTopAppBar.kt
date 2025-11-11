package com.example.uth_socials.ui.component.logo

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.uth_socials.R
import com.example.uth_socials.ui.screen.UthRed
import com.example.uth_socials.ui.screen.UthTeal
import com.example.uth_socials.data.repository.AdminRepository
import androidx.compose.runtime.produceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    onSearchClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onAdminClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = UthTeal, fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                        append("UTH")
                    }
                    withStyle(style = SpanStyle(color = UthRed, fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                        append(" Social")
                    }
                }
            )
        },
        actions = {
            // Check if user is admin and show admin button (non-blocking)
            val isAdmin by produceState(initialValue = false) {
                value = try {
                    AdminRepository().getCurrentUserAdminStatus() != AdminRepository.AdminStatus.USER
                } catch (e: Exception) {
                    Log.e("HomeTopAppBar", "Gặp lỗi khi kiểm tra admin status", e)
                    false
                }
            }

            if (isAdmin && onAdminClick != null) {
                IconButton(onClick = onAdminClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Admin Dashboard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onMessagesClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_chat),
                    contentDescription = "Messages",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onSearchClick) {
                Icon(
//                    imageVector = Icons.Outlined.Search,
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_search),

                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = UthTeal, fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                        append("UTH")
                    }
                    withStyle(style = SpanStyle(color = UthRed, fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                        append(" Social")
                    }
                }
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        scrollBehavior = scrollBehavior
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlyLogo(
    userName: String,
    onBackClick: () -> Unit
) {
    val appBarColor = MaterialTheme.colorScheme.surface
    val onAppBarColor = MaterialTheme.colorScheme.onSurface

    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = onAppBarColor
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = onAppBarColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = appBarColor,
            titleContentColor = onAppBarColor,
            navigationIconContentColor = onAppBarColor
        ),
        windowInsets = WindowInsets(0.dp)
    )
}
