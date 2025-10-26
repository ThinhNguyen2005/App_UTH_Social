package com.example.uth_socials.ui.screen


import PageIndicator
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uth_socials.R
import com.example.uth_socials.ui.viewmodel.HelloUserModel
import kotlinx.coroutines.delay
import com.example.uth_socials.ui.component.button.PrimaryButton
import com.example.uth_socials.ui.component.common.ClickablePrivacyPolicyText
import androidx.compose.ui.res.stringResource



val UthTeal = Color(0xFF008080)
val UthRed = Color(0xFFC70039)

val InactiveDotColor = Color(0xFFB0B0B0)

private val onboardingImages = listOf(
    R.drawable.truong,
    R.drawable.truong1,
)
@Composable
fun OnboardingScreen(
    helloUserModel: HelloUserModel = viewModel()
) {
    val uiState by helloUserModel.uiState.collectAsState()

    OnboardingContent(
        pagerIndex = uiState.pagerIndex,
        onPagerIndexChange = { newIndex -> helloUserModel.updatePagerIndex(newIndex) },
        onStartClicked = { helloUserModel.onStartClicked() }
    )
}


@Composable
fun OnboardingContent(
    pagerIndex: Int,
    onPagerIndexChange: (Int) -> Unit,
    onStartClicked: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLandscape) {
            OnboardingLandscapeLayout(pagerIndex, onPagerIndexChange, onStartClicked)
        } else {
            OnboardingPortraitLayout(pagerIndex, onPagerIndexChange, onStartClicked)
        }
    }
}
@Composable
fun OnboardingPortraitLayout(
    pagerIndex: Int,
    onPagerIndexChange: (Int) -> Unit,
    onStartClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Logo()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ImageSlideshowWithPager(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp)),
                images = onboardingImages,
                currentIndex = pagerIndex,
                onIndexChange = onPagerIndexChange
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.onboarding_message),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            PageIndicator(pageCount = onboardingImages.size, currentPage = pagerIndex)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ClickablePrivacyPolicyText()
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = stringResource(R.string.start_button),
                onClick = onStartClicked
            )
        }
    }
}

@Composable
fun OnboardingLandscapeLayout(
    pagerIndex: Int,
    onPagerIndexChange: (Int) -> Unit,
    onStartClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột bên trái: Chứa ảnh
        Box(
            modifier = Modifier.weight(1f), // Chiếm 1 nửa không gian
            contentAlignment = Alignment.Center
        ) {
            ImageSlideshowWithPager(
                modifier = Modifier
                    .fillMaxWidth() // Lấp đầy không gian được cấp
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp)),
                images = onboardingImages,
                currentIndex = pagerIndex,
                onIndexChange = onPagerIndexChange
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Cột bên phải: Chứa nội dung còn lại và có thể cuộn
        Column(
            modifier = Modifier
                .weight(1f) // Chiếm 1 nửa không gian còn lại
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Logo()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Xây dựng mạng lưới của bạn mọi lúc mọi nơi",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            PageIndicator(pageCount = onboardingImages.size, currentPage = pagerIndex)
            Spacer(modifier = Modifier.height(24.dp))
            ClickablePrivacyPolicyText()
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(text = "Bắt đầu", onClick = onStartClicked)
        }
    }
}
// --- CÁC COMPOSABLE PHỤ TRỢ ---

@Composable
fun Logo() {
    Text(
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = UthTeal, fontSize = 36.sp, fontWeight = FontWeight.Bold)) {
                append("UTH")
            }
            withStyle(style = SpanStyle(color = UthRed, fontSize = 36.sp, fontWeight = FontWeight.Bold)) {
                append(" Social")
            }
        }
    )
}


@Composable
fun ImageSlideshowWithPager(
    modifier: Modifier = Modifier,
    images: List<Int>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    slideDuration: Long = 3000L,
    loopCount: Int = 3 // <-- Giới hạn số vòng lặp ở đây
) {
    val imageCount = images.size
    if (imageCount == 0) return // Tránh lỗi nếu không có ảnh

    val totalPageCount = imageCount * loopCount
    val initialPage = imageCount * (loopCount / 2)

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPageCount }
    )

    // Tự động trượt
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { isScrolling ->
            if (!isScrolling) {
                delay(slideDuration)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
            }
        }
    }


    // Lắng nghe khi người dùng vuốt và cập nhật ViewModel
    LaunchedEffect(pagerState.currentPage) {
        val newIndex = pagerState.currentPage % imageCount
        onIndexChange(newIndex)
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        // Sử dụng modulo để lấy đúng index ảnh từ danh sách
        val imageIndex = page % imageCount
        Image(
            painter = painterResource(id = images[imageIndex]),
            contentDescription = "Onboarding Image ${imageIndex + 1}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}


// --- PREVIEW ---

@Preview(name = "Portrait Mode", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun OnboardingPortraitPreview() {
    OnboardingPortraitLayout(0, {}, {})
}

//@Preview(name = "Landscape Mode", showBackground = true, widthDp = 780, heightDp = 360)
//@Composable
//fun OnboardingLandscapePreview() {
//    OnboardingLandscapeLayout(0, {}, {})
//}