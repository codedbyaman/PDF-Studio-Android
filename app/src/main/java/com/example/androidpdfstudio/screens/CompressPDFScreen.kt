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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.androidpdfstudio.components.PageThumbnailView
import com.example.androidpdfstudio.ui.theme.NavyDark
import com.example.androidpdfstudio.ui.theme.SectionPalette
import com.example.androidpdfstudio.viewmodels.CompressPDFViewModel
import com.example.androidpdfstudio.viewmodels.CompressQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressPDFScreen(
    navController: NavController,
    vm: CompressPDFViewModel = viewModel(),
) {
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            vm.loadDocument(context, it)
        }
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
            context.startActivity(Intent.createChooser(intent, "Share Compressed PDF"))
            vm.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compress PDF", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (vm.documentUri != null) {
                        IconButton(onClick = { filePicker.launch(arrayOf("application/pdf")) }) {
                            Icon(Icons.Default.FolderOpen, null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = vm.documentUri == null,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "compressContent",
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyState(
                        colors      = SectionPalette.compress,
                        icon        = Icons.Default.Compress,
                        title       = "Compress PDF",
                        subtitle    = "Reduce your PDF file size while preserving readability.",
                        actionLabel = "Open PDF File",
                        onAction    = { filePicker.launch(arrayOf("application/pdf")) },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SectionHeaderBar(
                            title    = "Compress PDF",
                            subtitle = "Reduce file size for easy sharing",
                            colors   = SectionPalette.compress,
                            icon     = Icons.Default.Compress,
                        )

                        Column(
                            modifier            = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // File info card
                            FileInfoCard(
                                pageCount = vm.pageCount,
                                sizeBytes = vm.originalSizeBytes,
                            )

                            // Quality selector
                            QualitySelector(
                                selected   = vm.quality,
                                onSelect   = { vm.quality = it },
                                origBytes  = vm.originalSizeBytes,
                            )

                            // Page thumbnail preview
                            val renderer = vm.renderer
                            if (renderer != null) {
                                Card(
                                    shape  = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Text(
                                            "Preview",
                                            fontSize   = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            // Show up to 3 page thumbnails
                                            val thumbCount = minOf(vm.pageCount, 3)
                                            repeat(thumbCount) { idx ->
                                                PageThumbnailView(
                                                    renderer  = renderer,
                                                    pageIndex = idx,
                                                    isSelected = false,
                                                    onToggle  = {},
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Compress button
                            GradientButton(
                                text    = "Compress PDF",
                                colors  = SectionPalette.compress,
                                onClick = { vm.compress(context) },
                            )
                        }
                    }
                }
            }

            if (vm.isProcessing) ProcessingOverlay("Compressing PDF…")
        }
    }
}

// ── File info card ─────────────────────────────────────────────────────────────

@Composable
private fun FileInfoCard(pageCount: Int, sizeBytes: Long) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(SectionPalette.compress)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PictureAsPdf, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Original File",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "$pageCount pages  ·  ${sizeBytes.toReadableSize()}",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Size badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SectionPalette.compress.first().copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    sizeBytes.toReadableSize(),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SectionPalette.compress.first(),
                )
            }
        }
    }
}

// ── Quality selector ──────────────────────────────────────────────────────────

@Composable
private fun QualitySelector(
    selected: CompressQuality,
    onSelect: (CompressQuality) -> Unit,
    origBytes: Long,
) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Compression Level",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )

            CompressQuality.entries.forEach { q ->
                val isSelected = q == selected
                val estimatedBytes = (origBytes * q.estimatedRatio).toLong()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) Modifier.background(
                                Brush.horizontalGradient(
                                    SectionPalette.compress.map { it.copy(alpha = 0.12f) }
                                )
                            ) else Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        )
                        .border(
                            width  = if (isSelected) 1.5.dp else 0.dp,
                            color  = if (isSelected) SectionPalette.compress.first() else Color.Transparent,
                            shape  = RoundedCornerShape(12.dp),
                        )
                        .clickable { onSelect(q) }
                        .padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Radio circle
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (isSelected) SectionPalette.compress.first()
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            q.label,
                            fontSize   = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (isSelected) SectionPalette.compress.first()
                                         else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            q.description,
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Estimated size
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "~${estimatedBytes.toReadableSize()}",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isSelected) SectionPalette.compress.first()
                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val savings = ((1f - q.estimatedRatio) * 100).toInt()
                        Text(
                            "~$savings% saved",
                            fontSize = 10.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun Long.toReadableSize(): String = when {
    this < 1024L        -> "$this B"
    this < 1024L * 1024 -> "${this / 1024} KB"
    else                -> String.format("%.1f MB", this / 1024f / 1024f)
}
