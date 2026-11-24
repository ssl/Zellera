package zel.game.camera

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal object PhotoPixels {
    const val OPAQUE: Int = 0xFF000000.toInt()
    const val RGB: Int = 0xFFFFFF
    const val ALPHA_SHIFT: Int = 24
    const val MAX_LEVEL: Double = 255.0

    private const val FIGURE_WHITE = 254
    private const val HALF = 0.5
    private val CHANNEL_SHIFTS = intArrayOf(16, 8, 0)

    fun multiply(
        rgb: Int,
        tint: Int,
    ): Int {
        val r = (rgb shr 16 and 0xFF) * (tint shr 16 and 0xFF) / 0xFF
        val g = (rgb shr 8 and 0xFF) * (tint shr 8 and 0xFF) / 0xFF
        val b = (rgb and 0xFF) * (tint and 0xFF) / 0xFF
        return (r shl 16) or (g shl 8) or b
    }

    /** Figure libraries store full white as 254, so 254 counts as 255 before [multiply], rounded down. */
    fun multiplyFigure(
        rgb: Int,
        tint: Int,
    ): Int = multiply((figureLevel(rgb shr 16) shl 16) or (figureLevel(rgb shr 8) shl 8) or figureLevel(rgb), tint)

    private fun figureLevel(shifted: Int): Int {
        val level = shifted and 0xFF
        return if (level == FIGURE_WHITE) 0xFF else level
    }

    fun colorMatrix(
        argb: Int,
        m: DoubleArray,
    ): Int {
        val r = argb shr 16 and 0xFF
        val g = argb shr 8 and 0xFF
        val b = argb and 0xFF
        val a = argb ushr 24
        var i = 0
        var out = OPAQUE
        for (shift in CHANNEL_SHIFTS) {
            val value = m[i] * r + m[i + 1] * g + m[i + 2] * b + m[i + 3] * a + m[i + 4]
            out = out or (level(value) shl shift)
            i += MATRIX_COLUMNS
        }
        return out
    }

    private const val MATRIX_COLUMNS = 5

    fun blend(
        dst: Int,
        src: Int,
        alpha: Double,
        mode: BlendMode,
    ): Int {
        if (alpha <= 0) return dst
        val strength = min(1.0, alpha)
        var out = OPAQUE
        for (shift in CHANNEL_SHIFTS) {
            val below = (dst shr shift and 0xFF) / MAX_LEVEL
            val above = (src shr shift and 0xFF) / MAX_LEVEL
            val mixed =
                if (mode == BlendMode.ADD) {
                    min(1.0, below + above * strength)
                } else {
                    below * (1 - strength) + mix(mode, below, above) * strength
                }
            out = out or (level(mixed * MAX_LEVEL) shl shift)
        }
        return out
    }

    private fun mix(
        mode: BlendMode,
        d: Double,
        s: Double,
    ): Double =
        when (mode) {
            BlendMode.NORMAL -> s
            BlendMode.ADD -> min(1.0, d + s)
            BlendMode.MULTIPLY -> d * s
            BlendMode.SCREEN -> 1 - (1 - d) * (1 - s)
            BlendMode.OVERLAY -> if (d < HALF) 2 * d * s else 1 - 2 * (1 - d) * (1 - s)
            BlendMode.HARDLIGHT -> if (s < HALF) 2 * d * s else 1 - 2 * (1 - d) * (1 - s)
            BlendMode.DARKEN -> min(d, s)
            BlendMode.LIGHTEN -> max(d, s)
            BlendMode.DIFFERENCE -> abs(d - s)
            BlendMode.SUBTRACT -> max(0.0, d - s)
        }

    /** Math.round's half-up, unlike kotlin.math.round's half-even. */
    private fun level(value: Double): Int = Math.round(value).toInt().coerceIn(0, 0xFF)
}
