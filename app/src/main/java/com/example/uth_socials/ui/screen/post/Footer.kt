package com.example.uth_socials.ui.screen.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.uth_socials.ui.viewmodel.PostViewModel

@Composable
fun Footer(
    viewModel: PostViewModel,
    selectedTabIndex: Int,
    onTabClick: (Int) -> Unit
) {
    // Danh sách ảnh được chọn
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Bộ chọn nhiều ảnh
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            selectedImageUris = uris
            viewModel.imageUris = uris // 🔥 Lưu vào ViewModel để upload
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        // ✅ Hiển thị các ảnh được chọn
        if (selectedImageUris.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(selectedImageUris) { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Ảnh đã chọn",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, Color.Gray, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Nút chọn nhiều ảnh
        TextButton(
            onClick = {
                multiplePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "Chọn ảnh",
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Chọn ảnh", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Các tab
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                120.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            TabButton(
                text = "Bài viết",
                isSelected = selectedTabIndex == 0,
                onClick = { onTabClick(0) }
            )

            TabButton(
                text = "Sản phẩm",
                isSelected = selectedTabIndex == 1,
                onClick = { onTabClick(1) }
            )
        }
    }
}
