package com.example.androidpdfstudio.models

import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.example.androidpdfstudio.ui.theme.SectionPalette
import java.util.UUID

// ── PDF Item ──────────────────────────────────────────────────────────────────

data class PDFItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val displayName: String,
    val pageCount: Int = 0,
)

// ── Edit mode ─────────────────────────────────────────────────────────────────

enum class EditMode { TEXT, ERASE }

// ── Erase annotation (white rectangle drawn over existing content) ─────────────

data class EraseAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val x1: Float,   // 0..1 fractions of page width/height
    val y1: Float,
    val x2: Float,
    val y2: Float,
)

// ── Inline edit state (shared between ViewModel and PDFPageView) ──────────────

data class InlineEdit(
    val id: String? = null,          // null = new annotation; non-null = editing existing
    val pageIndex: Int,
    val xFraction: Float,            // 0..1 relative to page width
    val yFraction: Float,            // 0..1 relative to page height
    val text: String,
    val fontSize: Float = 14f,
    val colorArgb: Int = android.graphics.Color.BLACK,
)

// ── Text Annotation ───────────────────────────────────────────────────────────

data class TextAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val pageIndex: Int,
    val xFraction: Float,   // 0..1 relative to page width
    val yFraction: Float,   // 0..1 relative to page height
    val fontSize: Float = 14f,
    val colorArgb: Int = android.graphics.Color.BLACK,
)

// ── Tool Definition ───────────────────────────────────────────────────────────

data class PDFTool(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val icon: String,       // Material icon name
    val gradient: List<Color>,
)

object PDFTools {
    val all = listOf(
        PDFTool(
            title    = "Edit PDF",
            subtitle = "Modify text & add annotations",
            icon     = "edit",
            gradient = SectionPalette.edit,
        ),
        PDFTool(
            title    = "Merge PDF",
            subtitle = "Combine multiple documents",
            icon     = "call_merge",
            gradient = SectionPalette.merge,
        ),
        PDFTool(
            title    = "Split PDF",
            subtitle = "Extract specific pages",
            icon     = "content_cut",
            gradient = SectionPalette.split,
        ),
        PDFTool(
            title    = "Photos to PDF",
            subtitle = "Convert images to document",
            icon     = "photo_library",
            gradient = SectionPalette.photos,
        ),
        PDFTool(
            title    = "Compress PDF",
            subtitle = "Reduce file size, save space",
            icon     = "compress",
            gradient = SectionPalette.compress,
        ),
        PDFTool(
            title    = "PDF to Images",
            subtitle = "Export pages as JPEG images",
            icon     = "image",
            gradient = SectionPalette.toImages,
        ),
    )
}
