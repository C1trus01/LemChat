package com.flxrs.dankchat.data.repo.emote

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SevenTVPaintDto
import com.flxrs.dankchat.data.api.seventv.eventapi.dto.SevenTVPaintShadowDto
import kotlin.math.hypot

@Immutable
data class SevenTVPaint(
    val id: String,
    val fill: Fill,
    val shadow: Shadow?,
) {
    @Immutable
    sealed interface Fill {
        data class Linear(
            val angle: Float,
            val repeat: Boolean,
            val stops: List<Stop>,
        ) : Fill

        data class Radial(
            val repeat: Boolean,
            val stops: List<Stop>,
        ) : Fill

        data class Image(
            val url: String,
        ) : Fill
    }

    data class Stop(
        val position: Float,
        val color: Int,
    )

    data class Shadow(
        val xOffset: Float,
        val yOffset: Float,
        val radius: Float,
        val color: Int,
    )
}

fun SevenTVPaintDto.toPaint(): SevenTVPaint? {
    val stops =
        stops.map { stop ->
            SevenTVPaint.Stop(position = stop.at, color = stop.color.rgbaToArgb())
        }
    val fill =
        when (function) {
            "LINEAR_GRADIENT", "linear-gradient" -> SevenTVPaint.Fill.Linear(angle, repeat, stops)
            "RADIAL_GRADIENT", "radial-gradient" -> SevenTVPaint.Fill.Radial(repeat, stops)
            "URL", "url" -> imageUrl?.takeIf { it.isNotBlank() }?.let { SevenTVPaint.Fill.Image(it) }
            else -> null
        } ?: return null

    return SevenTVPaint(id = id, fill = fill, shadow = shadows.toSingleShadow())
}

// A text span can only draw one shadow. Paints commonly outline the name with shadows offset to each side, which averages to a centered blur.
private fun List<SevenTVPaintShadowDto>.toSingleShadow(): SevenTVPaint.Shadow? {
    if (isEmpty()) {
        return null
    }

    return SevenTVPaint.Shadow(
        xOffset = map { it.xOffset }.average().toFloat(),
        yOffset = map { it.yOffset }.average().toFloat(),
        radius = maxOf { it.radius + hypot(it.xOffset, it.yOffset) },
        color = maxBy { it.color and 0xFF }.color.rgbaToArgb(),
    )
}

// 7TV packs colors as 0xRRGGBBAA
internal fun Long.rgbaToArgb(): Int {
    val rgba = toInt()
    return (rgba ushr 8) or (rgba shl 24)
}
