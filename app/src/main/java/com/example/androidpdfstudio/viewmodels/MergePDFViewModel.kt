package com.example.androidpdfstudio.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpdfstudio.models.PDFItem
import com.example.androidpdfstudio.utils.PDFProcessor
import kotlinx.coroutines.launch
import java.io.File

class MergePDFViewModel : ViewModel() {

    val items = mutableStateListOf<PDFItem>()

    var isProcessing by mutableStateOf(false)
    var resultFile by mutableStateOf<File?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    fun addItems(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEach { uri ->
                val name = resolveFileName(context, uri)
                val count = PDFProcessor.pageCount(context, uri)
                items.add(PDFItem(uri = uri, displayName = name, pageCount = count))
            }
        }
    }

    fun removeItem(id: String) {
        items.removeAll { it.id == id }
    }

    fun moveUp(index: Int) {
        if (index > 0) {
            val item = items.removeAt(index)
            items.add(index - 1, item)
        }
    }

    fun moveDown(index: Int) {
        if (index < items.lastIndex) {
            val item = items.removeAt(index)
            items.add(index + 1, item)
        }
    }

    fun merge(context: Context) {
        if (items.size < 2) return
        viewModelScope.launch {
            isProcessing = true
            errorMessage = null
            try {
                resultFile = PDFProcessor.merge(context, items.map { it.uri })
            } catch (e: Exception) {
                errorMessage = "Merge failed: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun clearResult() { resultFile = null }

    private fun resolveFileName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(idx)
            }?.removeSuffix(".pdf") ?: uri.lastPathSegment ?: "Document"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Document"
        }
    }
}
