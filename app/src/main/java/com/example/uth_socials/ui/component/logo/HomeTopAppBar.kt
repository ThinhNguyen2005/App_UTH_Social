package com.example.uth_socials.ui.component.logo

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    onSearchClick: () -> Unit,
    onMessagesClick: () -> Unit,
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
fun ChatTopAppBar(
    userName: String,
    avatarUrl: String,
    onBackClick: () -> Unit
) {
    val darkAppBarColor = Color(0xFF1A2838)
    val onDarkAppBarColor = Color.White

    TopAppBar(
        modifier = Modifier
            .fillMaxWidth(),
         // ✅ Giới hạn chiều cao, không bị “phình”
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = avatarUrl.ifEmpty {
                        "https://cdn-icons-png.flaticon.com/512/149/149071.png"
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = onDarkAppBarColor
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = onDarkAppBarColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = darkAppBarColor
        ),
        windowInsets = WindowInsets(0.dp)

    )
}
