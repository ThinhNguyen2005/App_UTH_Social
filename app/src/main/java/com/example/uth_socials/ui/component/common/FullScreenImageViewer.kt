package com.example.uth_socials.ui.component.common

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (imageUrls.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, imageUrls.size - 1),
        pageCount = { imageUrls.size }
    )
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Animation states with smooth transitions
    var isInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isInitialized = true
    }
    
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isInitialized) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundAlpha"
    )
    val imageScale by animateFloatAsState(
        targetValue = if (isInitialized) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "imageScale"
    )
    val imageAlpha by animateFloatAsState(
        targetValue = if (isInitialized) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "imageAlpha"
    )
    
    // Close button animation with delay for better UX
    val closeButtonAlpha by animateFloatAsState(
        targetValue = if (isInitialized) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = 100 // Slight delay for smoother appearance
        ),
        label = "closeButtonAlpha"
    )

    // Zoom states for each image
    val zoomStates = remember { mutableStateMapOf<Int, ZoomState>() }

    // Background with fade animation
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Dismiss on background tap
                        onDismiss()
                    }
                )
            }
    ) {
        // Enhanced Close button with better UI/UX
        EnhancedCloseButton(
            onDismiss = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .alpha(closeButtonAlpha)
        )

        // Image pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = imageScale
                    scaleY = imageScale
                    alpha = imageAlpha
                },
            pageSpacing = 16.dp,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) { page ->
            val zoomState = zoomStates.getOrPut(page) {
                ZoomState()
            }

            ZoomableImageFullScreen(
                imageUrl = imageUrls[page],
                zoomState = zoomState,
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Page indicator (only show if multiple images)
        if (imageUrls.size > 1) {
            PageIndicator(
                pageCount = imageUrls.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .alpha(imageAlpha)
            )
        }
    }
}

@Composable
private fun ZoomableImageFullScreen(
    imageUrl: String,
    zoomState: ZoomState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Animated zoom values for smooth transitions
    val animatedScale by animateFloatAsState(
        targetValue = zoomState.scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "zoomScale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = zoomState.offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = zoomState.offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offsetY"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        coroutineScope.launch {
                            if (zoomState.scale > 1f) {
                                // Reset zoom
                                zoomState.scale = 1f
                                zoomState.offsetX = 0f
                                zoomState.offsetY = 0f
                            } else {
                                // Zoom in
                                zoomState.scale = 2.5f
                            }
                        }
                    },
                    onTap = {
                        // Single tap to dismiss if not zoomed
                        if (zoomState.scale <= 1f) {
                            onDismiss()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (zoomState.scale * zoom).coerceIn(1f, 4f)
                    zoomState.scale = newScale

                    if (zoomState.scale > 1f) {
                        // Allow panning when zoomed
                        val maxOffsetX = (size.width * (zoomState.scale - 1)) / 2
                        val maxOffsetY = (size.height * (zoomState.scale - 1)) / 2

                        val newOffsetX = (zoomState.offsetX + pan.x * zoomState.scale)
                        val newOffsetY = (zoomState.offsetY + pan.y * zoomState.scale)

                        zoomState.offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                        zoomState.offsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                    } else {
                        zoomState.offsetX = 0f
                        zoomState.offsetY = 0f
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Full screen image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffsetX,
                    translationY = animatedOffsetY
                )
        )
    }
}

@Composable
private fun EnhancedCloseButton(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale animation when pressed
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )
    
    // Background alpha animation
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 0.6f,
        animationSpec = tween(durationMillis = 150),
        label = "backgroundAlpha"
    )
    
    // Icon scale animation for better feedback
    val iconScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconScale"
    )

    Surface(
        modifier = modifier
            .size(48.dp)
            .scale(buttonScale)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clickable(
                onClick = onDismiss,
                interactionSource = interactionSource,
                indication = null // Disable default ripple, we handle it with scale
            ),
        shape = CircleShape,
        color = Color.Black.copy(alpha = backgroundAlpha)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale)
            )
        }
    }
}

private class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
}

