package com.example.androidpdfstudio.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.androidpdfstudio.models.EraseAnnotation
import com.example.androidpdfstudio.models.TextAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ── PDF Renderer wrapper (thread-safe via Mutex) ──────────────────────────────

class PDFPageRenderer(
    private val context: Context,
    private val uri: Uri,
) {
    private val mutex = Mutex()
    private var renderer: PdfRenderer? = null
    private var fd: ParcelFileDescriptor? = null

    val pageCount: Int get() = renderer?.pageCount ?: 0

    suspend fun open() = withContext(Dispatchers.IO) {
        mutex.withLock {
            fd?.close(); renderer?.close()
            fd = context.contentResolver.openFileDescriptor(uri, "r")
            renderer = fd?.let { PdfRenderer(it) }
        }
    }

    suspend fun renderPage(index: Int, scale: Float = 2.5f): Bitmap? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val r = renderer ?: return@withContext null
                if (index < 0 || index >= r.pageCount) return@withContext null
                val page = r.openPage(index)
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bmp
            }
        }

    suspend fun close() = withContext(Dispatchers.IO) {
        mutex.withLock {
            renderer?.close(); fd?.close()
            renderer = null; fd = null
        }
    }
}

// ── PDF Processor (static operations) ────────────────────────────────────────

object PDFProcessor {

    private const val RENDER_SCALE = 2.5f

    // ── Merge multiple PDFs into one ─────────────────────────────────────────
    suspend fun merge(context: Context, uris: List<Uri>): File =
        withContext(Dispatchers.IO) {
            val doc = PdfDocument()
            var globalPage = 1
            uris.forEach { uri ->
                val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@forEach
                val renderer = PdfRenderer(fd)
                repeat(renderer.pageCount) { i ->
                    val page = renderer.openPage(i)
                    val bmp = Bitmap.createBitmap(
                        (page.width * RENDER_SCALE).toInt(),
                        (page.height * RENDER_SCALE).toInt(),
                        Bitmap.Config.ARGB_8888
                    ).also {
                        it.eraseColor(Color.WHITE)
                        page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                    page.close()
                    val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, globalPage++).create()
                    val pdfPage = doc.startPage(info)
                    pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                    doc.finishPage(pdfPage)
                    bmp.recycle()
                }
                renderer.close(); fd.close()
            }
            val out = tempPdfFile(context, "merged")
            FileOutputStream(out).use { doc.writeTo(it) }
            doc.close()
            out
        }

    // ── Split: extract selected pages ─────────────────────────────────────────
    suspend fun split(context: Context, uri: Uri, pages: Set<Int>): File =
        withContext(Dispatchers.IO) {
            val doc = PdfDocument()
            val fd = context.contentResolver.openFileDescriptor(uri, "r")!!
            val renderer = PdfRenderer(fd)
            var outPage = 1
            pages.sorted().forEach { i ->
                if (i >= renderer.pageCount) return@forEach
                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(
                    (page.width * RENDER_SCALE).toInt(),
                    (page.height * RENDER_SCALE).toInt(),
                    Bitmap.Config.ARGB_8888
                ).also {
                    it.eraseColor(Color.WHITE)
                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
                page.close()
                val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, outPage++).create()
                val pdfPage = doc.startPage(info)
                pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                doc.finishPage(pdfPage)
                bmp.recycle()
            }
            renderer.close(); fd.close()
            val out = tempPdfFile(context, "split")
            FileOutputStream(out).use { doc.writeTo(it) }
            doc.close()
            out
        }

    // ── Photos → PDF ─────────────────────────────────────────────────────────
    suspend fun imagesToPdf(context: Context, uris: List<Uri>): File =
        withContext(Dispatchers.IO) {
            val doc = PdfDocument()
            uris.forEachIndexed { idx, uri ->
                val bmp = decodeBitmap(context, uri) ?: return@forEachIndexed
                // A4 landscape or portrait based on image orientation
                val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, idx + 1).create()
                val page = doc.startPage(info)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                doc.finishPage(page)
                bmp.recycle()
            }
            val out = tempPdfFile(context, "photos")
            FileOutputStream(out).use { doc.writeTo(it) }
            doc.close()
            out
        }

    // ── Save PDF with text annotations + erase rectangles ────────────────────
    suspend fun annotate(
        context: Context,
        uri: Uri,
        annotations: List<TextAnnotation>,
        eraseAnnotations: List<EraseAnnotation> = emptyList(),
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val fd = context.contentResolver.openFileDescriptor(uri, "r")!!
        val renderer = PdfRenderer(fd)
        repeat(renderer.pageCount) { i ->
            val page = renderer.openPage(i)
            val bmp = Bitmap.createBitmap(
                (page.width * RENDER_SCALE).toInt(),
                (page.height * RENDER_SCALE).toInt(),
                Bitmap.Config.ARGB_8888
            ).also {
                it.eraseColor(Color.WHITE)
                page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            page.close()

            val canvas = Canvas(bmp)

            // 1. Draw erase rectangles (white, covers existing content)
            val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            eraseAnnotations.filter { it.pageIndex == i }.forEach { erase ->
                canvas.drawRect(
                    erase.x1 * bmp.width,
                    erase.y1 * bmp.height,
                    erase.x2 * bmp.width,
                    erase.y2 * bmp.height,
                    erasePaint,
                )
            }

            // 2. Draw text annotations on top
            annotations.filter { it.pageIndex == i }.forEach { ann ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ann.colorArgb
                    textSize = ann.fontSize * RENDER_SCALE
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                canvas.drawText(ann.text, ann.xFraction * bmp.width, ann.yFraction * bmp.height, paint)
            }

            val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create()
            val pdfPage = doc.startPage(info)
            pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(pdfPage)
            bmp.recycle()
        }
        renderer.close(); fd.close()
        val out = tempPdfFile(context, "edited")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        out
    }

    // ── Compress PDF via lower-DPI re-render + JPEG encoding ─────────────────
    suspend fun compress(
        context: Context,
        uri: Uri,
        scale: Float,
        jpegQuality: Int,
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val fd = context.contentResolver.openFileDescriptor(uri, "r")!!
        val renderer = PdfRenderer(fd)
        repeat(renderer.pageCount) { i ->
            val page = renderer.openPage(i)
            val w = (page.width * scale).toInt().coerceAtLeast(1)
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            val raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            raw.eraseColor(Color.WHITE)
            page.render(raw, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // JPEG-encode then decode to achieve compression artefacts
            val baos = java.io.ByteArrayOutputStream()
            raw.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
            raw.recycle()
            val jpegBytes = baos.toByteArray()
            val bmp = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

            val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create()
            val pdfPage = doc.startPage(info)
            pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(pdfPage)
            bmp.recycle()
        }
        renderer.close(); fd.close()
        val out = tempPdfFile(context, "compressed")
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        out
    }

    // ── Export each page as a JPEG file ───────────────────────────────────────
    suspend fun toImages(
        context: Context,
        uri: Uri,
        scale: Float = 2f,
        onPageDone: ((index: Int) -> Unit)? = null,
    ): List<File> = withContext(Dispatchers.IO) {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")!!
        val renderer = PdfRenderer(fd)
        val dir = java.io.File(context.cacheDir, "pdf_images_${System.currentTimeMillis()}").apply { mkdirs() }
        val files = mutableListOf<File>()
        repeat(renderer.pageCount) { i ->
            val page = renderer.openPage(i)
            val w = (page.width * scale).toInt().coerceAtLeast(1)
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            val file = java.io.File(dir, "page_${String.format("%03d", i + 1)}.jpg")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            bmp.recycle()
            files.add(file)
            withContext(Dispatchers.Main) { onPageDone?.invoke(i) }
        }
        renderer.close(); fd.close()
        files
    }

    // ── Get page count from URI ───────────────────────────────────────────────
    suspend fun pageCount(context: Context, uri: Uri): Int =
        withContext(Dispatchers.IO) {
            try {
                val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext 0
                val count = PdfRenderer(fd).also { it.close() }.pageCount
                fd.close()
                count
            } catch (e: Exception) { 0 }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun tempPdfFile(context: Context, prefix: String): File {
        val dir = context.cacheDir.apply { mkdirs() }
        return File(dir, "${prefix}_${System.currentTimeMillis()}.pdf")
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) { null }
}
