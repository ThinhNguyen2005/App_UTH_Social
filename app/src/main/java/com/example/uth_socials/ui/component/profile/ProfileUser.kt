package com.example.uth_socials.ui.component.profile // Đặt trong package phù hợp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHeader(
    username: String,
    avatarUrl: String?,
    bio: String,
    followers: Int,
    following: Int,
    postCount: Int = 0,
    isOwner: Boolean,
    isFollowing: Boolean,
    onFollowClicked: () -> Unit,
    showBackButton: Boolean = true,
    onBackClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onMessageClicked: () -> Unit = {},
    onEditProfileClicked: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Background content: Info card and tabs
        Column {
            ProfileInfoCard(
                username = username,
                avatarUrl = avatarUrl,
                bio = bio,
                followers = followers,
                following = following,
                postCount = postCount,
                isOwner = isOwner,
                isFollowing = isFollowing,

                onFollowClicked = onFollowClicked,
                onMessageClicked = onMessageClicked,
                onEditProfileClicked = onEditProfileClicked
            )
            ProfileTabs()
        }

        // Overlay content: Header with back button and menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.TopCenter), // Align to the top of the Box
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Box {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More options"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!isOwner) {
                        DropdownMenuItem(
                            text = { Text("Chặn người dùng") },
                            onClick = {
                                showMenu = false
                                onMoreClicked()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Báo cáo") },
                            onClick = {
                                showMenu = false
                                // TODO: Implement report flow
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Cài đặt") },
                            onClick = {
                                showMenu = false
                                onEditProfileClicked()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(
    username: String,
    avatarUrl: String?,
    bio: String,
    followers: Int,
    following: Int,
    postCount: Int = 0,
    isOwner: Boolean,
    isFollowing: Boolean,
    onFollowClicked: () -> Unit,
    onMessageClicked: () -> Unit,
    onEditProfileClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier.padding(top = 40.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                ProfileStats(
                    postCount = postCount,
                    followers = followers,
                    following = following
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (bio.isNotEmpty()) {
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                ProfileActions(
                    isOwner = isOwner,
                    isFollowing = isFollowing,
                    onFollowClicked = onFollowClicked,
                    onMessageClicked = onMessageClicked,
                    onEditProfileClicked = onEditProfileClicked
                )
            }
        }

        AsyncImage(
            model = avatarUrl.takeIf { it?.isNotEmpty() == true },  // ✅ Handle nullable and empty
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(BorderStroke(4.dp, MaterialTheme.colorScheme.surface), CircleShape)
                .align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ProfileStats(postCount: Int, followers: Int, following: Int) {
    // Thẻ hiển thị thông tin số liệu
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(count = postCount, label = "bài viết")
            StatItem(count = followers, label = "người theo dõi")
            StatItem(count = following, label = "đang theo dõi")
        }
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileActions(
    isOwner: Boolean,
    isFollowing: Boolean,
    onFollowClicked: () -> Unit,
    onMessageClicked: () -> Unit,
    onEditProfileClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (isOwner) {
            // Khi là chủ nhân, chỉ có 1 nút
            FilledTonalButton(
                onClick = onEditProfileClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) {
                Text("Chỉnh sửa trang cá nhân")
            }
        } else {
            // Khi là khách, 2 nút sẽ hiển thị dạng stack theo hình mẫu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nút nhắn tin
                OutlinedButton(
                    onClick = onMessageClicked,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Nhắn tin")
                }


                // Nút theo dõi
                if (isFollowing) {
                    // Nếu đã theo dõi, hiển thị nút outlined
                    OutlinedButton(
                        onClick = onFollowClicked,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),

                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Đang theo dõi")
                    }
                } else {
                    // Nếu chưa theo dõi, hiển thị nút filled
                    Button(
                        onClick = onFollowClicked,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Theo dõi")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTabs() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Bài viết", "Sản phẩm")

    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTabIndex = index }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) selectedColor else unselectedColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    // Indicator line
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(40.dp)
                            .background(
                                color = if (isSelected) selectedColor else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            }
        }

        // Divider
        Divider(
            color = Color(0xFFEEEEEE),
            thickness = 1.dp
        )
    }
}


@Preview(showBackground = true, name = "Owner View")
@Composable
private fun ProfileHeaderPreviewOwner() {
    MaterialTheme {
        Surface {
            ProfileHeader(
                username = "Trần Văn A",
                avatarUrl = "https://picsum.photos/id/237/200/300",
                bio = "Đây là bio của tôi. Tôi thích lập trình và đi du lịch.",
                followers = 1250,
                following = 340,
                postCount = 42,
                isOwner = true,
                isFollowing = false, // Not relevant for owner
                onFollowClicked = {},
                showBackButton = true,
                onBackClicked = {},
                onMoreClicked = {},
                onMessageClicked = {},
                onEditProfileClicked = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Visitor View - Not Following")
@Composable
private fun ProfileHeaderPreviewVisitorNotFollowing() {
    MaterialTheme {
        Surface {
            ProfileHeader(
                username = "Nguyễn Thị B",
                avatarUrl = "https://picsum.photos/id/1/200/300",
                bio = "Chào mừng đến với trang cá nhân của mình!",
                followers = 800,
                following = 150,
                postCount = 25,
                isOwner = false,
                isFollowing = false,
                onFollowClicked = {},
                showBackButton = true,
                onBackClicked = {},
                onMoreClicked = {},
                onMessageClicked = {},
                onEditProfileClicked = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Visitor View - Following")
@Composable
private fun ProfileHeaderPreviewVisitorFollowing() {
    MaterialTheme {
        Surface {
            ProfileHeader(
                username = "Nguyễn Thị B",
                avatarUrl = "https://picsum.photos/id/1/200/300",
                bio = "Chào mừng đến với trang cá nhân của mình!",
                followers = 801,
                following = 150,
                postCount = 25,
                isOwner = false,
                isFollowing = true,
                onFollowClicked = {},
                showBackButton = true,
                onBackClicked = {},
                onMoreClicked = {},
                onMessageClicked = {},
                onEditProfileClicked = {}
            )
        }
    }
}