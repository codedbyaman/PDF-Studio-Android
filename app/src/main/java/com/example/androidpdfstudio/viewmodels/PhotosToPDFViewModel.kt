package com.example.androidpdfstudio.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpdfstudio.utils.PDFProcessor
import kotlinx.coroutines.launch
import java.io.File

class PhotosToPDFViewModel : ViewModel() {

    val photoUris = mutableStateListOf<Uri>()

    var isProcessing by mutableStateOf(false)
    var resultFile by mutableStateOf<File?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    fun addPhotos(uris: List<Uri>) {
        val remaining = 50 - photoUris.size
        photoUris.addAll(uris.take(remaining))
    }

    fun removePhoto(uri: Uri) {
        photoUris.remove(uri)
    }

    fun convert(context: Context) {
        if (photoUris.isEmpty()) return
        viewModelScope.launch {
            isProcessing = true
            errorMessage = null
            try {
                resultFile = PDFProcessor.imagesToPdf(context, photoUris.toList())
            } catch (e: Exception) {
                errorMessage = "Conversion failed: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun clearResult() { resultFile = null }
}
