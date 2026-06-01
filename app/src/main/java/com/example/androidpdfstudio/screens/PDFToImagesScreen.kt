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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.androidpdfstudio.viewmodels.PDFToImagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFToImagesScreen(
    navController: NavController,
    vm: PDFToImagesViewModel = viewModel(),
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

    // Share result images when ready
    val resultFiles = vm.resultFiles
    LaunchedEffect(resultFiles) {
        if (resultFiles.isNotEmpty()) {
            val uris = ArrayList(resultFiles.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            })
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Images"))
            vm.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF to Images", fontWeight = FontWeight.Bold, color = Color.White) },
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
                targetState   = vm.documentUri == null,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label         = "imagesToContent",
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyState(
                        colors      = SectionPalette.toImages,
                        icon        = Icons.Default.Image,
                        title       = "PDF to Images",
                        subtitle    = "Export every page of your PDF as a high-quality JPEG image.",
                        actionLabel = "Open PDF File",
                        onAction    = { filePicker.launch(arrayOf("application/pdf")) },
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SectionHeaderBar(
                            title    = "PDF to Images",
                            subtitle = "Export pages as JPEG images",
                            colors   = SectionPalette.toImages,
                            icon     = Icons.Default.Image,
                        )

                        // Page count summary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    null,
                                    tint     = SectionPalette.toImages.first(),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    "${vm.pageCount} page${if (vm.pageCount == 1) "" else "s"} → ${vm.pageCount} image${if (vm.pageCount == 1) "" else "s"}",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(SectionPalette.toImages.first().copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("JPEG · High Quality", fontSize = 10.sp, color = SectionPalette.toImages.first(), fontWeight = FontWeight.Medium)
                            }
                        }

                        // Thumbnail grid
                        val renderer = vm.renderer
                        if (renderer != null) {
                            LazyVerticalGrid(
                                columns             = GridCells.Adaptive(100.dp),
                                modifier            = Modifier.weight(1f),
                                contentPadding      = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement   = Arrangement.spacedBy(10.dp),
                            ) {
                                itemsIndexed(List(vm.pageCount) { it }) { _, pageIndex ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        PageThumbnailView(
                                            renderer  = renderer,
                                            pageIndex = pageIndex,
                                            isSelected = false,
                                            onToggle  = {},
                                        )
                                        Text(
                                            "page_${String.format("%03d", pageIndex + 1)}.jpg",
                                            fontSize = 8.sp,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        // Export action bar
                        AnimatedVisibility(
                            visible = true,
                            enter   = slideInVertically { it } + fadeIn(),
                        ) {
                            Surface(
                                shadowElevation = 16.dp,
                                color           = MaterialTheme.colorScheme.surface,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    GradientButton(
                                        text    = "Export ${vm.pageCount} Page${if (vm.pageCount == 1) "" else "s"} as Images",
                                        colors  = SectionPalette.toImages,
                                        onClick = { vm.exportImages(context) },
                                    )
                                    // Quality note
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment     = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint     = SectionPalette.toImages.first(),
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            "High-resolution JPEG · 2.5× scale",
                                            fontSize = 11.sp,
                                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Processing overlay with per-page progress
            if (vm.isProcessing) {
                val page = vm.processingPage
                val msg = if (page >= 0 && vm.pageCount > 0)
                    "Exporting page ${page + 1} / ${vm.pageCount}…"
                else
                    "Processing…"
                ProcessingOverlay(msg)
            }
        }
    }
}
