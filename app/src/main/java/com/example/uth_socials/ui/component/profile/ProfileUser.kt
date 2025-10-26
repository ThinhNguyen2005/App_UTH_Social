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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHeader(
    username: String,
    avatarUrl: String,
    bio: String,
    followers: Int,
    following: Int,
    postCount: Int = 0,
    isOwner: Boolean,
    isFollowing: Boolean,
    onFollowClicked: () -> Unit,
    // Thêm các lambda cho các hành động khác nếu cần
    onBackClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onMessageClicked: () -> Unit = {},
    onEditProfileClicked: () -> Unit = {},
    scrollOffsetPx: Float = 0f
) {
    // State cho menu dropdown
    var showMenu by remember { mutableStateOf(false) }
    // Tính toán các giá trị cho hiệu ứng parallax dựa trên scrollOffsetPx
    val headerHeight = 260.dp
    val backgroundHeight = 180.dp
    
    // Tính toán tỉ lệ cuộn (0 -> 1)
    val scrollRatio = min(1f, abs(scrollOffsetPx) / 500f)
    
    // Hiệu ứng parallax cho background: di chuyển lên khi cuộn
    val backgroundOffsetY = scrollOffsetPx * 0.3f // Tốc độ parallax cho background
    
    // Hiệu ứng scale và alpha cho card
    val cardScale = 1f - (scrollRatio * 0.05f) // Thu nhỏ nhẹ khi cuộn
    
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight) // Chiều cao tổng thể của phần header
        ) {
            // Phần background màu xanh với hiệu ứng parallax
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backgroundHeight)
                    .graphicsLayer {
                        // Áp dụng hiệu ứng parallax cho background
                        translationY = backgroundOffsetY
                        // Giữ background luôn đủ lớn khi cuộn
                        scaleX = 1f + (scrollRatio * 0.05f)
                        scaleY = 1f + (scrollRatio * 0.05f)
                    }
                    .background(
                        color = Color(0xFF18A08D), // Màu xanh giống trong hình
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
            )
            
            // Nút Back và More Options ở trên cùng
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Nút Back
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                // Nút More Options với dropdown menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }
                    
                    // Dropdown menu cho các tùy chọn
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
                                    // TODO: Implement report functionality
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Cài đặt") },
                                onClick = {
                                    showMenu = false
                                    // TODO: Implement settings
                                }
                            )
                        }
                    }
                }
            }

            // Card thông tin chính với hiệu ứng scale
            ProfileInfoCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                    },
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
                onEditProfileClicked = onEditProfileClicked,
                scrollOffsetPx = scrollOffsetPx
            )
        }

        // Tab "Bài viết" và "Sản phẩm"
        ProfileTabs()
    }
}

@Composable
private fun ProfileInfoCard(
    modifier: Modifier = Modifier,
    username: String,
    avatarUrl: String,
    bio: String,
    followers: Int,
    following: Int,
    postCount: Int = 0,
    isOwner: Boolean,
    isFollowing: Boolean,
    onFollowClicked: () -> Unit,
    onMessageClicked: () -> Unit,
    onEditProfileClicked: () -> Unit,
    scrollOffsetPx: Float = 0f
) {
    Box(modifier = modifier) {
        // Card trắng chứa thông tin
        Card(
            modifier = Modifier.padding(top = 40.dp), // Để chừa chỗ cho avatar
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp), // Padding trên lớn để avatar không che chữ
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Số liệu thống kê
                ProfileStats(
                    postCount = postCount,
                    followers = followers,
                    following = following
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bio
                if (bio.isNotEmpty()) {
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Các nút hành động
                ProfileActions(
                    isOwner = isOwner,
                    isFollowing = isFollowing,
                    onFollowClicked = onFollowClicked,
                    onMessageClicked = onMessageClicked,
                    onEditProfileClicked = onEditProfileClicked
                )
            }
        }

        // Tính toán hiệu ứng cho avatar
        val scrollRatio = min(1f, abs(scrollOffsetPx) / 500f)
        val avatarScale = 1f - (scrollRatio * 0.15f) // Thu nhỏ avatar khi cuộn
        val avatarOffsetY = scrollOffsetPx * 0.2f // Di chuyển avatar theo tốc độ chậm hơn
        
        // Avatar với hiệu ứng parallax và scale
        AsyncImage(
            model = avatarUrl,
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    // Áp dụng hiệu ứng scale và parallax cho avatar
                    scaleX = avatarScale
                    scaleY = avatarScale
                    translationY = avatarOffsetY
                    // Làm mờ dần khi cuộn
                    alpha = 1f - (scrollRatio * 0.3f)
                }
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
            
            // Nút nhắn tin
            OutlinedButton(
                onClick = onMessageClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Text("Nhắn tin")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Nút theo dõi
            if (isFollowing) {
                // Nếu đã theo dõi, hiển thị nút outlined
                OutlinedButton(
                    onClick = onFollowClicked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Text("Đang theo dõi")
                }
            } else {
                // Nếu chưa theo dõi, hiển thị nút filled
                Button(
                    onClick = onFollowClicked,
                    modifier = Modifier.fillMaxWidth(),
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

