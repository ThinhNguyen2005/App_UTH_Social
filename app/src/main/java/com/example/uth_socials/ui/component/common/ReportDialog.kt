package com.example.uth_socials.ui.component.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onReportReasonChanged: (String) -> Unit,
    onReportDescriptionChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    reportReason: String,
    reportDescription: String,
    isReporting: Boolean,
    reportErrorMessage: String? = null
) {
    if (!isVisible) return

    val reportReasons = listOf(
        "Spam",
        "Nội dung không phù hợp",
        "Quấy rối",
        "Bạo lực",
        "Lừa đảo",
        "Vi phạm quyền tác giả",
        "Khác"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Báo cáo bài viết") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Vui lòng chọn lý do báo cáo:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Dropdown để chọn lý do
                var expanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = reportReason.ifEmpty { "Chọn lý do..." },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        reportReasons.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason) },
                                onClick = {
                                    onReportReasonChanged(reason)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TextField cho mô tả chi tiết
                OutlinedTextField(
                    value = reportDescription,
                    onValueChange = onReportDescriptionChanged,
                    label = { Text("Mô tả chi tiết (tùy chọn)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 4,
                    enabled = !isReporting
                )

                // ✅ Hiển thị error message nếu có
                if (reportErrorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = reportErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = reportReason.isNotEmpty() && !isReporting
            ) {
                if (isReporting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Gửi báo cáo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isReporting) {
                Text("Hủy")
            }
        }
    )
}




