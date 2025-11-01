package com.example.uth_socials.ui.screen.post

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.uth_socials.R
import com.example.uth_socials.ui.component.button.PrimaryButton
import com.example.uth_socials.ui.viewmodel.PostViewModel
import com.example.uth_socials.ui.viewmodel.ProductViewModel

@Composable
fun Header(postViewModel: PostViewModel, productViewModel: ProductViewModel, selectedTabIndex : Int) {
    val context = LocalContext.current  // lấy context để hiển thị Toast

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text("SKT_DucT1", fontWeight = FontWeight.Medium)
        }

        PrimaryButton(
            buttonColor = ButtonDefaults.buttonColors(containerColor = Color(0xFF007E8F)),
            text = "Đăng",
            textColor = Color.White,
            modifier = Modifier
                .width(100.dp)
                .height(40.dp),
            onClick = {
                if (selectedTabIndex == 0) {
                    postViewModel.postArticle("Nghia")
                }else{
                    productViewModel.postArticle("Nghia")
                }

                Toast.makeText(context, "Đăng thành công", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
