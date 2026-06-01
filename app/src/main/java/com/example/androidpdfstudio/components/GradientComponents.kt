package com.example.androidpdfstudio.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidpdfstudio.models.PDFTool
import com.example.androidpdfstudio.ui.theme.SpringBouncy
import com.example.androidpdfstudio.ui.theme.SpringPress
import com.example.androidpdfstudio.ui.theme.GradientMeshBackground
import com.example.androidpdfstudio.ui.theme.HeroShape

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    icon: @Composable BoxScope.() -> Unit,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(HeroShape)
    ) {
        GradientMeshBackground(colors = colors)

        // Bottom edge depth overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.18f),
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
                content = icon,
            )
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.82f))
            extraContent?.invoke(this)
        }
    }
}

// ── Gradient Icon Badge ────────────────────────────────────────────────────────

@Composable
fun GradientIconBadge(
    colors: List<Color>,
    iconContent: @Composable () -> Unit,
    size: Int = 110,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(
                elevation  = 28.dp,
                shape      = CircleShape,
                spotColor  = colors.first().copy(alpha = 0.55f),
                ambientColor = colors.first().copy(alpha = 0.25f),
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        // Inner ring
        Box(
            modifier = Modifier
                .size((size - 8).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        iconContent()
    }
}

// ── Tool Card — with spring press scale animation ─────────────────────────────

@Composable
fun ToolCard(
    tool: PDFTool,
    onClick: () -> Unit,
    iconContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue    = if (isPressed) 0.94f else 1f,
        animationSpec  = SpringPress,
        label          = "cardScale",
    )

    Box(
        modifier = modifier
            .height(165.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation    = if (isPressed) 6.dp else 14.dp,
                shape        = RoundedCornerShape(22.dp),
                spotColor    = tool.gradient.first().copy(alpha = 0.45f),
                ambientColor = tool.gradient.first().copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(tool.gradient))
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 50.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.13f))
        )
        Box(
            modifier = Modifier
                .size(75.dp)
                .offset(x = 88.dp, y = 18.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        // Clickable surface (no ripple — scale handles feedback)
        Surface(
            onClick           = onClick,
            interactionSource = interactionSource,
            color             = Color.Transparent,
            modifier          = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                    content          = iconContent,
                )

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        tool.title,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                    Text(
                        tool.subtitle,
                        fontSize  = 11.sp,
                        color     = Color.White.copy(alpha = 0.82f),
                        lineHeight = 14.sp,
                        maxLines  = 2,
                    )
                }
            }
        }

        // Arrow badge — bottom-right (above clickable surface)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "→",
                color      = Color.White,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Stat Pill ─────────────────────────────────────────────────────────────────

@Composable
fun StatPill(
    label: String,
    icon: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        icon()
        Text(
            label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            color      = Color.White.copy(alpha = 0.9f),
        )
    }
}

// ── Gradient Button — with press animation ────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = SpringPress,
        label         = "btnScale",
    )

    Surface(
        onClick           = onClick,
        interactionSource = interactionSource,
        enabled           = enabled,
        shape             = RoundedCornerShape(14.dp),
        color             = Color.Transparent,
        modifier          = modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation    = if (enabled) 10.dp else 0.dp,
                shape        = RoundedCornerShape(14.dp),
                spotColor    = colors.first().copy(alpha = 0.4f),
                ambientColor = colors.first().copy(alpha = 0.15f),
            )
            .background(
                if (enabled)
                    Brush.horizontalGradient(colors)
                else
                    Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.Gray.copy(alpha = 0.4f)))
            ),
    ) {
        Box(
            modifier         = Modifier.padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
            )
        }
    }
}

// ── Outline Button ────────────────────────────────────────────────────────────

@Composable
fun OutlineButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        color    = color.copy(alpha = 0.08f),
        border   = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Box(
            modifier         = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (enabled) color else Color.Gray,
            )
        }
    }
}
