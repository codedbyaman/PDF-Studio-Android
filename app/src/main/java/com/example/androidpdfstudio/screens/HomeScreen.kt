package com.example.androidpdfstudio.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.androidpdfstudio.components.StatPill
import com.example.androidpdfstudio.components.ToolCard
import com.example.androidpdfstudio.models.PDFTool
import com.example.androidpdfstudio.models.PDFTools
import com.example.androidpdfstudio.navigation.Screen
import com.example.androidpdfstudio.ui.theme.*
import kotlinx.coroutines.delay

// HomeScreen uses a Box layout so the hero gradient bleeds behind the status bar

@Composable
fun HomeScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Scrollable body
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            HeroSection()
            ToolGridSection(
                onToolClick = { tool ->
                    when (tool.title) {
                        "Edit PDF"      -> navController.navigate(Screen.EditPDF.route)
                        "Merge PDF"     -> navController.navigate(Screen.MergePDF.route)
                        "Split PDF"     -> navController.navigate(Screen.SplitPDF.route)
                        "Photos to PDF" -> navController.navigate(Screen.PhotosToPDF.route)
                        "Compress PDF"  -> navController.navigate(Screen.CompressPDF.route)
                        "PDF to Images" -> navController.navigate(Screen.PDFToImages.route)
                    }
                },
            )
        }

        // Floating transparent nav bar overlay — matches iOS transparent nav bar
        FloatingNavBar()
    }
}

// ── Floating nav bar ──────────────────────────────────────────────────────────

@Composable
private fun FloatingNavBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Centered logo + title (matches iOS principal toolbar item)
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Colorful "P" badge
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF68C040), Color(0xFFDDAC20),
                                Color(0xFF3A58C8), Color(0xFFD04444),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "P",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }
            Text(
                "PDF Studio",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
            )
        }
    }
}

// ── Hero Section — gradient bleeds from status bar ────────────────────────────

@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HeroShape)
    ) {
        // Column drives the Box height — must be placed before the gradient
        // so matchParentSize() below can resolve to the Column's intrinsic height.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()         // clear status bar
                .padding(top = 72.dp)        // clear floating nav bar
                .padding(horizontal = 20.dp)
                .padding(bottom = 52.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Branding row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF68C040), Color(0xFFDDAC20),
                                    Color(0xFF3A58C8), Color(0xFFD04444),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("P", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "PDF Studio",
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                    Text(
                        "Professional PDF toolkit",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.65f),
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 1.dp)

            // Stat pills row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(
                    label = "${PDFTools.all.size} Tools",
                    icon  = {
                        Icon(
                            Icons.Default.Build, null,
                            tint     = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(11.dp),
                        )
                    },
                )
                StatPill(
                    label = "On-Device",
                    icon  = {
                        Icon(
                            Icons.Default.PhoneAndroid, null,
                            tint     = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(11.dp),
                        )
                    },
                )
                StatPill(
                    label = "Private",
                    icon  = {
                        Icon(
                            Icons.Default.Lock, null,
                            tint     = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(11.dp),
                        )
                    },
                )
            }
        }

        // Gradient rendered behind the column via zIndex(-1f).
        // matchParentSize() sizes it to the Column's intrinsic height.
        GradientMeshBackground(
            colors   = SectionPalette.home,
            modifier = Modifier.matchParentSize().zIndex(-1f),
        )
    }
}

// ── Tool Grid Section ─────────────────────────────────────────────────────────

@Composable
private fun ToolGridSection(onToolClick: (PDFTool) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section title
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Choose a Tool",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
                modifier   = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StudioAccent.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("${PDFTools.all.size} tools", fontSize = 11.sp, color = StudioAccent)
            }
        }

        // 2-column grid with staggered spring animations
        val tools = PDFTools.all
        val visible = remember { Array(tools.size) { mutableStateOf(false) } }

        LaunchedEffect(Unit) {
            tools.indices.forEach { i ->
                delay(i * 80L)
                visible[i].value = true
            }
        }

        tools.chunked(2).forEachIndexed { rowIdx, rowTools ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowTools.forEachIndexed { colIdx, tool ->
                    val idx = rowIdx * 2 + colIdx
                    AnimatedVisibility(
                        visible = visible[idx].value,
                        enter   = fadeIn(tween(300)) +
                                  slideInVertically(
                                      spring(
                                          dampingRatio = Spring.DampingRatioMediumBouncy,
                                          stiffness    = Spring.StiffnessMedium,
                                      )
                                  ) { 60 },
                        modifier = Modifier.weight(1f),
                    ) {
                        ToolCard(
                            tool    = tool,
                            onClick = { onToolClick(tool) },
                            iconContent = {
                                Icon(
                                    imageVector     = toolIcon(tool.title),
                                    contentDescription = null,
                                    tint            = Color.White,
                                    modifier        = Modifier.size(24.dp),
                                )
                            },
                        )
                    }
                }
                if (rowTools.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // Privacy note
        AnimatedVisibility(
            visible = visible.lastOrNull()?.value == true,
            enter   = fadeIn(tween(400, delayMillis = 350)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioGreen.copy(alpha = 0.10f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint     = StudioGreen,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "All processing is done on your device. Your files never leave it.",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Tool → Material Icon mapping ──────────────────────────────────────────────

private fun toolIcon(title: String): ImageVector = when (title) {
    "Edit PDF"      -> Icons.Default.Edit
    "Merge PDF"     -> Icons.AutoMirrored.Filled.CallMerge
    "Split PDF"     -> Icons.Default.ContentCut
    "Photos to PDF" -> Icons.Default.Collections
    "Compress PDF"  -> Icons.Default.Compress
    "PDF to Images" -> Icons.Default.Image
    else            -> Icons.Default.PictureAsPdf
}
