package com.flxrs.dankchat.ui.chat.messages.common

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.core.graphics.drawable.toBitmap
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import com.flxrs.dankchat.data.repo.emote.SevenTVPaint
import com.flxrs.dankchat.ui.chat.emote.ClockedAnimationDrawable
import com.flxrs.dankchat.ui.chat.emote.EmoteAnimationCoordinator
import com.flxrs.dankchat.ui.chat.emote.LocalChatPageVisible
import com.flxrs.dankchat.ui.chat.emote.LocalEmoteAnimationCoordinator
import com.flxrs.dankchat.ui.chat.emote.loadPaintImage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A part of a message text that is drawn with a paint, [userColor] shows through its transparent parts. */
@Immutable
internal data class PaintedRange(
    val paint: SevenTVPaint,
    val start: Int,
    val end: Int,
    val userColor: Color,
)

/** The paints of a message text, loads their images and keeps animations running while the page is visible. */
@Composable
internal fun rememberPaintedText(ranges: List<PaintedRange>): PaintedText? {
    if (ranges.isEmpty()) {
        return null
    }

    val context = LocalPlatformContext.current
    val emoteCoordinator = LocalEmoteAnimationCoordinator.current
    val isPageVisible = LocalChatPageVisible.current
    val density = LocalDensity.current.density
    val paintedText = remember(ranges, density, emoteCoordinator) { PaintedText(ranges, density, emoteCoordinator) }
    LaunchedEffect(paintedText, isPageVisible) {
        if (isPageVisible) {
            paintedText.loadMissing(context)
        }
    }
    DisposableEffect(paintedText, isPageVisible) {
        paintedText.setInvalidationsEnabled(isPageVisible)
        onDispose { paintedText.setInvalidationsEnabled(false) }
    }
    return paintedText
}

/**
 * Painted parts are cut out of the message text and drawn from layouts of their own, so the message is only laid out once.
 * Offscreen layers per name would cost more than everything else in a row, on every frame the screen redraws. Images are
 * shaders instead, an animated one needs a new shader per frame, so it only redraws the layer of its own text.
 */
@Stable
internal class PaintedText(
    private val ranges: List<PaintedRange>,
    private val density: Float,
    private val emoteCoordinator: EmoteAnimationCoordinator,
) {
    private val urls = ranges.mapNotNullTo(mutableSetOf()) { (it.paint.fill as? SevenTVPaint.Fill.Image)?.url }
    private val images = HashMap<String, Drawable>()
    private val registeredDrawables = HashSet<Drawable>()
    private var invalidationsEnabled = false

    // Read while drawing, so loads and animation frames only redraw the painted text
    private var drawTick by mutableIntStateOf(0)
    private val invalidationListener: () -> Unit = { drawTick++ }

    init {
        urls.forEach { url -> emoteCoordinator.getCached(url)?.let { images[url] = it } }
    }

    suspend fun loadMissing(context: PlatformContext) = coroutineScope {
        urls.forEach { url ->
            if (url !in images) {
                launch {
                    // Failed images keep the color of the user
                    images[url] = emoteCoordinator.loadPaintImage(context, url) ?: return@launch
                    updateRegistrations()
                    drawTick++
                }
            }
        }
    }

    fun setInvalidationsEnabled(enabled: Boolean) {
        invalidationsEnabled = enabled
        updateRegistrations()
    }

    private fun updateRegistrations() {
        val drawables =
            when {
                invalidationsEnabled -> images.values.toSet()
                else -> emptySet()
            }
        (registeredDrawables - drawables).forEach { emoteCoordinator.unregisterInvalidationListener(it, invalidationListener) }
        (drawables - registeredDrawables).forEach { emoteCoordinator.registerInvalidationListener(it, invalidationListener) }
        registeredDrawables.clear()
        registeredDrawables.addAll(drawables)
    }

    fun measureSegments(
        layout: TextLayoutResult,
        measure: (AnnotatedString) -> TextLayoutResult,
    ): List<PaintedSegment> {
        val text = layout.layoutInput.text
        return ranges.flatMap { range ->
            // The text can be cut off before the range, e.g. by the split of ASCII art
            if (range.start >= text.length) {
                return@flatMap emptyList()
            }

            val lines = layout.paintedLines(range.start, text.paintedEnd(range.start, minOf(range.end, text.length)))
            val bounds = lines.map { it.bounds }.reduceOrNull(Rect::union) ?: return@flatMap emptyList()
            lines.mapNotNull { line ->
                val shadow = range.paint.shadow?.let { Shadow(Color(it.color), Offset(it.xOffset * density, it.yOffset * density), it.radius * density) }
                val lineText = text.subSequence(line.start, line.end)
                val fill = range.paint.fill
                val measured = measure(lineText)
                val topLeft = Offset(line.bounds.left, layout.getLineBaseline(line.index) - measured.firstBaseline)
                val fillBounds = bounds.translate(-topLeft)
                when (fill) {
                    is SevenTVPaint.Fill.Image -> {
                        // A layout only ever draws with a color or a brush, its paint would keep a stale shader otherwise
                        PaintedSegment(line.bounds, topLeft, measured, measure(lineText), shadow, brush = null, PaintedImage(fill.url, fillBounds))
                    }

                    else -> {
                        val brush = fill.toBrush(fillBounds, range.userColor) ?: return@mapNotNull null
                        PaintedSegment(line.bounds, topLeft, under = null, measured, shadow, brush, image = null)
                    }
                }
            }
        }
    }

    /** Draws what replaces the parts of the message that are cut out by [drawTextWithoutSegments]. */
    fun DrawScope.drawSegments(segments: List<PaintedSegment>) {
        drawTick
        segments.forEach { segment ->
            segment.under?.let { drawText(it, topLeft = segment.topLeft, shadow = segment.shadow) }
            when {
                segment.brush != null -> {
                    drawText(segment.over, segment.brush, topLeft = segment.topLeft, shadow = segment.shadow)
                }

                segment.image != null -> {
                    imageBrush(segment.image)?.let { drawText(segment.over, it, topLeft = segment.topLeft) }
                }
            }
        }
    }

    private fun imageBrush(image: PaintedImage): Brush? {
        val drawable = images[image.url] ?: return null
        return when {
            drawable is ClockedAnimationDrawable -> image.brushFor(drawable.frame)
            drawable.bounds.isEmpty -> null
            else -> image.brushFor(drawable.toBitmap())
        }
    }
}

/** A line of a painted range, in the coordinates of the message layout. */
private class PaintedLine(
    val index: Int,
    val start: Int,
    val end: Int,
    val bounds: Rect,
)

internal class PaintedSegment(
    val bounds: Rect,
    val topLeft: Offset,
    /** Shows the color of the user through the transparent parts of an image. */
    val under: TextLayoutResult?,
    val over: TextLayoutResult,
    val shadow: Shadow?,
    val brush: Brush?,
    val image: PaintedImage?,
)

internal class PaintedImage(
    val url: String,
    /** The area the image is stretched over, in the coordinates of its segment. */
    private val bounds: Rect,
) {
    private var brush: Brush? = null
    private var bitmap: Bitmap? = null
    private var generationId = 0

    // A shader keeps showing the pixels it was created with, a new frame needs a new one
    fun brushFor(frame: Bitmap): Brush {
        val current = brush
        if (current != null && frame === bitmap && frame.generationId == generationId) {
            return current
        }

        val matrix =
            Matrix().apply {
                setScale(bounds.width / frame.width, bounds.height / frame.height)
                postTranslate(bounds.left, bounds.top)
            }
        val shader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(matrix) }
        bitmap = frame
        generationId = frame.generationId
        return ShaderBrush(shader).also { brush = it }
    }
}

internal fun DrawScope.drawTextWithoutSegments(
    layout: TextLayoutResult,
    segments: List<PaintedSegment>,
) {
    if (segments.isEmpty()) {
        drawText(layout)
        return
    }

    val canvas = drawContext.canvas
    canvas.save()
    segments.forEach { canvas.clipRect(it.bounds, ClipOp.Difference) }
    drawText(layout)
    canvas.restore()
}

// The colon and the space after a name are not part of what gets painted
private fun AnnotatedString.paintedEnd(
    start: Int,
    end: Int,
): Int {
    var last = end - 1
    while (last > start && (this[last] == ' ' || this[last] == ':')) {
        last--
    }
    return last + 1
}

private fun TextLayoutResult.paintedLines(
    start: Int,
    end: Int,
): List<PaintedLine> {
    if (end <= start) {
        return emptyList()
    }

    return (getLineForOffset(start)..getLineForOffset(end - 1)).mapNotNull { line ->
        val lineStart = max(start, getLineStart(line))
        val lineEnd = min(end, getLineEnd(line, visibleEnd = true))
        if (lineEnd <= lineStart) {
            return@mapNotNull null
        }

        val first = getBoundingBox(lineStart)
        val last = getBoundingBox(lineEnd - 1)
        val bounds =
            Rect(
                left = minOf(first.left, last.left),
                top = minOf(first.top, last.top),
                right = maxOf(first.right, last.right),
                bottom = maxOf(first.bottom, last.bottom),
            )
        PaintedLine(line, lineStart, lineEnd, bounds).takeUnless { bounds.isEmpty }
    }
}

private fun Rect.union(other: Rect) =
    Rect(
        left = min(left, other.left),
        top = min(top, other.top),
        right = max(right, other.right),
        bottom = max(bottom, other.bottom),
    )

private fun SevenTVPaint.Fill.toBrush(
    bounds: Rect,
    userColor: Color,
): Brush? = when (this) {
    is SevenTVPaint.Fill.Linear -> linearBrush(this, bounds, userColor)
    is SevenTVPaint.Fill.Radial -> radialBrush(this, bounds, userColor)
    is SevenTVPaint.Fill.Image -> null
}

// A plain color would lose against the color spans of the text, a shader does not
private fun singleColorBrush(color: Color) = Brush.linearGradient(listOf(color, color))

private fun linearBrush(
    fill: SevenTVPaint.Fill.Linear,
    bounds: Rect,
    userColor: Color,
): Brush? {
    val stops = fill.stops.toColorStops(fill.repeat, userColor) ?: return null
    if (stops.size == 1) {
        return singleColorBrush(stops.single().second)
    }

    // The CSS gradient line: it runs through the center and is long enough for the corners to get the first and last color
    val radians = Math.toRadians(fill.angle.toDouble())
    val directionX = sin(radians).toFloat()
    val directionY = -cos(radians).toFloat()
    val halfLength = (abs(bounds.width * directionX) + abs(bounds.height * directionY)) / 2
    val center = bounds.center
    val lineStart = Offset(center.x - directionX * halfLength, center.y - directionY * halfLength)
    val lineEnd = Offset(center.x + directionX * halfLength, center.y + directionY * halfLength)

    return when {
        fill.repeat -> {
            val first = fill.stops.first().position
            val last = fill.stops.last().position
            Brush.linearGradient(
                *stops,
                start = lerp(lineStart, lineEnd, first),
                end = lerp(lineStart, lineEnd, last),
                tileMode = TileMode.Repeated,
            )
        }

        else -> {
            Brush.linearGradient(*stops, start = lineStart, end = lineEnd, tileMode = TileMode.Clamp)
        }
    }
}

private fun radialBrush(
    fill: SevenTVPaint.Fill.Radial,
    bounds: Rect,
    userColor: Color,
): Brush? {
    val stops = fill.stops.toColorStops(fill.repeat, userColor) ?: return null
    if (stops.size == 1) {
        return singleColorBrush(stops.single().second)
    }

    val radius = max(bounds.width, bounds.height) / 2
    return Brush.radialGradient(
        *stops,
        center = bounds.center,
        radius =
            when {
                fill.repeat -> radius * fill.stops.last().position
                else -> radius
            }.coerceAtLeast(1f),
        tileMode =
            when {
                fill.repeat -> TileMode.Repeated
                else -> TileMode.Clamp
            },
    )
}

/** Translucent stops show the color of the user beneath, repeating gradients only span their stops. */
internal fun List<SevenTVPaint.Stop>.toColorStops(
    repeat: Boolean,
    userColor: Color,
): Array<Pair<Float, Color>>? {
    if (isEmpty()) {
        return null
    }

    val first = first().position
    val range = last().position - first
    var previous = 0f
    return Array(size) { index ->
        val stop = this[index]
        val position =
            when {
                repeat && range > 0f -> (stop.position - first) / range
                else -> stop.position
            }
        // Android requires ascending stops
        previous = max(previous, position.coerceIn(0f, 1f))
        previous to Color(stop.color).compositeOver(userColor)
    }
}
