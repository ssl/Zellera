package zel.game.camera

import zel.content.ClientStrings
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Locale
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

object CameraSources {
    const val MAX_ASSET_BYTES = AssetBundle.MAX_BYTES
    const val MAX_PICTURE_BYTES = 4 * 1024 * 1024
    const val MAX_PICTURE_SIDE = 2048
    private const val READ_CHUNK = 16384
    private const val INITIAL_BUFFER = 64 * 1024

    /** The header is checked before decoding. */
    @Throws(IOException::class)
    fun picture(
        bytes: ByteArray,
        maxSide: Int,
    ): BufferedImage? =
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes))?.use { input -> readPicture(input, maxSide) }

    private fun readPicture(
        input: ImageInputStream,
        maxSide: Int,
    ): BufferedImage? {
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) return null
        val reader = readers.next()
        try {
            reader.setInput(input, true, true)
            val tooBig = reader.getWidth(0) > maxSide || reader.getHeight(0) > maxSide
            return if (tooBig) null else reader.read(0)
        } finally {
            reader.dispose()
        }
    }

    /** The game web server's path for a drawable URL sprite, or null; checked on the raw path, sent without escapes. */
    fun mirrorPath(url: String): String? {
        val uri = URI.create(url)
        val path = uri.rawPath
        if (path == null || "%" in path || "\\" in path || ".." in path || "//" in path) return null
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        val images = ClientStrings[ClientStrings.IMAGE_ROOT]?.let { "/$it/" }
        return when {
            images == null -> path.takeIf { MIRRORED_PREFIXES.any(path::startsWith) }
            host == ClientStrings[ClientStrings.ASSET_HOST] && path.startsWith(images) -> "/assets$path"
            MIRRORED_PREFIXES.any(path::startsWith) || path.startsWith("/assets$images") -> path
            else -> null
        }
    }

    /**
     * The file under the camera's storage folder that a [mirrorPath] names, or null for a path only the web has.
     * Photos and badges are the server's own files, and the web shows them to logged-in visitors only.
     */
    fun storedFile(path: String): String? =
        when {
            path.startsWith(STORED_PHOTOS) && path.endsWith(".png") -> path.removePrefix("/usercontent/")
            else -> STORED_BADGE.matchEntire(path)?.let { "badges/${it.groupValues[1]}.png" }
        }

    private const val STORED_PHOTOS = "/usercontent/servercamera/photos/"
    private val STORED_BADGE = Regex("/imaging/badge/([A-Za-z0-9]{1,64})\\.(?:png|gif)")

    private val MIRRORED_PREFIXES = listOf("/usercontent/servercamera/", "/imaging/badge/")

    @Throws(IOException::class)
    fun readCapped(
        input: InputStream,
        maxBytes: Int,
    ): ByteArray? {
        val out = ByteArrayOutputStream(minOf(maxBytes, INITIAL_BUFFER))
        val chunk = ByteArray(READ_CHUNK)
        while (true) {
            val n = input.read(chunk)
            if (n == -1) return out.toByteArray()
            if (out.size() + n > maxBytes) return null
            out.write(chunk, 0, n)
        }
    }

    fun variables(text: ByteArray?): Map<String, String> =
        text
            ?.toString(Charsets.UTF_8)
            ?.split("\n")
            ?.mapNotNull { line ->
                val eq = line.indexOf('=')
                if (eq > 0) line.substring(0, eq).trim() to line.substring(eq + 1).trim() else null
            }?.toMap()
            .orEmpty()

    fun path(url: String): String {
        if (url.startsWith("/") && !url.startsWith("//")) return url
        return URI.create(if (url.startsWith("//")) "https:$url" else url).path ?: "/"
    }
}
