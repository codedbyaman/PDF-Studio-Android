package com.example.androidpdfstudio.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Animation Specs ───────────────────────────────────────────────────────────

val SpringBouncy = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessMedium,
)

val SpringSnappy = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness    = Spring.StiffnessMediumLow,
)

val SpringPress = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness    = Spring.StiffnessHigh,
)

// ── Gradient Brush helpers ────────────────────────────────────────────────────

fun List<Color>.horizontalBrush() = Brush.horizontalGradient(this)
fun List<Color>.verticalBrush()   = Brush.verticalGradient(this)
fun List<Color>.diagonalBrush()   = Brush.linearGradient(this)

// ── Gradient Mesh Background ──────────────────────────────────────────────────
// Layered: gradient base + two soft orbs + vignette

@Composable
fun GradientMeshBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    // Outer Box uses only the caller's modifier — no fillMaxSize() here,
    // so matchParentSize() works correctly inside a scrollable container.
    Box(modifier = modifier) {
        // Base gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors))
        )
        // Soft orb 1 — top-right
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 90.dp, y = (-60).dp)
                .blur(60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        )
        // Soft orb 2 — left-center
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-70).dp, y = 80.dp)
                .blur(60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )
        // Bottom vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1.0f  to Color.Black.copy(alpha = 0.18f),
                    )
                )
        )
    }
}

// ── Hero shape: flat top, rounded bottom 28dp ─────────────────────────────────
val HeroShape = RoundedCornerShape(
    topStart    = 0.dp, topEnd     = 0.dp,
    bottomStart = 28.dp, bottomEnd = 28.dp,
)
