package com.example.androidpdfstudio.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpdfstudio.utils.PDFPageRenderer
import com.example.androidpdfstudio.utils.PDFProcessor
import kotlinx.coroutines.launch
import java.io.File

class SplitPDFViewModel : ViewModel() {

    var documentUri by mutableStateOf<Uri?>(null)
        private set

    var pageCount by mutableStateOf(0)
        private set

    val selectedPages = mutableStateSetOf<Int>()

    var isProcessing by mutableStateOf(false)
    var resultFile by mutableStateOf<File?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    var renderer: PDFPageRenderer? = null
        private set

    fun loadDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            isProcessing = true
            errorMessage = null
            try {
                renderer?.close()
                val r = PDFPageRenderer(context, uri)
                r.open()
                renderer = r
                documentUri = uri
                pageCount = r.pageCount
                selectedPages.clear()
            } catch (e: Exception) {
                errorMessage = "Failed to open PDF: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun togglePage(index: Int) {
        if (selectedPages.contains(index)) selectedPages.remove(index)
        else selectedPages.add(index)
    }

    fun selectAll() {
        selectedPages.addAll((0 until pageCount).toSet())
    }

    fun selectNone() {
        selectedPages.clear()
    }

    fun extract(context: Context) {
        if (selectedPages.isEmpty()) return
        val uri = documentUri ?: return
        viewModelScope.launch {
            isProcessing = true
            errorMessage = null
            try {
                resultFile = PDFProcessor.split(context, uri, selectedPages.toSet())
            } catch (e: Exception) {
                errorMessage = "Split failed: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun clearResult() { resultFile = null }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { renderer?.close() }
    }
}
