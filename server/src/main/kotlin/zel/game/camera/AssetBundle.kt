package zel.game.camera

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import zel.content.ClientStrings
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.DataFormatException
import java.util.zip.Inflater
import javax.imageio.ImageIO
import kotlin.math.max

/** One asset bundle of the client: a library with `<name>.json` and a sheet PNG, or one PNG entry per asset. */
internal class AssetBundle private constructor(
    val name: String,
    private val assets: JsonObject,
    private val frames: JsonObject,
    private val sheetPng: ByteArray?,
    private val pngEntries: Map<String, ByteArray>,
    private val binaries: Map<String, ByteArray>,
    private val storedBytes: Long,
) {
    data class Image(
        val image: BufferedImage,
        val flipH: Boolean,
    )

    data class Extent(
        val width: Int,
        val height: Int,
    )

    // sheet, cache, imageBytes and greyShades are guarded by this object.
    private var sheet: BufferedImage? = null
    private val cache =
        object : LinkedHashMap<String, Image?>(INITIAL_CACHE_CAPACITY, CACHE_LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Image?>?): Boolean {
                if (size <= MAX_CACHED_IMAGES) return false
                imageBytes -= ownBytes(eldest?.value)
                return true
            }
        }
    private var imageBytes = 0L

    /** What the sheet takes once decoded, read from its PNG header: 4 bytes a pixel. */
    val sheetBytes: Long = sheetPng?.let(::pngExtent)?.let(::argbBytes) ?: 0L

    /** The most this bundle holds before its images are cut: its files, and its sheet decoded. */
    val plannedBytes: Long get() = storedBytes + sheetBytes

    /** Opaque grey pixels whose level is a key are drawn with the value's level instead. */
    var greyShades: Map<Int, Int> = emptyMap()
        @Synchronized set(value) {
            field = value.toMap()
            cache.clear()
            imageBytes = 0
        }

    /** The heap this bundle holds now: its files, its sheet once decoded and the images cut from it. */
    @Synchronized
    fun heldBytes(): Long = storedBytes + (if (sheet == null) 0 else sheetBytes) + imageBytes

    /** RGB triples, opaque, padded to 256 entries with 0; null when the bundle has no such entry. */
    fun palette(source: String): IntArray? =
        binaries[source]?.let { bytes ->
            IntArray(PALETTE_SIZE) { i ->
                val blue = i * RGB_BYTES + 2
                if (blue < bytes.size) {
                    PhotoPixels.OPAQUE or
                        (channel(bytes[blue - 2]) shl 16) or
                        (channel(bytes[blue - 1]) shl 8) or
                        channel(bytes[blue])
                } else {
                    0
                }
            }
        }

    fun has(asset: String): Boolean = assets.has(asset) || asset in pngEntries

    @Synchronized
    @Throws(IOException::class)
    fun image(asset: String): Image? {
        if (asset in cache) return cache[asset]
        val found = find(asset)
        // A name the bundle does not have is not remembered, so made-up names in a frame cannot fill the cache.
        if (found != null || has(asset)) {
            imageBytes += ownBytes(found)
            cache[asset] = found
        }
        return found
    }

    // a cut of the sheet shares its pixels; a restored, shaded or turned copy has its own
    private fun ownBytes(image: Image?): Long {
        val own = image?.image?.takeIf { it.raster.dataBuffer !== sheet?.raster?.dataBuffer } ?: return 0L
        return argbBytes(Extent(own.width, own.height))
    }

    /** The size [image] draws [asset] at, from the bundle's index alone: nothing is decoded. */
    fun extent(asset: String): Extent? {
        pngEntries[asset]?.let { return pngExtent(it) }
        val frame = sheetFrame(asset)?.first?.takeIf { sheetPng != null }
        val box = frame?.getAsJsonObject(if (isTrimmed(frame)) "sourceSize" else "frame")
        return box?.let { Extent(it["w"].asInt, it["h"].asInt) }
    }

    private fun find(asset: String): Image? = pngEntries[asset]?.let { Image(readPng(it), false) } ?: sheetImage(asset)

    /** The sheet frame [asset] is cut from, and whether it is drawn mirrored. */
    private fun sheetFrame(asset: String): Pair<JsonObject, Boolean>? {
        val alias = assets[asset]?.takeIf { it.isJsonObject }?.asJsonObject
        val source = alias?.get("source")?.asString ?: asset
        val flipH = alias?.get("flipH")?.asBoolean == true
        val frame = frame(source) ?: frame(asset).takeIf { source != asset }
        return frame?.let { it to flipH }
    }

    private fun sheetImage(asset: String): Image? {
        val (frame, flipH) = sheetFrame(asset) ?: return null
        val png = sheetPng ?: return null
        val cut = cropped(frame, sheet ?: readPng(png).also { sheet = it }) ?: return null
        val shadedCut = shaded(cut)
        val restored = if (isTrimmed(frame)) restoreTrim(frame, shadedCut) else shadedCut
        return restored?.let { Image(it, flipH) }
    }

    private fun frame(asset: String): JsonObject? =
        (frames["${name}_$asset"] ?: frames[asset])?.takeIf { it.isJsonObject }?.asJsonObject

    private fun cropped(
        frame: JsonObject,
        from: BufferedImage,
    ): BufferedImage? {
        val rect = frame.getAsJsonObject("frame")
        val rotated = frame["rotated"]?.asBoolean == true
        val x = rect["x"].asInt
        val y = rect["y"].asInt
        val w = rect["w"].asInt
        val h = rect["h"].asInt
        val sheetW = if (rotated) h else w
        val sheetH = if (rotated) w else h
        val inside = w > 0 && h > 0 && x >= 0 && y >= 0 && x + sheetW <= from.width && y + sheetH <= from.height
        if (!inside) return null
        val crop = from.getSubimage(x, y, sheetW, sheetH)
        return if (rotated) unrotate(crop) else crop
    }

    private fun isTrimmed(frame: JsonObject): Boolean =
        frame["trimmed"]?.asBoolean == true && frame.has("spriteSourceSize") && frame.has("sourceSize")

    /** A trimmed frame is the opaque part of the picture, put back in place: asset offsets are for the full size. */
    private fun restoreTrim(
        frame: JsonObject,
        crop: BufferedImage,
    ): BufferedImage? {
        val placed = frame.getAsJsonObject("spriteSourceSize")
        val full = frame.getAsJsonObject("sourceSize")
        val fullW = full["w"].asInt
        val fullH = full["h"].asInt
        if (fullW !in 1..MAX_RESTORED_SIDE || fullH !in 1..MAX_RESTORED_SIDE) return null
        return BufferedImage(fullW, fullH, BufferedImage.TYPE_INT_ARGB).also {
            it.createGraphics().apply {
                drawImage(crop, placed["x"].asInt, placed["y"].asInt, null)
                dispose()
            }
        }
    }

    private fun shaded(image: BufferedImage): BufferedImage {
        if (greyShades.isEmpty()) return image
        val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val argb = image.getRGB(x, y)
                val shade = greyShades[argb shr 16 and 0xFF]?.takeIf { isOpaqueGrey(argb) }
                val pixel = if (shade == null) argb else PhotoPixels.OPAQUE or (shade shl 16) or (shade shl 8) or shade
                out.setRGB(x, y, pixel)
            }
        }
        return out
    }

    private fun isOpaqueGrey(argb: Int): Boolean {
        val red = argb shr 16 and 0xFF
        return argb ushr 24 == 0xFF && red == (argb shr 8 and 0xFF) && red == (argb and 0xFF)
    }

    companion object {
        const val MAX_BYTES: Int = 32 * 1024 * 1024

        /** A palette is 256 RGB triples (some bundles carry 257; the green channel only reaches the first 256). */
        const val MAX_PALETTE_BYTES: Int = 1024

        private const val PALETTE_SIZE = 256
        private const val RGB_BYTES = 3
        private const val HEADER_SIZE = 20
        private const val INDEX_LENGTH_AT = 8
        private const val MAGIC_END = "\u0000"
        private const val MAX_RESTORED_SIDE = 4096

        /** Images one bundle keeps, the least recently used going first: more than a photo takes from one bundle. */
        internal const val MAX_CACHED_IMAGES = 1024
        private const val INITIAL_CACHE_CAPACITY = 64
        private const val CACHE_LOAD_FACTOR = 0.75f
        private const val INFLATE_CHUNK = 65536
        private const val MIN_INFLATE_BUFFER = 64
        private const val INFLATE_GROWTH = 3
        private const val MIME_JSON = "application/json"
        private const val MIME_PNG = "image/png"
        private const val MIME_BINARY = "application/octet-stream"
        private const val ARGB_BYTES = 4
        private const val PNG_WIDTH_AT = 16
        private const val PNG_HEADER_END = 24

        /** About what a parsed library index takes on the heap per byte of its text. */
        private const val JSON_HEAP_FACTOR = 4

        private fun channel(value: Byte): Int = value.toInt() and 0xFF

        private fun argbBytes(extent: Extent): Long = extent.width.toLong() * extent.height * ARGB_BYTES

        /** Width and height from a PNG's IHDR chunk; null for anything shorter. */
        private fun pngExtent(png: ByteArray): Extent? {
            if (png.size < PNG_HEADER_END) return null
            val header = ByteBuffer.wrap(png, PNG_WIDTH_AT, PNG_HEADER_END - PNG_WIDTH_AT)
            return Extent(header.getInt(), header.getInt()).takeIf { it.width > 0 && it.height > 0 }
        }

        /** The file extension of a bundle, as the client profile names it; empty without one. */
        fun extension(): String = ClientStrings[ClientStrings.BUNDLE_EXTENSION].orEmpty()

        @Throws(IOException::class)
        fun parse(data: ByteArray): AssetBundle {
            val magic = ClientStrings[ClientStrings.BUNDLE_MAGIC].orEmpty() + MAGIC_END
            val known = magic != MAGIC_END && data.size >= HEADER_SIZE
            if (!known || String(data, 0, magic.length, Charsets.ISO_8859_1) != magic) {
                throw IOException("not an asset bundle")
            }
            val stored = ByteBuffer.wrap(data, 0, HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).getInt(INDEX_LENGTH_AT)
            if (stored <= 0 || HEADER_SIZE.toLong() + stored > data.size) throw IOException("bad bundle index length")
            val index = JsonParser.parseString(inflate(data, HEADER_SIZE, stored).toString(Charsets.UTF_8)).asJsonObject
            var assets = JsonObject()
            var frames = JsonObject()
            var sheetPng: ByteArray? = null
            val pngEntries = HashMap<String, ByteArray>()
            val binaries = HashMap<String, ByteArray>()
            var kept = 0L
            for (element in index.getAsJsonArray("entries")) {
                val entry = element.asJsonObject
                val entryName = entry["name"].asString
                val mime = entry["mimeType"]?.asString.orEmpty()
                val body = entryBody(entry, data, HEADER_SIZE + stored)
                when {
                    mime == MIME_JSON && entryName.endsWith(".json") -> {
                        val library = JsonParser.parseString(body.toString(Charsets.UTF_8)).asJsonObject
                        kept += body.size.toLong() * JSON_HEAP_FACTOR
                        library["assets"]?.takeIf { it.isJsonObject }?.let { assets = it.asJsonObject }
                        library["spritesheet"]?.takeIf { it.isJsonObject }?.asJsonObject?.get("frames")?.let {
                            frames = it.asJsonObject
                        }
                    }

                    mime == MIME_PNG && entryName.endsWith(".png") -> {
                        sheetPng = body
                    }

                    mime == MIME_PNG -> {
                        pngEntries[entryName] = body
                        kept += body.size
                    }

                    mime == MIME_BINARY && body.size <= MAX_PALETTE_BYTES -> {
                        binaries[entryName] = body
                        kept += body.size
                    }
                }
            }
            kept += sheetPng?.size ?: 0
            val name = index["name"]?.asString.orEmpty()
            return AssetBundle(name, assets, frames, sheetPng, pngEntries, binaries, kept)
        }

        private fun entryBody(
            entry: JsonObject,
            data: ByteArray,
            base: Int,
        ): ByteArray {
            val offset = entry["offset"].asInt
            val stored = entry["storedLength"].asInt
            if (offset < 0 || stored < 0 || base.toLong() + offset + stored > data.size) {
                throw IOException("bad bundle entry ${entry["name"].asString}")
            }
            val deflated = entry["compression"]?.asString == "deflate"
            val start = base + offset
            return if (deflated) inflate(data, start, stored) else data.copyOfRange(start, start + stored)
        }

        private fun unrotate(rotated: BufferedImage): BufferedImage {
            val w = rotated.height
            val h = rotated.width
            val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until h) {
                for (x in 0 until w) out.setRGB(x, y, rotated.getRGB(h - 1 - y, x))
            }
            return out
        }

        @Throws(IOException::class)
        fun readPng(png: ByteArray): BufferedImage {
            val read = ImageIO.read(ByteArrayInputStream(png)) ?: throw IOException("unreadable png")
            if (read.type == BufferedImage.TYPE_INT_ARGB) return read
            return BufferedImage(read.width, read.height, BufferedImage.TYPE_INT_ARGB).also {
                it.createGraphics().apply {
                    drawImage(read, 0, 0, null)
                    dispose()
                }
            }
        }

        private fun inflate(
            data: ByteArray,
            from: Int,
            length: Int,
        ): ByteArray {
            val inflater = Inflater()
            try {
                inflater.setInput(data, from, length)
                return drain(inflater, max(MIN_INFLATE_BUFFER, length * INFLATE_GROWTH))
            } catch (e: DataFormatException) {
                throw IOException(e)
            } finally {
                inflater.end()
            }
        }

        private fun drain(
            inflater: Inflater,
            sizeHint: Int,
        ): ByteArray {
            val out = ByteArrayOutputStream(sizeHint)
            val chunk = ByteArray(INFLATE_CHUNK)
            while (!inflater.finished()) {
                val count = inflater.inflate(chunk)
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw IOException("truncated deflate")
                }
                out.write(chunk, 0, count)
                if (out.size() > MAX_BYTES) throw IOException("entry too large")
            }
            return out.toByteArray()
        }
    }
}
