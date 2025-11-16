package com.example.uth_socials.ui.component.common

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*

@Composable
fun ConfirmDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false,
    title: String = "Xác nhận",
    message: String = "Bạn có chắc chắn muốn thực hiện hành động này?",
    confirmButtonText: String = "Xác nhận",
    dismissButtonText: String = "Hủy",
    confirmButtonColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.error,
    isCurrentUserAdmin: Boolean = false
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            // Nếu có isCurrentUserAdmin và message mặc định, hiển thị message đặc biệt
            if (isCurrentUserAdmin && message == "Bạn có chắc chắn muốn thực hiện hành động này?") {
                Text("Bạn đang thực hiện hành động này với quyền Admin? Hành động này không thể hoàn tác.")
            } else {
                Text(message)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmButtonColor
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(dismissButtonText)
            }
        }
    )
}



