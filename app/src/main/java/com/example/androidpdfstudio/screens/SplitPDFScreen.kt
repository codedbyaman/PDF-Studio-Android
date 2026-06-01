package com.example.androidpdfstudio.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.androidpdfstudio.ui.theme.StudioAccent
import com.example.androidpdfstudio.viewmodels.SplitPDFViewModel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPDFScreen(
    navController: NavController,
    vm: SplitPDFViewModel = viewModel(),
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
            context.startActivity(Intent.createChooser(intent, "Share Split PDF"))
            vm.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split PDF", fontWeight = FontWeight.Bold, color = Color.White) },
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
                    title    = "Split PDF",
                    subtitle = "Select pages to extract",
                    colors   = SectionPalette.split,
                    icon     = Icons.Default.ContentCut,
                )

                if (vm.documentUri == null) {
                    EmptyState(
                        colors      = SectionPalette.split,
                        icon        = Icons.Default.ContentCut,
                        title       = "Split PDF",
                        subtitle    = "Open a PDF and select the pages you want to extract.",
                        actionLabel = "Open PDF File",
                        onAction    = { filePicker.launch(arrayOf("application/pdf")) },
                    )
                } else {
                    val renderer = vm.renderer
                    if (renderer != null && vm.pageCount > 0) {

                        // Selection bar
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${vm.selectedPages.size} selected of ${vm.pageCount} pages",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color    = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { vm.selectAll() }) {
                                    Text("All", color = StudioAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                TextButton(onClick = { vm.selectNone() }) {
                                    Text("None", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }

                            // Animated progress bar
                            LinearProgressIndicator(
                                progress     = {
                                    if (vm.pageCount > 0)
                                        vm.selectedPages.size.toFloat() / vm.pageCount.toFloat()
                                    else 0f
                                },
                                modifier     = Modifier.fillMaxWidth().height(3.dp),
                                color        = SectionPalette.split.first(),
                                trackColor   = SectionPalette.split.first().copy(alpha = 0.15f),
                            )
                        }

                        // Thumbnail grid with staggered appearance
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 92.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                            verticalArrangement   = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                        ) {
                            items(count = vm.pageCount, key = { it }) { index ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(index * 30L)
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter   = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.85f),
                                ) {
                                    PageThumbnailView(
                                        renderer   = renderer,
                                        pageIndex  = index,
                                        isSelected = vm.selectedPages.contains(index),
                                        onToggle   = { vm.togglePage(index) },
                                    )
                                }
                            }
                        }

                        // Action bar
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
                                GradientButton(
                                    text    = if (vm.selectedPages.isEmpty())
                                        "Select Pages to Extract"
                                    else
                                        "Extract ${vm.selectedPages.size} Page(s)",
                                    colors  = SectionPalette.split,
                                    enabled = vm.selectedPages.isNotEmpty(),
                                    onClick = { vm.extract(context) },
                                )
                                TextButton(
                                    onClick  = { filePicker.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                ) {
                                    Text("Open Different PDF", color = StudioAccent)
                                }
                            }
                        }
                    }
                }
            }

            if (vm.isProcessing) ProcessingOverlay("Extracting pages…")
        }
    }
}
