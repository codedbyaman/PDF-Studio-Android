package com.example.androidpdfstudio.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpdfstudio.utils.PDFPageRenderer
import com.example.androidpdfstudio.utils.PDFProcessor
import kotlinx.coroutines.launch
import java.io.File

enum class CompressQuality(
    val label: String,
    val description: String,
    val scale: Float,
    val jpegQuality: Int,
    /** Rough estimated ratio of compressed / original file size */
    val estimatedRatio: Float,
) {
    LOW("Low", "Smallest file · lower resolution", 0.9f, 45, 0.18f),
    MEDIUM("Medium", "Balanced size & quality", 1.4f, 68, 0.42f),
    HIGH("High", "Better quality · moderate savings", 2.0f, 85, 0.68f),
}

class CompressPDFViewModel : ViewModel() {

    var documentUri by mutableStateOf<Uri?>(null)
        private set

    var originalSizeBytes by mutableLongStateOf(0L)
        private set

    var pageCount by mutableIntStateOf(0)
        private set

    var quality by mutableStateOf(CompressQuality.MEDIUM)

    var isProcessing by mutableStateOf(false)
        private set

    var resultFile by mutableStateOf<File?>(null)
    var resultSizeBytes by mutableLongStateOf(0L)
        private set

    var renderer: PDFPageRenderer? = null
        private set

    fun loadDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            isProcessing = true
            try {
                renderer?.close()
                val r = PDFPageRenderer(context, uri)
                r.open()
                renderer = r
                documentUri = uri
                pageCount = r.pageCount
                originalSizeBytes = context.contentResolver
                    .openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } finally {
                isProcessing = false
            }
        }
    }

    fun compress(context: Context) {
        val uri = documentUri ?: return
        viewModelScope.launch {
            isProcessing = true
            try {
                val file = PDFProcessor.compress(context, uri, quality.scale, quality.jpegQuality)
                resultSizeBytes = file.length()
                resultFile = file
            } finally {
                isProcessing = false
            }
        }
    }

    fun clearResult() { resultFile = null; resultSizeBytes = 0L }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { renderer?.close() }
    }
}
