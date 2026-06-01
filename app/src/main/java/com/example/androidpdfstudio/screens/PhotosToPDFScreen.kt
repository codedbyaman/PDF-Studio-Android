package com.example.androidpdfstudio.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.androidpdfstudio.components.GradientButton
import com.example.androidpdfstudio.ui.theme.NavyDark
import com.example.androidpdfstudio.ui.theme.SectionPalette
import com.example.androidpdfstudio.ui.theme.StudioAccent
import com.example.androidpdfstudio.viewmodels.PhotosToPDFViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosToPDFScreen(
    navController: NavController,
    vm: PhotosToPDFViewModel = viewModel(),
) {
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris: List<Uri> -> vm.addPhotos(uris) }

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
            context.startActivity(Intent.createChooser(intent, "Share PDF"))
            vm.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photos to PDF", fontWeight = FontWeight.Bold, color = Color.White) },
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
                    title    = "Photos to PDF",
                    subtitle = "Convert images to a PDF document",
                    colors   = SectionPalette.photos,
                    icon     = Icons.Default.Collections,
                )

                AnimatedContent(
                    targetState = vm.photoUris.isEmpty(),
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    modifier     = Modifier.weight(1f),
                    label        = "photosContent",
                ) { isEmpty ->
                    if (isEmpty) {
                        EmptyState(
                            colors      = SectionPalette.photos,
                            icon        = Icons.Default.Collections,
                            title       = "No Photos Selected",
                            subtitle    = "Pick photos from your gallery to convert to PDF.",
                            actionLabel = "Select Photos",
                            onAction    = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Count header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${vm.photoUris.size} photo(s)",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onBackground,
                                    modifier   = Modifier.weight(1f),
                                )
                                TextButton(onClick = { vm.photoUris.clear() }) {
                                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }

                            // Photo grid — 3 columns, square cells
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement   = Arrangement.spacedBy(2.dp),
                            ) {
                                items(vm.photoUris.toList(), key = { it.toString() }) { uri ->
                                    Box(modifier = Modifier.aspectRatio(1f)) {
                                        UriImage(uri = uri, modifier = Modifier.fillMaxSize())

                                        // Remove button
                                        IconButton(
                                            onClick = { vm.removePhoto(uri) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(22.dp)
                                                .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                        ) {
                                            Icon(
                                                Icons.Default.Close, null,
                                                tint     = Color.White,
                                                modifier = Modifier.size(12.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action bar
                AnimatedVisibility(
                    visible = vm.photoUris.isNotEmpty(),
                    enter   = slideInVertically { it } + fadeIn(),
                ) {
                    Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            GradientButton(
                                text   = "Convert ${vm.photoUris.size} Photo(s) to PDF",
                                colors = SectionPalette.photos,
                                onClick = { vm.convert(context) },
                            )
                            TextButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text("Add More Photos", color = StudioAccent)
                            }
                        }
                    }
                }
            }

            if (vm.isProcessing) ProcessingOverlay("Converting to PDF…")
        }
    }
}

// ── URI → Bitmap composable (no third-party library) ─────────────────────────

@Composable
private fun UriImage(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) { null }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap           = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale     = ContentScale.Crop,
            modifier         = modifier,
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
