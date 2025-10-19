package com.example.uth_socials.ui.logo

import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

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
