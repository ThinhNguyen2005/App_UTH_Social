package com.example.uth_socials.ui.component.logo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    onSearchClick: (String) -> Unit,
    onMessagesClick: () -> Unit,
    onAdminClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    var isSearching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    // Box {
    TopAppBar(
        title = {
            if (!isSearching) {
                var uth_text by remember { mutableStateOf("") }
                var social_text by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    delay(200)
                    uth_text = "UTH"
                    social_text = " Social"
                }

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
        actions = {

            val isAdmin by produceState(initialValue = false) {
                value = try {
                    AdminRepository().getCurrentUserAdminStatus() != com.example.uth_socials.config.AdminStatus.USER
                } catch (e: Exception) {
                    false
                }
            }

            if (isAdmin && onAdminClick != null && !isSearching) {
                IconButton(onClick = onAdminClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Admin Dashboard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(
                onClick = onMessagesClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent // ẩn background
                    //disabledContainerColor = Color.Transparent
                ),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_chat),
                    contentDescription = "Messages",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            if (!isSearching) {
                IconButton(
                    onClick = { isSearching = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent // ẩn background
                    ),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

            }

            AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn(animationSpec = tween(500)) + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut(animationSpec = tween(500)) + shrinkHorizontally(shrinkTowards = Alignment.End),
            ) {
                // Thanh tìm kiếm mở rộng ra
                var expandedWidth by remember { mutableStateOf(350.dp) } // bắt đầu nhỏ
                val keyboardController = LocalSoftwareKeyboardController.current

                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Tìm kiếm...") },
                    //textStyle = TextStyle(fontSize = 3.sp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = Color.Transparent,//MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),

                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchText.isNotEmpty()) {
                                onSearchClick(searchText)
                            }
                            keyboardController?.hide()
                            isSearching = false
                            searchText = ""
                        }
                    ),
                    modifier = Modifier
                        .width(expandedWidth)
                        .height(55.dp)//.fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    //.background(color = Color.Transparent)
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchText.isNotEmpty()) {
                                onSearchClick(searchText)
                            }
                            isSearching = false
                            searchText = ""
                        }) {
                            if (searchText.isEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_search),
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
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
                    withStyle(
                        style = SpanStyle(
                            color = UthTeal,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("UTH")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = UthRed,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
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