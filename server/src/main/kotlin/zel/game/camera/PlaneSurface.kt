package zel.game.camera

import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

internal class PlaneSurface private constructor(
    private val plane: PhotoFrame.Plane,
    private val height: Double,
    private val surface: Basis,
    private val texture: Basis,
    private val outline: Outline,
    private val texels: Texels?,
) {
    private val origin = plane.corners[ORIGIN]

    private class Basis(
        val ux: Double,
        val uy: Double,
        val vx: Double,
        val vy: Double,
    ) {
        val det = ux * vy - uy * vx
    }

    class PlacedMask(
        private val image: BufferedImage,
        private val x: Double,
        private val y: Double,
        private val flipH: Boolean,
        private val flipV: Boolean,
    ) {
        /** The mask is cut 1 px left and 2 higher than sent. */
        fun cuts(
            u: Double,
            v: Double,
        ): Boolean {
            val mx = floor(u - x + 1).toInt()
            val my = floor(v - y + 2).toInt()
            if (mx < 0 || my < 0 || mx >= image.width || my >= image.height) return false
            val column = if (flipH) image.width - 1 - mx else mx
            val row = if (flipV) image.height - 1 - my else my
            return image.getRGB(column, row) ushr PhotoPixels.ALPHA_SHIFT > 0
        }
    }

    fun covers(
        x: Int,
        y: Int,
    ): Boolean = outline.contains(x + COVER_X, y + COVER_Y)

    fun hasHole(
        x: Int,
        y: Int,
        masks: List<PlacedMask>,
    ): Boolean {
        val dx = x - origin.x
        val dy = y - origin.y
        val u = (dx * surface.vy - dy * surface.vx) / surface.det
        val v = (surface.ux * dy - surface.uy * dx) / surface.det
        return masks.any { it.cuts(u, v) }
    }

    fun colorAt(
        x: Int,
        y: Int,
        textures: List<BufferedImage>,
    ): Int {
        if (textures.isEmpty()) return plane.color
        val su = textureU(x, y)
        val column =
            if (textures.size > 1) floor(su / textures[0].width).toInt().mod(textures.size) else 0
        val image = textures[column]
        val sv = textureV(x, y)
        val tx = floor(su).toInt().mod(image.width)
        val ty =
            if (plane.bottomAligned) {
                (image.height - 1 - floor(height - sv).toInt()).mod(image.height)
            } else {
                floor(sv).toInt().mod(image.height)
            }
        val texel = image.getRGB(tx, ty)
        return if (texel ushr PhotoPixels.ALPHA_SHIFT == 0) SEE_THROUGH else PhotoPixels.multiply(texel, plane.color)
    }

    private fun textureU(
        x: Int,
        y: Int,
    ): Double {
        if (texels != null) return texels.u(x - origin.x.toLong(), y - origin.y.toLong()) + HALF_TEXEL
        val sx = x + TEXTURE_X - origin.x
        val sy = y + TEXTURE_Y - origin.y
        return (sx * texture.vy - sy * texture.vx) / texture.det
    }

    private fun textureV(
        x: Int,
        y: Int,
    ): Double {
        if (texels != null) return texels.v(x - origin.x.toLong(), y - origin.y.toLong()) + HALF_TEXEL
        val sx = x + TEXTURE_X - origin.x
        val sy = y + TEXTURE_Y - origin.y
        return (texture.ux * sy - texture.uy * sx) / texture.det
    }

    /** Texel by whole pixels from the plane's origin corner; the px part truncates toward zero rather than flooring. */
    private class Texels(
        private val ua: Long,
        private val ub: Double,
        private val va: Long,
        private val vb: Double,
    ) {
        fun u(
            px: Long,
            py: Long,
        ): Long = py * ua + (px * ub).toLong()

        fun v(
            px: Long,
            py: Long,
        ): Long = py * va + (px * vb).toLong()

        companion object {
            fun of(
                origin: PhotoFrame.Point,
                basis: Basis,
            ): Texels? {
                if (origin.x != Math.rint(origin.x) || origin.y != Math.rint(origin.y)) return null
                val ua = -basis.vx / basis.det
                val ub = basis.vy / basis.det
                val va = basis.ux / basis.det
                val vb = -basis.uy / basis.det
                if (!isWhole(ua) || !isWhole(va) || !isWhole(2 * ub) || !isWhole(2 * vb)) return null
                return Texels(Math.round(ua), ub, Math.round(va), vb)
            }

            private fun isWhole(value: Double): Boolean = abs(value - Math.rint(value)) < EPSILON
        }
    }

    /** Edges in order round the plane (corners 3, 2, 0, 1); a point on an edge is inside. */
    private class Outline(
        corners: List<PhotoFrame.Point>,
    ) {
        private val px = DoubleArray(corners.size)
        private val py = DoubleArray(corners.size)
        private val dx = DoubleArray(corners.size)
        private val dy = DoubleArray(corners.size)

        init {
            corners.forEachIndexed { i, a ->
                val b = corners[(i + 1) % corners.size]
                val edge = isoEdge(b.x - a.x, b.y - a.y)
                val anchor = if (lowerEnd(a, b, edge)) b else a
                px[i] = anchor.x
                py[i] = anchor.y
                dx[i] = edge[0]
                dy[i] = edge[1]
            }
        }

        private fun lowerEnd(
            a: PhotoFrame.Point,
            b: PhotoFrame.Point,
            edge: DoubleArray,
        ): Boolean {
            if (edge[0] == 0.0) return false
            val slope = edge[1] / edge[0]
            return b.y - slope * b.x > a.y - slope * a.x
        }

        fun contains(
            x: Double,
            y: Double,
        ): Boolean {
            var left = true
            var right = true
            for (i in px.indices) {
                val side = dx[i] * (y - py[i]) - dy[i] * (x - px[i])
                if (side < 0) left = false
                if (side > 0) right = false
            }
            return left || right
        }
    }

    companion object {
        const val SEE_THROUGH: Int = -1

        private const val ORIGIN = 3
        private const val RIGHT = 2
        private const val DOWN = 1
        private const val FAR = 0
        private const val MIN_SIDE = 0.5
        private const val EPSILON = 1e-9
        private const val COVER_X = 0.5
        private const val COVER_Y = 0.125
        private const val TEXTURE_X = 0.5
        private const val TEXTURE_Y = 0.5
        private const val HALF_TEXEL = 0.5

        fun of(plane: PhotoFrame.Plane): PlaneSurface? {
            val c = plane.corners
            val origin = c[ORIGIN]
            val right = c[RIGHT]
            val down = c[DOWN]
            val w = abs(right.x - origin.x)
            val h = if (down.x == origin.x) abs(down.y - origin.y) else abs(down.x - origin.x)
            if (w < MIN_SIDE || h < MIN_SIDE) return null
            val surface =
                Basis(
                    (right.x - origin.x) / w,
                    (right.y - origin.y) / w,
                    (down.x - origin.x) / h,
                    (down.y - origin.y) / h,
                )
            if (abs(surface.det) < EPSILON) return null
            // the texture follows the true edges: corners arrive truncated, so an edge can be a pixel off 2:1
            val tu = isoEdge(right.x - origin.x, right.y - origin.y)
            val tv = isoEdge(down.x - origin.x, down.y - origin.y)
            val texture = Basis(tu[0] / w, tu[1] / w, tv[0] / h, tv[1] / h)
            if (abs(texture.det) < EPSILON) return null
            val outline = Outline(listOf(c[ORIGIN], c[RIGHT], c[FAR], c[DOWN]))
            return PlaneSurface(plane, h, surface, texture, outline, Texels.of(origin, texture))
        }

        /** Within a pixel of 2:1 it becomes exactly 2:1, an upright edge stays upright, anything else is left. */
        fun isoEdge(
            dx: Double,
            dy: Double,
        ): DoubleArray =
            when {
                abs(dx) < MIN_SIDE -> doubleArrayOf(0.0, dy)
                abs(abs(dy) - abs(dx) / 2) <= 1 -> doubleArrayOf(dx, sign(dy) * abs(dx) / 2)
                else -> doubleArrayOf(dx, dy)
            }
    }
}
