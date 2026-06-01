package com.example.androidpdfstudio.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.androidpdfstudio.components.PDFPageView
import com.example.androidpdfstudio.models.EditMode
import com.example.androidpdfstudio.ui.theme.NavyDark
import com.example.androidpdfstudio.ui.theme.SectionPalette
import com.example.androidpdfstudio.ui.theme.StudioAccent
import com.example.androidpdfstudio.viewmodels.EditPDFViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPDFScreen(
    navController: NavController,
    vm: EditPDFViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Track the last saved URI so the Share action can use it
    var lastSavedUri by remember { mutableStateOf<Uri?>(null) }

    // ── Open PDF file picker ─────────────────────────────────────────────────
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

    // ── Save As — system file picker so user picks name + location ───────────
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri: Uri? ->
        val tempFile = vm.resultFile
        vm.clearResult()
        if (destUri != null && tempFile != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destUri)?.use { out ->
                            tempFile.inputStream().use { it.copyTo(out) }
                        }
                        true
                    } catch (_: Exception) { false }
                }
                val result = snackbarHostState.showSnackbar(
                    message      = if (ok) "PDF saved to your device" else "Failed to save file",
                    actionLabel  = if (ok) "Share" else null,
                    duration     = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed && ok) {
                    lastSavedUri = destUri
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, destUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                }
            }
        }
    }

    // When the ViewModel finishes processing, open the Save As picker
    val resultFile = vm.resultFile
    LaunchedEffect(resultFile) {
        if (resultFile != null) {
            val name = "PDFStudio_${System.currentTimeMillis()}.pdf"
            saveLauncher.launch(name)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit PDF", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    // Undo — shown only when there is something to undo
                    if (vm.documentUri != null && vm.canUndo) {
                        IconButton(onClick = { vm.undo() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = Color.White)
                        }
                    }
                    if (vm.documentUri != null) {
                        IconButton(onClick = { vm.saveDocument(context) }) {
                            Icon(Icons.Default.Save, "Save", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { filePicker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Default.FolderOpen, "Open", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (vm.documentUri == null) {
                EmptyState(
                    colors      = SectionPalette.edit,
                    icon        = Icons.Default.Edit,
                    title       = "Edit PDF",
                    subtitle    = "Tap a page to add text. Switch to Erase mode to white-out content. Pinch to zoom.",
                    actionLabel = "Open PDF File",
                    onAction    = { filePicker.launch(arrayOf("application/pdf")) },
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    val renderer = vm.renderer

                    // Tool bar — page counter + mode toggle
                    EditToolBar(
                        currentPage  = vm.currentPage + 1,
                        pageCount    = vm.pageCount,
                        editMode     = vm.editMode,
                        onModeChange = { vm.switchMode(it) },
                    )

                    if (renderer != null) {
                        val listState = rememberLazyListState()
                        LaunchedEffect(listState.firstVisibleItemIndex) {
                            vm.currentPage = listState.firstVisibleItemIndex
                        }

                        // Pinch-to-zoom + pan (disabled in Erase mode so fingers can drag-erase)
                        var scale  by remember { mutableFloatStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                            scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                            offset += panChange
                        }
                        LaunchedEffect(scale) { if (scale <= 1f) offset = Offset.Zero }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clipToBounds()
                                .then(
                                    if (vm.editMode == EditMode.TEXT)
                                        Modifier.transformable(state = transformState)
                                    else Modifier
                                ),
                        ) {
                            LazyColumn(
                                state   = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        scaleX       = scale,
                                        scaleY       = scale,
                                        translationX = offset.x,
                                        translationY = offset.y,
                                    ),
                                contentPadding      = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                items(count = vm.pageCount, key = { it }) { pageIndex ->
                                    PDFPageView(
                                        renderer           = renderer,
                                        pageIndex          = pageIndex,
                                        annotations        = vm.annotations.filter { it.pageIndex == pageIndex },
                                        eraseAnnotations   = vm.eraseAnnotations.filter { it.pageIndex == pageIndex },
                                        editMode           = vm.editMode,
                                        onTap              = { x, y -> vm.startInlineEdit(pageIndex, x, y) },
                                        onDeleteAnnotation = { id -> vm.removeAnnotation(id) },
                                        onMoveAnnotation   = { id, x, y -> vm.moveAnnotation(id, x, y) },
                                        onEraseCommit      = { x1, y1, x2, y2 -> vm.commitErase(pageIndex, x1, y1, x2, y2) },
                                        onDeleteErase      = { id -> vm.removeErase(id) },
                                        inlineEdit         = vm.inlineEdit,
                                        onInlineTextChange = { vm.updateInlineText(it) },
                                        onInlineCommit     = { vm.commitInline() },
                                    )
                                }
                            }

                            // Zoom badge
                            if (scale != 1f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "${(scale * 100).toInt()}%",
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Color.White,
                                    )
                                }
                            }

                            // Mode hint
                            val hint = when (vm.editMode) {
                                EditMode.TEXT  -> if (scale != 1f) "Pinch to zoom  •  Drag to pan" else null
                                EditMode.ERASE -> "Drag over text to erase it"
                            }
                            if (hint != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (vm.editMode == EditMode.ERASE) Color(0xCCFF6B00)
                                            else Color.Black.copy(alpha = 0.45f)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text(hint, fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Annotations panel
                    AnimatedVisibility(
                        visible = vm.annotations.isNotEmpty() || vm.eraseAnnotations.isNotEmpty(),
                        enter   = slideInVertically { it } + fadeIn(tween(200)),
                    ) {
                        AnnotationsPanel(
                            annotations = vm.annotations.toList(),
                            eraseCount  = vm.eraseAnnotations.size,
                            onDelete    = { id -> vm.removeAnnotation(id) },
                            onClearAll  = {
                                vm.annotations.clear()
                                vm.eraseAnnotations.clear()
                            },
                        )
                    }
                }
            }

            if (vm.isProcessing) ProcessingOverlay()
        }
    }
}

// ── Edit tool bar ─────────────────────────────────────────────────────────────

@Composable
private fun EditToolBar(
    currentPage: Int,
    pageCount: Int,
    editMode: EditMode,
    onModeChange: (EditMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyDark)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Page $currentPage / $pageCount", color = Color.White, fontSize = 12.sp)

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ToolModeButton(
                label    = "Text",
                icon     = Icons.Default.TextFields,
                selected = editMode == EditMode.TEXT,
                onClick  = { onModeChange(EditMode.TEXT) },
            )
            ToolModeButton(
                label            = "Erase",
                icon             = Icons.Default.Delete,
                selected         = editMode == EditMode.ERASE,
                onClick          = { onModeChange(EditMode.ERASE) },
                tintWhenSelected = Color(0xFFFF6B00),
            )
        }
    }
}

@Composable
private fun ToolModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    tintWhenSelected: Color = StudioAccent,
) {
    val bg   = if (selected) tintWhenSelected.copy(alpha = 0.20f) else Color.Transparent
    val tint = if (selected) tintWhenSelected else Color.White.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
        Text(
            label,
            fontSize   = 11.sp,
            color      = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ── Annotations panel ─────────────────────────────────────────────────────────

@Composable
private fun AnnotationsPanel(
    annotations: List<com.example.androidpdfstudio.models.TextAnnotation>,
    eraseCount: Int,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.TextFields, null, tint = StudioAccent, modifier = Modifier.size(15.dp))
                    val parts = buildList {
                        if (annotations.isNotEmpty()) add("${annotations.size} text")
                        if (eraseCount > 0) add("$eraseCount erase")
                    }.joinToString(" · ")
                    Text(parts, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            if (annotations.isNotEmpty()) {
                Row(
                    modifier              = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    annotations.forEach { ann ->
                        AnnotationChip(
                            text     = ann.text,
                            page     = ann.pageIndex + 1,
                            onDelete = { onDelete(ann.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationChip(
    text: String,
    page: Int,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(StudioAccent.copy(alpha = 0.10f))
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "p$page: $text",
            fontSize  = 11.sp,
            color     = StudioAccent,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            modifier  = Modifier.widthIn(max = 120.dp),
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(11.dp))
            }
        }
    }
}
