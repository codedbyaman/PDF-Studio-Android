package com.example.androidpdfstudio.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpdfstudio.utils.PDFPageRenderer
import com.example.androidpdfstudio.utils.PDFProcessor
import kotlinx.coroutines.launch
import java.io.File

class PDFToImagesViewModel : ViewModel() {

    var documentUri by mutableStateOf<Uri?>(null)
        private set

    var pageCount by mutableIntStateOf(0)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    /** Index of the page currently being exported (0-based), -1 when idle */
    var processingPage by mutableIntStateOf(-1)
        private set

    var resultFiles by mutableStateOf<List<File>>(emptyList())
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
            } finally {
                isProcessing = false
            }
        }
    }

    fun exportImages(context: Context) {
        val uri = documentUri ?: return
        viewModelScope.launch {
            isProcessing = true
            processingPage = 0
            try {
                val files = PDFProcessor.toImages(
                    context     = context,
                    uri         = uri,
                    scale       = 2.5f,
                    onPageDone  = { idx -> processingPage = idx + 1 },
                )
                resultFiles = files
            } finally {
                isProcessing = false
                processingPage = -1
            }
        }
    }

    fun clearResult() { resultFiles = emptyList() }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { renderer?.close() }
    }
}
