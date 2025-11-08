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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.uth_socials.data.repository.UserRepository
import com.example.uth_socials.data.user.User
import com.example.uth_socials.ui.component.button.PrimaryButton
import com.example.uth_socials.ui.viewmodel.PostViewModel
import com.example.uth_socials.ui.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth


@Composable
fun Header(postViewModel: PostViewModel, productViewModel: ProductViewModel, navController : NavController, selectedTabIndex : Int) {
    val context = LocalContext.current  // lấy context để hiển thị Toast

    val userRepository: UserRepository = UserRepository()

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var user by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        user = userRepository.getUser(userId)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
                model = user?.avatarUrl,
                contentScale = ContentScale.Crop
            )
            Text(user?.username ?: "", fontWeight = FontWeight.Medium)
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
                    if(postViewModel.content.isEmpty()) {
                        Toast.makeText(context, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show()
                        return@PrimaryButton
                    }
                    postViewModel.postArticle(userId)
                    navController.navigate("home")
                }else{
                    if(productViewModel.name.isEmpty() || productViewModel.type.isEmpty()){
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        return@PrimaryButton
                    }
                    productViewModel.postArticle(userId)
                    navController.navigate("home")
                }

                Toast.makeText(context, "Đăng thành công", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
