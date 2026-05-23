package com.example.uth_socials.ui.component.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.uth_socials.data.market.Product

@Composable
fun ProfileHeader(
    username: String,
    avatarUrl: String?,
    bio: String,
    followers: Int,
    following: Int,
    postCount: Int = 0,
    products: List<Product>,
    isOwner: Boolean,
    isFollowing: Boolean,
    onFollowClicked: () -> Unit,
    showBackButton: Boolean = true,
    onBackClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onMessageClicked: () -> Unit = {},
    onSettingClicked: () -> Unit = {},
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileTopBar(
            username = username,
            showBackButton = showBackButton,
            onBackClicked = onBackClicked,
            isOwner = isOwner,
            onMoreClicked = onMoreClicked,
            onSettingClicked = onSettingClicked
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = avatarUrl.takeIf { !it.isNullOrEmpty() },
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(20.dp))
            ProfileStatsRow(
                postCount = postCount,
                productCount = products.size,
                followers = followers,
                following = following,
                modifier = Modifier.weight(1f)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (bio.isNotBlank()) {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ProfileActions(
            isOwner = isOwner,
            isFollowing = isFollowing,
            onFollowClicked = onFollowClicked,
            onMessageClicked = onMessageClicked,
            onSettingClicked = onSettingClicked
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun ProfileTopBar(
    username: String,
    showBackButton: Boolean,
    onBackClicked: () -> Unit,
    isOwner: Boolean,
    onMoreClicked: () -> Unit,
    onSettingClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại"
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
        Text(
            text = username,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        Box {
            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Thêm"
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!isOwner) {
                    DropdownMenuItem(
                        text = { Text("Chặn người dùng") },
                        onClick = { showMenu = false; onMoreClicked() }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Cài đặt") },
                        onClick = { showMenu = false; onSettingClicked() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatsRow(
    postCount: Int,
    productCount: Int,
    followers: Int,
    following: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(count = postCount, label = "Bài viết")
        StatItem(count = productCount, label = "Sản phẩm")
        StatItem(count = followers, label = "Theo dõi")
        StatItem(count = following, label = "Đang TD")
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = formatCount(count),
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

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}

@Composable
private fun ProfileActions(
    isOwner: Boolean,
    isFollowing: Boolean,
    onFollowClicked: () -> Unit,
    onMessageClicked: () -> Unit,
    onSettingClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isOwner) {
            FilledTonalButton(
                onClick = onSettingClicked,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) { Text("Chỉnh sửa trang cá nhân", fontWeight = FontWeight.SemiBold) }
        } else {
            if (isFollowing) {
                OutlinedButton(
                    onClick = onFollowClicked,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) { Text("Đang theo dõi", fontWeight = FontWeight.SemiBold) }
            } else {
                Button(
                    onClick = onFollowClicked,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) { Text("Theo dõi", fontWeight = FontWeight.SemiBold) }
            }
            OutlinedButton(
                onClick = onMessageClicked,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) { Text("Nhắn tin", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun ProfileTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Bài viết", "Sản phẩm")
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) selectedColor else unselectedColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(if (isSelected) selectedColor else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 1.dp
        )
    }
}
