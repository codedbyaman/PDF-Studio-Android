package com.example.androidpdfstudio.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidpdfstudio.ui.theme.SpringBouncy
import com.example.androidpdfstudio.ui.theme.StudioAccent
import com.example.androidpdfstudio.utils.PDFPageRenderer

@Composable
fun PageThumbnailView(
    renderer: PDFPageRenderer,
    pageIndex: Int,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(renderer, pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(renderer, pageIndex) {
        bitmap = renderer.renderPage(pageIndex, scale = 1.2f)
    }

    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) StudioAccent else Color.Gray.copy(alpha = 0.18f),
        animationSpec = tween(200),
        label         = "thumbBorder",
    )
    val borderWidth by animateFloatAsState(
        targetValue   = if (isSelected) 2.5f else 0.5f,
        animationSpec = SpringBouncy,
        label         = "thumbBorderW",
    )
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 0.95f else 1f,
        animationSpec = SpringBouncy,
        label         = "thumbScale",
    )

    Box(
        modifier = modifier
            .width(92.dp)
            .height(122.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(borderWidth.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle),
    ) {
        if (bitmap == null) {
            ThumbnailShimmer()
        }

        bitmap?.let { bmp ->
            Image(
                bitmap             = bmp.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        }

        // Page number badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text(
                "${pageIndex + 1}",
                fontSize   = 9.sp,
                fontWeight = FontWeight.Medium,
                color      = Color.White,
            )
        }

        // Checkmark badge when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(StudioAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint     = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ThumbnailShimmer() {
    val transition = rememberInfiniteTransition(label = "thumbShimmer")
    val shimmerX by transition.animateFloat(
        initialValue  = -1f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
        ),
        label = "tShimX",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFEAEAEA), Color(0xFFF5F5F5), Color(0xFFEAEAEA)),
                    start = Offset(shimmerX * 400f - 200f, 0f),
                    end   = Offset(shimmerX * 400f, 0f),
                )
            )
    )
}
