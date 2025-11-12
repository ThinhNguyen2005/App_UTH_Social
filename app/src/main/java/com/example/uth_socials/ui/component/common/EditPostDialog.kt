package com.example.uth_socials.ui.component.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * EditPostDialog - Dialog để chỉnh sửa nội dung bài viết
 * 
 * @param isVisible Hiển thị dialog hay không
 * @param currentContent Nội dung hiện tại của bài viết
 * @param isLoading Đang lưu hay không
 * @param errorMessage Thông báo lỗi (nếu có)
 * @param onDismiss Callback khi đóng dialog
 * @param onSave Callback khi lưu với nội dung mới
 */
@Composable
fun EditPostDialog(
    isVisible: Boolean,
    currentContent: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!isVisible) return
    
    var editedContent by remember(isVisible) { mutableStateOf(currentContent) }
    val maxLength = 2000
    val isContentValid = editedContent.trim().isNotEmpty() && editedContent.length <= maxLength
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Chỉnh sửa bài viết",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { 
                        if (it.length <= maxLength) {
                            editedContent = it
                        }
                    },
                    label = { Text("Nội dung bài viết") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    supportingText = {
                        Text(
                            text = "${editedContent.length}/$maxLength",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (editedContent.length > maxLength) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    isError = editedContent.length > maxLength || editedContent.trim().isEmpty()
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editedContent.trim()) },
                enabled = isContentValid && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Hủy")
            }
        }
    )
}

