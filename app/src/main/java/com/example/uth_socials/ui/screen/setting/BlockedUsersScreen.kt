package com.example.uth_socials.ui.screen.setting

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.uth_socials.ui.component.common.ConfirmDialog
import com.example.uth_socials.ui.viewmodel.BlockedUsersViewModel
import com.example.uth_socials.ui.viewmodel.DialogType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onBackClicked: () -> Unit,
    onUserUnblocked: () -> Unit = {}
) {
    Log.d("BlockedUsersScreen", "BlockedUsersScreen composable called")
    val viewModel: BlockedUsersViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
            onUserUnblocked()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Người dùng đã chặn",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.blockedUsers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Chưa có người dùng nào bị chặn",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.blockedUsers, key = { it.userId }) { blockedUser ->
                        BlockedUserItem(
                            user = blockedUser,
                            onUnblockClicked = {
                                viewModel.onUnblockClicked(blockedUser.userId, blockedUser.username)
                            }
                        )
                        Divider()
                    }
                }
            }
        }
        
        when (val dialog = uiState.dialogType) {
            is DialogType.None -> {  }
            is DialogType.UnblockUser -> {
                ConfirmDialog(
                    isVisible = true,
                    onDismiss = { viewModel.onDismissDialog() },
                    onConfirm = { viewModel.onConfirmDialog() },
                    isLoading = uiState.isProcessing,
                    title = "Gỡ chặn người dùng",
                    message = "Bạn có chắc chắn muốn gỡ chặn ${dialog.username}? Bạn sẽ có thể xem bài viết và tương tác với họ.",
                    confirmButtonText = "Gỡ chặn",
                    confirmButtonColor = MaterialTheme.colorScheme.primary
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun BlockedUserItem(
    user: com.example.uth_socials.ui.viewmodel.BlockedUserItem,
    onUnblockClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl ?: "https://firebasestorage.googleapis.com/v0/b/uthsocial-a2f90.firebasestorage.app/o/avatarDef.jpg?alt=media&token=b6363023-1c54-4370-a2f1-09127c4673da",
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        TextButton(onClick = onUnblockClicked) {
            Text("Gỡ chặn")
        }
    }
}

