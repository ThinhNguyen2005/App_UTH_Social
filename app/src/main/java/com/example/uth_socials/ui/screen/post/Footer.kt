package com.example.uth_socials.ui.screen.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    // URI của ảnh được chọn
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Bộ chọn ảnh (Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri   // Cập nhật biến cục bộ
            viewModel.imageUri = uri // Lưu vào ViewModel để dùng sau (ví dụ upload)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        //horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ✅ Nếu có ảnh đã chọn → hiển thị ảnh ở trên nút
        selectedImageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Ảnh đã chọn",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Gray, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Nút chọn ảnh
        TextButton(
            onClick = {
                photoPickerLauncher.launch(
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
