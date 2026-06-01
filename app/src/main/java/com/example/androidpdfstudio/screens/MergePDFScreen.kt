package com.example.androidpdfstudio.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.androidpdfstudio.components.GradientButton
import com.example.androidpdfstudio.components.GradientIconBadge
import com.example.androidpdfstudio.components.OutlineButton
import com.example.androidpdfstudio.ui.theme.NavyDark
import com.example.androidpdfstudio.ui.theme.SectionPalette
import com.example.androidpdfstudio.ui.theme.StudioAccent
import com.example.androidpdfstudio.ui.theme.StudioGreen
import com.example.androidpdfstudio.viewmodels.MergePDFViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePDFScreen(
    navController: NavController,
    vm: MergePDFViewModel = viewModel(),
) {
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        vm.addItems(context, uris)
    }

    val resultFile = vm.resultFile
    LaunchedEffect(resultFile) {
        if (resultFile != null) {
            val fileUri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", resultFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Merged PDF"))
            vm.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDF", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {

                SectionHeaderBar(
                    title    = "Merge PDF",
                    subtitle = "Combine multiple documents into one",
                    colors   = SectionPalette.merge,
                    icon     = Icons.AutoMirrored.Filled.CallMerge,
                )

                // Content switches between empty state and list with smooth animation
                AnimatedContent(
                    targetState = vm.items.isEmpty(),
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    modifier = Modifier.weight(1f),
                    label    = "mergeContent",
                ) { isEmpty ->
                    if (isEmpty) {
                        EmptyState(
                            colors      = SectionPalette.merge,
                            icon        = Icons.AutoMirrored.Filled.CallMerge,
                            title       = "No PDFs Added",
                            subtitle    = "Add two or more PDFs to merge them together.",
                            actionLabel = "Add PDF Files",
                            onAction    = { filePicker.launch(arrayOf("application/pdf")) },
                        )
                    } else {
                        LazyColumn(
                            modifier      = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(
                                vm.items,
                                key = { _, item -> item.id },
                            ) { index, item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            vm.removeItem(item.id)
                                            true
                                        } else false
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.animateItem(fadeInSpec = tween(300)),
                                    enableDismissFromEndToStart   = true,
                                    enableDismissFromStartToEnd   = false,
                                    backgroundContent = {
                                        val color = MaterialTheme.colorScheme.errorContainer
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(color)
                                                .padding(end = 20.dp),
                                            contentAlignment = Alignment.CenterEnd,
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                null,
                                                tint     = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    },
                                ) {
                                    PDFListCard(
                                        displayName = item.displayName,
                                        pageCount   = item.pageCount,
                                        index       = index,
                                        isFirst     = index == 0,
                                        isLast      = index == vm.items.lastIndex,
                                        onMoveUp    = { vm.moveUp(index) },
                                        onMoveDown  = { vm.moveDown(index) },
                                        onDelete    = { vm.removeItem(item.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom action bar (only when items exist)
                AnimatedVisibility(
                    visible = vm.items.isNotEmpty(),
                    enter   = slideInVertically { it } + fadeIn(),
                ) {
                    Surface(
                        shadowElevation = 16.dp,
                        color           = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (vm.items.size >= 2) {
                                GradientButton(
                                    text   = "Merge ${vm.items.size} PDFs",
                                    colors = SectionPalette.merge,
                                    onClick = { vm.merge(context) },
                                )
                            }
                            OutlineButton(
                                text   = "Add More PDFs",
                                color  = StudioAccent,
                                onClick = { filePicker.launch(arrayOf("application/pdf")) },
                            )
                        }
                    }
                }
            }

            if (vm.isProcessing) ProcessingOverlay("Merging PDFs…")
        }
    }
}

// ── PDF list card ─────────────────────────────────────────────────────────────

@Composable
private fun PDFListCard(
    displayName: String,
    pageCount: Int,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Gradient PDF icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(SectionPalette.merge)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    null,
                    tint     = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                )
                Text(
                    if (pageCount > 0) "$pageCount pages" else "PDF Document",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Reorder buttons
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallIconButton(
                    icon    = Icons.Default.KeyboardArrowUp,
                    tint    = if (!isFirst) StudioAccent else Color.Gray.copy(0.4f),
                    enabled = !isFirst,
                    onClick = onMoveUp,
                )
                SmallIconButton(
                    icon    = Icons.Default.KeyboardArrowDown,
                    tint    = if (!isLast) StudioAccent else Color.Gray.copy(0.4f),
                    enabled = !isLast,
                    onClick = onMoveDown,
                )
            }
        }
    }
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.size(30.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// ── Shared composables (used by Split & Photos screens too) ───────────────────

@Composable
internal fun SectionHeaderBar(
    title: String,
    subtitle: String,
    colors: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Brush.linearGradient(colors))
    ) {
        // Decorative orb
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 220.dp, y = (-30).dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.82f))
            }
        }
    }
}

@Composable
internal fun ProcessingOverlay(message: String = "Processing…") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyDark),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(
                    color        = SectionPalette.edit.first(),
                    strokeWidth  = 3.dp,
                )
                Text(message, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
internal fun EmptyState(
    colors: List<Color>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GradientIconBadge(
            colors = colors,
            iconContent = {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(44.dp))
            },
        )
        Spacer(Modifier.height(28.dp))
        Text(
            title,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            fontSize = 14.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(36.dp))
        GradientButton(text = actionLabel, colors = colors, onClick = onAction)
    }
}
