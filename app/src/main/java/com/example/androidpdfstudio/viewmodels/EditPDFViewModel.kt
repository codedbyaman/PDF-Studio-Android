package com.example.androidpdfstudio.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpdfstudio.models.EditMode
import com.example.androidpdfstudio.models.EraseAnnotation
import com.example.androidpdfstudio.models.InlineEdit
import com.example.androidpdfstudio.models.TextAnnotation
import com.example.androidpdfstudio.utils.PDFPageRenderer
import com.example.androidpdfstudio.utils.PDFProcessor
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class EditPDFViewModel : ViewModel() {

    var documentUri by mutableStateOf<Uri?>(null)
        private set

    var pageCount by mutableIntStateOf(0)
        private set

    var currentPage by mutableIntStateOf(0)

    var isProcessing by mutableStateOf(false)
        private set

    var resultFile by mutableStateOf<File?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    val annotations = mutableStateListOf<TextAnnotation>()
    val eraseAnnotations = mutableStateListOf<EraseAnnotation>()

    var editMode by mutableStateOf(EditMode.TEXT)
        private set

    // ── Undo stack (max 30 actions) ───────────────────────────────────────────
    private val undoStack = ArrayDeque<() -> Unit>()
    var canUndo by mutableStateOf(false)
        private set

    private fun pushUndo(undo: () -> Unit) {
        undoStack.addLast(undo)
        if (undoStack.size > 30) undoStack.removeFirst()
        canUndo = true
    }

    fun undo() {
        undoStack.removeLastOrNull()?.invoke()
        canUndo = undoStack.isNotEmpty()
    }

    fun switchMode(mode: EditMode) {
        if (mode != editMode) {
            cancelInline()
            editMode = mode
        }
    }

    // ── Inline editing (replaces modal dialog) ────────────────────────────────
    var inlineEdit by mutableStateOf<InlineEdit?>(null)
        private set

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
                currentPage = 0
                annotations.clear()
                eraseAnnotations.clear()
                undoStack.clear()
                canUndo = false
                inlineEdit = null
                editMode = EditMode.TEXT
            } catch (e: Exception) {
                errorMessage = "Failed to open PDF: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * Called when the user taps on a page.
     * If there is an existing annotation near the tap point it is loaded for editing;
     * otherwise a new blank inline edit starts at that location.
     * Any in-progress inline edit is automatically committed first.
     */
    fun startInlineEdit(pageIndex: Int, xFraction: Float, yFraction: Float) {
        commitInline()  // auto-commit any current edit

        val existing = annotations.firstOrNull { ann ->
            ann.pageIndex == pageIndex &&
            abs(ann.xFraction - xFraction) < HIT_RADIUS &&
            abs(ann.yFraction - yFraction) < HIT_RADIUS
        }

        inlineEdit = if (existing != null) {
            InlineEdit(
                id         = existing.id,
                pageIndex  = existing.pageIndex,
                xFraction  = existing.xFraction,
                yFraction  = existing.yFraction,
                text       = existing.text,
                fontSize   = existing.fontSize,
                colorArgb  = existing.colorArgb,
            )
        } else {
            InlineEdit(
                pageIndex = pageIndex,
                xFraction = xFraction,
                yFraction = yFraction,
                text      = "",
            )
        }
    }

    fun updateInlineText(text: String) {
        inlineEdit = inlineEdit?.copy(text = text)
    }

    /**
     * Commits the active inline edit. Empty text cancels silently.
     */
    fun commitInline() {
        val edit = inlineEdit ?: return
        inlineEdit = null
        if (edit.text.isBlank()) return

        if (edit.id != null) {
            val idx = annotations.indexOfFirst { it.id == edit.id }
            if (idx >= 0) {
                val old = annotations[idx]
                annotations[idx] = old.copy(
                    text      = edit.text,
                    fontSize  = edit.fontSize,
                    colorArgb = edit.colorArgb,
                )
                pushUndo {
                    val i = annotations.indexOfFirst { it.id == edit.id }
                    if (i >= 0) annotations[i] = old
                }
            }
        } else {
            val ann = TextAnnotation(
                text      = edit.text,
                pageIndex = edit.pageIndex,
                xFraction = edit.xFraction,
                yFraction = edit.yFraction,
                fontSize  = edit.fontSize,
                colorArgb = edit.colorArgb,
            )
            annotations.add(ann)
            pushUndo { annotations.removeAll { it.id == ann.id } }
        }
    }

    // ── Erase (white-rectangle over existing content) ─────────────────────────

    fun commitErase(pageIndex: Int, x1: Float, y1: Float, x2: Float, y2: Float) {
        val erase = EraseAnnotation(
            pageIndex = pageIndex,
            x1 = x1, y1 = y1, x2 = x2, y2 = y2,
        )
        eraseAnnotations.add(erase)
        pushUndo { eraseAnnotations.removeAll { it.id == erase.id } }
    }

    fun removeErase(id: String) {
        eraseAnnotations.removeAll { it.id == id }
    }

    fun moveAnnotation(id: String, xFraction: Float, yFraction: Float) {
        val idx = annotations.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = annotations[idx]
            annotations[idx] = old.copy(
                xFraction = xFraction.coerceIn(0f, 0.95f),
                yFraction = yFraction.coerceIn(0f, 0.95f),
            )
            pushUndo {
                val i = annotations.indexOfFirst { it.id == id }
                if (i >= 0) annotations[i] = old
            }
        }
    }

    fun cancelInline() { inlineEdit = null }

    fun removeAnnotation(id: String) {
        if (inlineEdit?.id == id) inlineEdit = null
        annotations.removeAll { it.id == id }
    }

    fun saveDocument(context: Context) {
        commitInline()  // flush any open edit before saving
        val uri = documentUri ?: return
        viewModelScope.launch {
            isProcessing = true
            errorMessage = null
            try {
                resultFile = PDFProcessor.annotate(context, uri, annotations.toList(), eraseAnnotations.toList())
            } catch (e: Exception) {
                errorMessage = "Failed to save: ${e.message}"
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

    companion object {
        /** How close (as a fraction of page size) a tap must be to hit an annotation */
        private const val HIT_RADIUS = 0.08f
    }
}
