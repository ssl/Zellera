package zel.game.camera

import zel.util.NANOS_PER_MILLI
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class PhotoRenderer(
    private val assets: CameraAssets,
    private val deadlineMs: Long = DEADLINE_MS,
    private val externalImage: (String) -> BufferedImage?,
) {
    data class Size(
        val width: Int,
        val height: Int,
    )

    private class Canvas(
        val width: Int,
        val height: Int,
    ) {
        val pixels = IntArray(width * height) { PhotoPixels.OPAQUE }

        /** The middle half of the picture, doubled. */
        fun zoomedIn(): IntArray {
            val left = width / 4
            val top = height / 4
            return IntArray(pixels.size) { i -> pixels[(top + i / width / 2) * width + left + i % width / 2] }
        }
    }

    private inner class Job(
        furni: Set<String>?,
    ) {
        private val deadline = System.nanoTime() + deadlineMs * NANOS_PER_MILLI
        private val sprites = HashMap<String, AssetBundle.Image?>()
        private val extents = HashMap<String, AssetBundle.Extent?>()
        private val pictures = HashMap<String, BufferedImage?>()
        val fetches = assets.frameFetches(furni)
        private var work = 0L
        private var urlSprites = 0

        fun spend(pixels: Long) {
            work += pixels
            require(work <= MAX_WORK) { "frame too costly to draw" }
            check()
        }

        fun check() {
            require(System.nanoTime() - deadline <= 0) { "frame took too long" }
        }

        fun extent(name: String): AssetBundle.Extent? {
            if (extents.containsKey(name)) return extents[name]
            if (extents.size >= MAX_ASSET_NAMES) return null
            check()
            return assets.spriteExtent(name, fetches).also { extents[name] = it }
        }

        fun sprite(name: String): AssetBundle.Image? {
            if (sprites.containsKey(name)) return sprites[name]
            if (sprites.size >= MAX_ASSET_NAMES) return null
            check()
            return assets.sprite(name, fetches).also { sprites[name] = it }
        }

        fun sprite(
            name: String,
            palette: String?,
        ): AssetBundle.Image? {
            if (palette == null) return sprite(name)
            val key = "$name@$palette"
            if (sprites.containsKey(key)) return sprites[key]
            val plain = sprite(name)
            if (plain == null || sprites.size >= MAX_ASSET_NAMES) return plain
            check()
            val colours = assets.palette(name, palette, fetches)
            val found = if (colours == null) plain else AssetBundle.Image(paletted(plain.image, colours), plain.flipH)
            sprites[key] = found
            return found
        }

        fun picture(url: String): BufferedImage? {
            urlSprites++
            if (urlSprites > MAX_URL_SPRITES) return null
            if (pictures.containsKey(url)) return pictures[url]
            check()
            return externalImage(url).also { pictures[url] = it }
        }
    }

    fun render(
        frame: PhotoFrame,
        furni: Set<String>? = null,
    ): BufferedImage {
        val job = Job(furni)
        val (width, height) = size(frame)
        val canvas = Canvas(width, height)
        val layers: List<Pair<Double, () -> Unit>> =
            frame.planes.map { plane -> plane.z to { drawPlane(canvas, plane, job) } } +
                frame.sprites.map { sprite -> sprite.z to { drawSprite(canvas, sprite, job) } }
        layers.sortedByDescending { it.first }.forEach { (_, draw) -> draw() }
        for (use in frame.filters) {
            job.spend(canvas.pixels.size.toLong())
            applyFilter(canvas, use)
        }
        val pixels = if (frame.zoom == 2) canvas.zoomedIn() else canvas.pixels
        return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
            setRGB(0, 0, width, height, pixels, 0, width)
        }
    }

    private fun drawPlane(
        canvas: Canvas,
        plane: PhotoFrame.Plane,
        job: Job,
    ) {
        val surface = PlaneSurface.of(plane) ?: return
        val textures = plane.textures.mapNotNull { assets.roomAsset(it, job.fetches)?.image }
        val masks =
            plane.masks.mapNotNull { mask ->
                assets.roomAsset(mask.name, job.fetches)?.let {
                    PlaneSurface.PlacedMask(it.image, mask.x, mask.y, mask.flipH xor it.flipH, mask.flipV)
                }
            }
        val xs = plane.corners.map { it.x }
        val ys = plane.corners.map { it.y }
        val x0 = floor(xs.min()).toInt().coerceIn(0, canvas.width)
        val x1 = ceil(xs.max()).toInt().coerceIn(0, canvas.width)
        val y0 = floor(ys.min()).toInt().coerceIn(0, canvas.height)
        val y1 = ceil(ys.max()).toInt().coerceIn(0, canvas.height)
        job.spend((x1 - x0).toLong() * max(0, y1 - y0) * (1 + masks.size))
        for (y in y0 until y1) {
            job.check()
            for (x in x0 until x1) {
                val rgb =
                    if (surface.covers(x, y) && !surface.hasHole(x, y, masks)) {
                        surface.colorAt(x, y, textures)
                    } else {
                        PlaneSurface.SEE_THROUGH
                    }
                if (rgb != PlaneSurface.SEE_THROUGH) canvas.pixels[y * canvas.width + x] = PhotoPixels.OPAQUE or rgb
            }
        }
    }

    private fun drawSprite(
        canvas: Canvas,
        sprite: PhotoFrame.Sprite,
        job: Job,
    ) {
        val isPicture = sprite.url || (sprite.name == BOX && sprite.width > 0 && sprite.height > 0)
        val left = floor(sprite.x).toInt()
        val top = floor(sprite.y).toInt()
        if (!isPicture && !showsOn(canvas, left, top, sprite.name, job)) return
        val image = spriteImage(sprite, isPicture, job) ?: return
        val sx0 = max(0, -left)
        val sx1 = min(image.width, canvas.width - left)
        val sy0 = max(0, -top)
        val sy1 = min(image.height, canvas.height - top)
        if (sx0 >= sx1 || sy0 >= sy1) return
        job.spend((sx1 - sx0).toLong() * (sy1 - sy0))
        val blend = BlendMode.ofSprite(sprite.blendMode)
        val alpha = sprite.alpha / PhotoPixels.MAX_LEVEL
        val tint = if (isPicture || sprite.color < 0) NO_TINT else sprite.color
        val isFigure = !isPicture && CameraAssets.figurePart(sprite.name)
        for (sy in sy0 until sy1) {
            job.check()
            for (sx in sx0 until sx1) {
                val src = image.getRGB(if (sprite.flipH) image.width - 1 - sx else sx, sy)
                val a = src ushr PhotoPixels.ALPHA_SHIFT
                if (a != 0) {
                    val index = (top + sy) * canvas.width + left + sx
                    canvas.pixels[index] =
                        PhotoPixels.blend(
                            canvas.pixels[index],
                            tinted(src, tint, isFigure),
                            a / PhotoPixels.MAX_LEVEL * alpha,
                            blend,
                        )
                }
            }
        }
    }

    private fun showsOn(
        canvas: Canvas,
        left: Int,
        top: Int,
        name: String,
        job: Job,
    ): Boolean {
        if (left >= canvas.width || top >= canvas.height) return false
        val extent = job.extent(name) ?: return false
        return left + extent.width > 0 && top + extent.height > 0
    }

    private fun spriteImage(
        sprite: PhotoFrame.Sprite,
        isPicture: Boolean,
        job: Job,
    ): BufferedImage? {
        if (!isPicture) return job.sprite(sprite.name, sprite.palette)?.image
        job.spend(sprite.width.toLong() * sprite.height)
        return wallPicture(sprite, if (sprite.url) job.picture(sprite.name) else null)
    }

    private fun tinted(
        src: Int,
        tint: Int,
        isFigure: Boolean,
    ): Int =
        when {
            tint == NO_TINT -> src
            isFigure -> (src and PhotoPixels.OPAQUE) or PhotoPixels.multiplyFigure(src, tint)
            else -> (src and PhotoPixels.OPAQUE) or PhotoPixels.multiply(src, tint)
        }

    private fun applyFilter(
        canvas: Canvas,
        use: PhotoFrame.FilterUse,
    ) {
        val filter = PhotoFilters[use.name] ?: return
        val strength = use.alpha / PhotoPixels.MAX_LEVEL
        when (filter) {
            is PhotoFilter.Matrix -> {
                val weighted =
                    DoubleArray(filter.values.size) { i ->
                        filter.values[i] * strength + IDENTITY_MATRIX[i] * (1 - strength)
                    }
                val pixels = canvas.pixels
                for (i in pixels.indices) pixels[i] = PhotoPixels.colorMatrix(pixels[i], weighted)
            }

            is PhotoFilter.Overlay -> {
                assets.filterImage(use.name)?.let { overlay(canvas, it, strength, filter.blend) }
            }

            PhotoFilter.Border -> {
                assets.filterImage(use.name)?.let { overlay(canvas, it, 1.0, BlendMode.NORMAL) }
            }
        }
    }

    private fun overlay(
        canvas: Canvas,
        image: BufferedImage,
        alpha: Double,
        blend: BlendMode,
    ) {
        for (y in 0 until min(canvas.height, image.height)) {
            for (x in 0 until min(canvas.width, image.width)) {
                val src = image.getRGB(x, y)
                val a = src ushr PhotoPixels.ALPHA_SHIFT
                if (a != 0) {
                    val index = y * canvas.width + x
                    canvas.pixels[index] =
                        PhotoPixels.blend(canvas.pixels[index], src, a / PhotoPixels.MAX_LEVEL * alpha, blend)
                }
            }
        }
    }

    companion object {
        const val MAX_SIZE: Int = 640

        /** 256 photo canvases (320 x 320): a crowded room's frame needs a few dozen. */
        internal const val MAX_WORK: Long = 256L * 320 * 320

        /** Asset loading included. */
        internal const val DEADLINE_MS: Long = 5000

        internal const val MAX_URL_SPRITES: Int = 30

        /** Distinct asset names one frame may look up; a crowded room's frame names a few hundred. */
        internal const val MAX_ASSET_NAMES: Int = 1024

        private const val BOX = "box"
        private const val NO_TINT = -1
        private const val DEFAULT_SIDE = 320
        private const val MIN_SIDE = 16
        private val IDENTITY_MATRIX = "1 0 0 0 0 0 1 0 0 0 0 0 1 0 0 0 0 0 1 0".split(' ').map(String::toDouble)

        /** The first plane's extent, else the default side; clamped to 16..640. */
        internal fun size(frame: PhotoFrame): Size {
            val corners = frame.planes.firstOrNull()?.corners
            val width = if (corners == null) DEFAULT_SIDE else side(corners.map { it.x })
            val height = if (corners == null) DEFAULT_SIDE else side(corners.map { it.y })
            return Size(width.coerceIn(MIN_SIDE, MAX_SIZE), height.coerceIn(MIN_SIDE, MAX_SIZE))
        }

        private fun side(positions: List<Double>): Int = max(0.0, positions.max()).roundToInt()

        internal fun paletted(
            image: BufferedImage,
            palette: IntArray,
        ): BufferedImage {
            val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val argb = image.getRGB(x, y)
                    val colour = palette[argb shr 8 and 0xFF] and PhotoPixels.RGB
                    out.setRGB(x, y, (argb and PhotoPixels.OPAQUE) or colour)
                }
            }
            return out
        }

        internal fun wallPicture(
            sprite: PhotoFrame.Sprite,
            picture: BufferedImage?,
        ): BufferedImage? {
            val w = sprite.width
            val h = sprite.height
            if (w <= 0 || h <= 0) return picture
            val slope = abs(sprite.skew)
            val side = max(1, h - ((w - 1) * slope).roundToInt())
            val flat = flatPicture(sprite, picture, side)
            if (slope == 0.0) return flat
            val sheared = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            for (x in 0 until w) {
                val dy = ((if (sprite.skew < 0) x else w - 1 - x) * slope).roundToInt()
                for (y in 0 until min(side, h - dy)) sheared.setRGB(x, y + dy, flat.getRGB(x, y))
            }
            return sheared
        }

        private fun flatPicture(
            sprite: PhotoFrame.Sprite,
            picture: BufferedImage?,
            side: Int,
        ): BufferedImage {
            val w = sprite.width
            val flat = BufferedImage(w, side, BufferedImage.TYPE_INT_ARGB)
            val g = flat.createGraphics()
            val inset = if (sprite.frame) 1 else 0
            if (sprite.frame) {
                g.color = Color.BLACK
                g.fillRect(0, 0, w, side)
            }
            if (picture != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g.drawImage(picture, inset, inset, w - 2 * inset, side - 2 * inset, null)
            } else if (sprite.color >= 0) {
                g.color = Color(sprite.color)
                g.fillRect(inset, inset, w - 2 * inset, side - 2 * inset)
            }
            g.dispose()
            return flat
        }
    }
}
