package zel.game.camera

import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import zel.game.camera.PhotoFrame.Companion.MAX_COLUMNS
import zel.game.camera.PhotoFrame.Companion.MAX_COORDINATE
import zel.game.camera.PhotoFrame.Companion.MAX_FILTERS
import zel.game.camera.PhotoFrame.Companion.MAX_MASKS
import zel.game.camera.PhotoFrame.Companion.MAX_PLANES
import zel.game.camera.PhotoFrame.Companion.MAX_SPRITES
import java.io.IOException
import java.io.StringReader
import kotlin.math.abs

/** Reads a [PhotoFrame] strictly: anything past the limits refuses the frame; unused keys are skipped unbuilt. */
internal object PhotoFrameReader {
    private val NAME = Regex("^[A-Za-z0-9_\\-@*]{1,160}$")
    private val PALETTE = Regex("^[A-Za-z0-9_\\-]{1,120}$")
    private val URL = Regex("^https?://[A-Za-z0-9.\\-:]{1,100}/[A-Za-z0-9_\\-./%]{1,300}$")

    private val SITE_PATH = Regex("^/[A-Za-z0-9_\\-][A-Za-z0-9_\\-./]{0,300}(?:\\?[A-Za-z0-9_=&.\\-]{0,100})?$")
    private const val OPAQUE_ALPHA = 255
    private const val CORNERS = 4

    fun read(json: String): PhotoFrame =
        try {
            JsonReader(StringReader(json)).use { reader ->
                reader.setStrictness(Strictness.LENIENT)
                frame(reader).also {
                    require(reader.peek() == JsonToken.END_DOCUMENT) { "text after the frame" }
                }
            }
        } catch (e: IOException) {
            throw IllegalArgumentException("frame json: ${e.message}", e)
        }

    private fun frame(reader: JsonReader): PhotoFrame {
        var planes: List<PhotoFrame.Plane>? = null
        var sprites: List<PhotoFrame.Sprite>? = null
        var filters: List<PhotoFrame.FilterUse>? = null
        var roomId: Int? = null
        var zoom: Int? = null
        reader.eachKey { key ->
            when (key) {
                "planes" -> planes = once(planes, key, list(reader, key, MAX_PLANES, ::plane))
                "sprites" -> sprites = once(sprites, key, list(reader, key, MAX_SPRITES, ::sprite))
                "filters" -> filters = once(filters, key, list(reader, key, MAX_FILTERS, ::filter))
                "zoom" -> zoom = once(zoom, key, intValue(reader))
                "roomid" -> roomId = once(roomId, key, intValue(reader))
                else -> reader.skipValue()
            }
        }
        val drawnPlanes = requireNotNull(planes) { "no planes" }
        val drawnSprites = requireNotNull(sprites) { "no sprites" }
        val scale = zoom ?: 1
        require(scale == 1 || scale == 2) { "zoom $scale" }
        return PhotoFrame(drawnPlanes, drawnSprites, filters.orEmpty(), requireNotNull(roomId) { "no roomid" }, scale)
    }

    private fun plane(reader: JsonReader): PhotoFrame.Plane {
        var corners: List<PhotoFrame.Point>? = null
        var textures = emptyList<String>()
        var masks = emptyList<PhotoFrame.Mask>()
        var z = 0.0
        var color = 0
        var bottomAligned = false
        reader.eachKey { key ->
            when (key) {
                "cornerPoints" -> {
                    corners =
                        list(reader, "plane corners", CORNERS, ::point).also {
                            require(it.size == CORNERS) { "plane corners" }
                        }
                }

                "texCols" -> {
                    textures =
                        list(reader, "texCols", MAX_COLUMNS, ::firstAssetName)
                            .filter { it.isNotEmpty() }
                            .map(::assetName)
                }

                "masks" -> {
                    masks = list(reader, "masks", MAX_MASKS, ::mask)
                }

                "z" -> {
                    z = number(reader, "z")
                }

                "color" -> {
                    color = intValue(reader) and PhotoPixels.RGB
                }

                "bottomAligned" -> {
                    bottomAligned = flag(reader)
                }

                else -> {
                    reader.skipValue()
                }
            }
        }
        return PhotoFrame.Plane(
            z,
            requireNotNull(corners) { "plane corners" },
            color,
            bottomAligned,
            textures,
            masks,
        )
    }

    private fun point(reader: JsonReader): PhotoFrame.Point {
        var x = 0.0
        var y = 0.0
        reader.eachKey { key ->
            when (key) {
                "x" -> x = inRange(number(reader, "x"), "x")
                "y" -> y = inRange(number(reader, "y"), "y")
                else -> reader.skipValue()
            }
        }
        return PhotoFrame.Point(x, y)
    }

    private fun firstAssetName(reader: JsonReader): String? {
        var first: String? = null
        reader.eachKey { key ->
            if (key != "assetNames" || reader.peek() == JsonToken.NULL) {
                reader.skipValue()
            } else {
                reader.beginArray()
                first = if (reader.hasNext()) string(reader) else null
                while (reader.hasNext()) reader.skipValue()
                reader.endArray()
            }
        }
        return first
    }

    private fun mask(reader: JsonReader): PhotoFrame.Mask {
        var name: String? = null
        var location: PhotoFrame.Point? = null
        var flipH = false
        var flipV = false
        reader.eachKey { key ->
            when (key) {
                "name" -> name = string(reader)
                "location" -> location = point(reader)
                "flipH" -> flipH = flag(reader)
                "flipV" -> flipV = flag(reader)
                else -> reader.skipValue()
            }
        }
        val at = requireNotNull(location) { "mask" }
        return PhotoFrame.Mask(assetName(requireNotNull(name) { "mask" }), at.x, at.y, flipH, flipV)
    }

    private fun sprite(reader: JsonReader): PhotoFrame.Sprite? {
        var name: String? = null
        var blend: String? = null
        var palette: String? = null
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var width = 0.0
        var height = 0.0
        var skew = 0.0
        var color = -1
        var alpha = OPAQUE_ALPHA
        var flipH = false
        var frame = false
        reader.eachKey { key ->
            when (key) {
                "name" -> name = string(reader)

                "x" -> x = number(reader, "x")

                "y" -> y = number(reader, "y")

                "z" -> z = number(reader, "z")

                "color" -> color = intValue(reader) and PhotoPixels.RGB

                "alpha" -> alpha = intValue(reader).coerceIn(0, OPAQUE_ALPHA)

                "flipH" -> flipH = flag(reader)

                "blendMode" -> blend = string(reader)

                "width" -> width = number(reader, "width")

                "height" -> height = number(reader, "height")

                "skew" -> skew = number(reader, "skew")

                "frame" -> frame = flag(reader)

                // a paletted layer's name loses its "@<palette id>" and the palette's source asset comes along
                "paletteSourceName" -> palette = string(reader)

                else -> reader.skipValue()
            }
        }
        val sent = requireNotNull(name) { "sprite name" }
        val isUrl = sent.startsWith("http") || sent.startsWith("/")
        val allowed =
            when {
                ".." in sent -> false
                sent.startsWith("/") -> SITE_PATH.matches(sent)
                isUrl -> URL.matches(sent)
                else -> NAME.matches(sent)
            }
        if (!allowed) return null
        return PhotoFrame.Sprite(
            if (!isUrl && sent.indexOf('@') > 0) sent.substringBefore('@') else sent,
            isUrl,
            inRange(x, "x"),
            inRange(y, "y"),
            z,
            color,
            alpha,
            flipH,
            blend,
            width.coerceIn(0.0, PhotoRenderer.MAX_SIZE.toDouble()).toInt(),
            height.coerceIn(0.0, PhotoRenderer.MAX_SIZE.toDouble()).toInt(),
            skew.coerceIn(-1.0, 1.0),
            frame,
            palette?.takeIf { !isUrl && PALETTE.matches(it) },
        )
    }

    private fun filter(reader: JsonReader): PhotoFrame.FilterUse {
        var name: String? = null
        var alpha = OPAQUE_ALPHA
        reader.eachKey { key ->
            when (key) {
                "name" -> name = string(reader)
                "alpha" -> alpha = intValue(reader)
                else -> reader.skipValue()
            }
        }
        val known = name?.takeIf { PhotoFilters[it] != null }
        return PhotoFrame.FilterUse(
            requireNotNull(known) { "unknown filter $name" },
            alpha.coerceIn(0, OPAQUE_ALPHA),
        )
    }

    private inline fun JsonReader.eachKey(onKey: (String) -> Unit) {
        beginObject()
        while (hasNext()) onKey(nextName())
        endObject()
    }

    private fun <T : Any> list(
        reader: JsonReader,
        key: String,
        max: Int,
        element: (JsonReader) -> T?,
    ): List<T> {
        require(reader.peek() == JsonToken.BEGIN_ARRAY) { "no $key" }
        val out = ArrayList<T>()
        var count = 0
        reader.beginArray()
        while (reader.hasNext()) {
            count++
            require(count <= max) { "too many $key" }
            element(reader)?.let(out::add)
        }
        reader.endArray()
        return out
    }

    private fun <T : Any> once(
        previous: T?,
        key: String,
        value: T,
    ): T {
        require(previous == null) { "two $key" }
        return value
    }

    private fun assetName(name: String): String {
        require(NAME.matches(name)) { "asset name" }
        return name
    }

    private fun string(reader: JsonReader): String =
        when (reader.peek()) {
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
            else -> throw IllegalArgumentException("not a string")
        }

    private fun inRange(
        value: Double,
        key: String,
    ): Double {
        require(abs(value) <= MAX_COORDINATE) { "$key out of range" }
        return value
    }

    private fun number(
        reader: JsonReader,
        key: String,
    ): Double {
        val token = reader.peek()
        require(token == JsonToken.NUMBER || token == JsonToken.STRING) { key }
        return reader.nextString().toDouble().also { require(it.isFinite()) { key } }
    }

    private fun intValue(reader: JsonReader): Int {
        val token = reader.peek()
        require(token == JsonToken.NUMBER || token == JsonToken.STRING) { "not a number" }
        val text = reader.nextString()
        return text.toIntOrNull()
            ?: text.toLongOrNull()?.toInt()
            ?: text.toDouble().also { require(it.isFinite()) { "not a number" } }.toInt()
    }

    private fun flag(reader: JsonReader): Boolean =
        when (reader.peek()) {
            JsonToken.BOOLEAN -> {
                reader.nextBoolean()
            }

            JsonToken.STRING, JsonToken.NUMBER -> {
                reader.nextString().toBoolean()
            }

            else -> {
                reader.skipValue()
                false
            }
        }
}
