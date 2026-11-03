package com.flxrs.dankchat.utils.extensions

import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import kotlin.math.sin

/** Parses an IRC color tag value, returning null for empty or malformed values instead of throwing. */
@ColorInt
fun String.parseColorOrNull(): Int? = runCatching { toColorInt() }.getOrNull()

/**
 * Adjusts this color to ensure readable contrast against [background].
 *
 * Two-phase approach matching Chatterino2's color pipeline:
 * 1. Hue-specific HSL correction (clamp lightness, adjust greens on light / blues on dark)
 * 2. Contrast-based binary search (3.5:1 target) for any remaining contrast issues
 */
@ColorInt
fun Int.normalizeColor(
    @ColorInt background: Int,
): Int {
    val cacheKey = (toLong() shl 32) or (background.toLong() and 0xFFFFFFFFL)
    normalizedColorCache[cacheKey]?.let { return it }
    return computeNormalizedColor(background).also { normalizedColorCache.put(cacheKey, it) }
}

@ColorInt
private fun Int.computeNormalizedColor(
    @ColorInt background: Int,
): Int {
    // Phase 1: hue-specific correction (matches C2 / old DankChat)
    val opaqueColor = correctColor(this or 0xFF000000.toInt(), background or 0xFF000000.toInt())
    val opaqueBackground = background or 0xFF000000.toInt()
    val contrast = ColorUtils.calculateContrast(opaqueColor, opaqueBackground)
    if (contrast >= MIN_CONTRAST_RATIO) return opaqueColor

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(opaqueColor, hsl)

    val bgLuminance = ColorUtils.calculateLuminance(opaqueBackground)
    // On dark backgrounds, increase lightness; on light backgrounds, decrease it
    val shouldLighten = bgLuminance < 0.5

    // Binary search for the minimum lightness adjustment that meets contrast.
    var low: Float
    var high: Float
    if (shouldLighten) {
        low = hsl[2] // original lightness
        high = 1f // max lightness
    } else {
        low = 0f // min lightness
        high = hsl[2] // original lightness
    }

    var bestL = hsl[2]
    var bestContrast = contrast

    repeat(MAX_ITERATIONS) {
        val mid = (low + high) / 2f
        hsl[2] = mid
        val candidate = ColorUtils.HSLToColor(hsl)
        val candidateContrast = ColorUtils.calculateContrast(candidate, opaqueBackground)

        if (candidateContrast >= MIN_CONTRAST_RATIO) {
            bestL = mid
            bestContrast = candidateContrast
            // Try closer to original (less adjustment)
            if (shouldLighten) high = mid else low = mid
        } else {
            if (candidateContrast > bestContrast) {
                bestL = mid
                bestContrast = candidateContrast
            }
            // Need more adjustment (further from original)
            if (shouldLighten) low = mid else high = mid
        }
    }

    hsl[2] = bestL
    return ColorUtils.HSLToColor(hsl)
}

/**
 * Hue-specific HSL lightness correction matching Chatterino2's algorithm.
 * On light backgrounds: clamps lightness to 0.5, darkens greens (hue 0.1–0.33).
 * On dark backgrounds: clamps lightness to 0.5, lightens blues (hue 0.54–0.83).
 */
@ColorInt
private fun correctColor(
    @ColorInt color: Int,
    @ColorInt background: Int,
): Int {
    val isLightBackground = ColorUtils.calculateLuminance(background) > 0.5
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color, hsl)
    val hue = hsl[0] / 360f

    when {
        isLightBackground -> {
            if (hsl[2] > 0.5f) {
                hsl[2] = 0.5f
            }
            if (hsl[2] > 0.4f && hue > 0.1f && hue < 0.33333f) {
                hsl[2] -= (sin((hue - 0.1f) / (0.33333f - 0.1f) * Math.PI.toFloat()) * hsl[1] * 0.4f)
            }
        }

        else -> {
            if (hsl[2] < 0.5f) {
                hsl[2] = 0.5f
            }
            if (hsl[2] < 0.6f && hue > 0.54444f && hue < 0.83333f) {
                hsl[2] += (sin((hue - 0.54444f) / (0.83333f - 0.54444f) * Math.PI.toFloat()) * hsl[1] * 0.4f)
            }
        }
    }

    return ColorUtils.HSLToColor(hsl)
}

private const val MIN_CONTRAST_RATIO = 3.5
private const val MAX_ITERATIONS = 16

private val normalizedColorCache = LruCache<Long, Int>(256)

/** convert int to RGB with zero pad */
val Int.hexCode: String
    get() = Integer.toHexString(rgb).padStart(6, '0')

/** helper to extract only RGB part (i.e. drop the alpha part) */
private val Int.rgb: Int
    get() = this and 0xffffff
