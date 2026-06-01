package com.example.androidpdfstudio.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidpdfstudio.models.EditMode
import com.example.androidpdfstudio.models.EraseAnnotation
import com.example.androidpdfstudio.models.InlineEdit
import com.example.androidpdfstudio.models.TextAnnotation
import com.example.androidpdfstudio.ui.theme.StudioAccent
import com.example.androidpdfstudio.utils.PDFPageRenderer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val EDITOR_PAD_H = 6.dp
private val EDITOR_PAD_V = 4.dp

// ── Single PDF Page with shimmer placeholder ──────────────────────────────────

@Composable
fun PDFPageView(
    renderer: PDFPageRenderer,
    pageIndex: Int,
    annotations: List<TextAnnotation> = emptyList(),
    eraseAnnotations: List<EraseAnnotation> = emptyList(),
    editMode: EditMode = EditMode.TEXT,
    onTap: ((xFraction: Float, yFraction: Float) -> Unit)? = null,
    onDeleteAnnotation: ((id: String) -> Unit)? = null,
    onMoveAnnotation: ((id: String, xFraction: Float, yFraction: Float) -> Unit)? = null,
    onEraseCommit: ((x1: Float, y1: Float, x2: Float, y2: Float) -> Unit)? = null,
    onDeleteErase: ((id: String) -> Unit)? = null,
    inlineEdit: InlineEdit? = null,
    onInlineTextChange: ((String) -> Unit)? = null,
    onInlineCommit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(renderer, pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(renderer, pageIndex) {
        bitmap = null
        bitmap = renderer.renderPage(pageIndex)
    }

    val alpha by animateFloatAsState(
        targetValue   = if (bitmap != null) 1f else 0f,
        animationSpec = tween(250),
        label         = "pageAlpha",
    )

    var eraseStartFrac   by remember { mutableStateOf<Offset?>(null) }
    var eraseCurrentFrac by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
    ) {
        if (bitmap == null) ShimmerPage()

        bitmap?.let { bmp ->
            val aspectRatio = bmp.width.toFloat() / bmp.height.toFloat()
            val activeInlineEdit = inlineEdit?.takeIf { it.pageIndex == pageIndex }

            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = alpha)
                    .then(
                        when {
                            editMode == EditMode.ERASE && onEraseCommit != null ->
                                Modifier.pointerInput(editMode, bmp) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            eraseStartFrac   = Offset((offset.x / size.width).coerceIn(0f, 1f), (offset.y / size.height).coerceIn(0f, 1f))
                                            eraseCurrentFrac = eraseStartFrac
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            eraseCurrentFrac = Offset((change.position.x / size.width).coerceIn(0f, 1f), (change.position.y / size.height).coerceIn(0f, 1f))
                                        },
                                        onDragEnd = {
                                            val s = eraseStartFrac; val e = eraseCurrentFrac
                                            if (s != null && e != null) {
                                                val x1 = min(s.x, e.x); val y1 = min(s.y, e.y)
                                                val x2 = max(s.x, e.x); val y2 = max(s.y, e.y)
                                                if (x2 - x1 > 0.01f && y2 - y1 > 0.005f) onEraseCommit(x1, y1, x2, y2)
                                            }
                                            eraseStartFrac = null; eraseCurrentFrac = null
                                        },
                                        onDragCancel = { eraseStartFrac = null; eraseCurrentFrac = null },
                                    )
                                }

                            onTap != null ->
                                Modifier.pointerInput(editMode, bmp) {
                                    detectTapGestures { offset ->
                                        onTap(
                                            (offset.x / size.width).coerceIn(0f, 1f),
                                            (offset.y / size.height).coerceIn(0f, 1f),
                                        )
                                    }
                                }

                            else -> Modifier
                        }
                    )
            ) {
                Image(
                    bitmap             = bmp.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale       = ContentScale.FillWidth,
                    modifier           = Modifier.fillMaxWidth(),
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val pageW = maxWidth
                    val pageH = (maxWidth.value / aspectRatio).dp
                    val density = LocalDensity.current
                    val pageWPx = with(density) { pageW.toPx() }
                    val pageHPx = with(density) { pageH.toPx() }

                    // 1. Erase rectangles
                    eraseAnnotations.filter { it.pageIndex == pageIndex }.forEach { erase ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = pageW * erase.x1, y = pageH * erase.y1)
                                .size(width = pageW * (erase.x2 - erase.x1), height = pageH * (erase.y2 - erase.y1))
                                .background(Color.White)
                        ) {
                            if (onDeleteErase != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(15.dp)
                                        .background(Color.Red.copy(alpha = 0.82f), CircleShape)
                                        .pointerInput(erase.id) { detectTapGestures { onDeleteErase(erase.id) } },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(8.dp))
                                }
                            }
                        }
                    }

                    // 2. Erase live preview
                    val s = eraseStartFrac; val e = eraseCurrentFrac
                    if (editMode == EditMode.ERASE && s != null && e != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = pageW * min(s.x, e.x), y = pageH * min(s.y, e.y))
                                .size(width = pageW * abs(s.x - e.x), height = pageH * abs(s.y - e.y))
                                .background(Color(0x33FF6B00))
                                .border(1.5.dp, Color(0xFFFF6B00), RoundedCornerShape(2.dp))
                        )
                    }

                    // 3. Text annotations — drag handle uses PointerEventPass.Initial to grab
                    //    events before LazyColumn scroll can claim them.
                    annotations.filter { it.id != activeInlineEdit?.id }.forEach { ann ->
                        var dragDelta by remember(ann.id) { mutableStateOf(Offset.Zero) }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(
                                    x = pageW * (ann.xFraction + dragDelta.x),
                                    y = pageH * (ann.yFraction + dragDelta.y),
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 18.dp, bottom = 2.dp),
                            ) {
                                // ── Drag handle ─────────────────────────────────────────────
                                // Uses Initial pass so it beats LazyColumn scroll and page-tap.
                                if (editMode == EditMode.TEXT && onMoveAnnotation != null) {
                                    Icon(
                                        Icons.Default.OpenWith,
                                        contentDescription = "Drag to move",
                                        tint     = Color(ann.colorArgb).copy(alpha = 0.38f),
                                        modifier = Modifier
                                            .size(22.dp)
                                            .padding(3.dp)
                                            .pointerInput(ann.id, pageWPx, pageHPx) {
                                                awaitEachGesture {
                                                    // Grab the down event in Initial pass so LazyColumn
                                                    // never sees an unconsumed down → scroll won't start.
                                                    val down = awaitFirstDown(
                                                        requireUnconsumed = false,
                                                        pass = PointerEventPass.Initial,
                                                    )
                                                    down.consume()

                                                    var delta = Offset.Zero

                                                    while (true) {
                                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                                        val change = event.changes.firstOrNull { it.id == down.id }
                                                            ?: break
                                                        if (!change.pressed) break
                                                        change.consume()
                                                        delta = Offset(
                                                            delta.x + (change.position.x - change.previousPosition.x) / pageWPx,
                                                            delta.y + (change.position.y - change.previousPosition.y) / pageHPx,
                                                        )
                                                        dragDelta = delta
                                                    }

                                                    // Commit if moved meaningfully
                                                    if (abs(delta.x) > 0.003f || abs(delta.y) > 0.003f) {
                                                        onMoveAnnotation(
                                                            ann.id,
                                                            (ann.xFraction + delta.x).coerceIn(0f, 0.95f),
                                                            (ann.yFraction + delta.y).coerceIn(0f, 0.95f),
                                                        )
                                                    }
                                                    dragDelta = Offset.Zero
                                                }
                                            },
                                    )
                                    Spacer(Modifier.width(2.dp))
                                }

                                // Text — no pointer modifier; taps propagate to page's detectTapGestures
                                Text(
                                    text       = ann.text,
                                    fontSize   = ann.fontSize.sp,
                                    fontWeight = FontWeight.Normal,
                                    color      = Color(ann.colorArgb),
                                )
                            }

                            // ×-delete badge
                            if (onDeleteAnnotation != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(15.dp)
                                        .background(Color.Red.copy(alpha = 0.82f), CircleShape)
                                        .pointerInput(ann.id) { detectTapGestures { onDeleteAnnotation(ann.id) } },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(8.dp))
                                }
                            }
                        }
                    }

                    // 4. Inline editor — offset compensated so text baseline aligns with committed Text
                    if (activeInlineEdit != null) {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            try { focusRequester.requestFocus() } catch (_: Exception) {}
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(
                                    x = pageW * activeInlineEdit.xFraction - EDITOR_PAD_H,
                                    y = pageH * activeInlineEdit.yFraction - EDITOR_PAD_V,
                                )
                                .widthIn(min = 90.dp, max = pageW * 0.85f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.90f))
                                .border(1.5.dp, StudioAccent, RoundedCornerShape(4.dp))
                                .padding(horizontal = EDITOR_PAD_H, vertical = EDITOR_PAD_V),
                        ) {
                            BasicTextField(
                                value         = activeInlineEdit.text,
                                onValueChange = { onInlineTextChange?.invoke(it) },
                                textStyle     = TextStyle(
                                    fontSize   = activeInlineEdit.fontSize.sp,
                                    color      = Color(activeInlineEdit.colorArgb),
                                    fontWeight = FontWeight.Normal,
                                ),
                                modifier        = Modifier.focusRequester(focusRequester).widthIn(min = 80.dp),
                                cursorBrush     = SolidColor(StudioAccent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onInlineCommit?.invoke() }),
                                decorationBox   = { inner ->
                                    if (activeInlineEdit.text.isEmpty()) {
                                        Text(
                                            "Type here…",
                                            style = TextStyle(
                                                fontSize = activeInlineEdit.fontSize.sp,
                                                color    = Color.Gray.copy(alpha = 0.55f),
                                            ),
                                        )
                                    }
                                    inner()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Shimmer placeholder ───────────────────────────────────────────────────────

@Composable
private fun ShimmerPage() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue  = -1f,
        targetValue   = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label         = "shimmerX",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.707f)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFEAEAEA), Color(0xFFF5F5F5), Color(0xFFEAEAEA)),
                    start = Offset(shimmerX * 1000f - 500f, 0f),
                    end   = Offset(shimmerX * 1000f, 0f),
                )
            )
    )
}
