package com.example.uth_socials.ui.screen.post

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.R
import com.example.uth_socials.ui.component.button.PrimaryButton
import com.example.uth_socials.ui.component.navigation.HomeBottomNavigation
import com.example.uth_socials.ui.component.logo.HomeTopAppBarPrimary
import com.example.uth_socials.ui.viewmodel.PostViewModel
import com.example.uth_socials.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(postViewModel: PostViewModel, productViewModel: ProductViewModel) {
    var selectedTabIndex by remember { mutableStateOf(0) }

   // var text by remember { mutableStateOf(viewModel.content) }

    Scaffold(
        topBar = { HomeTopAppBarPrimary() },
        bottomBar = { HomeBottomNavigation() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            Header(postViewModel,productViewModel,selectedTabIndex)

            when (selectedTabIndex) {
                0 -> ArticlePost(postViewModel = postViewModel)
                1 -> ProductPost(productViewModel = productViewModel)
            }

            Footer(postViewModel, selectedTabIndex = selectedTabIndex) {
                selectedTabIndex = it
            }

        }
    }
}