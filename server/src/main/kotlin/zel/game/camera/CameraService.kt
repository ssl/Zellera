package zel.game.camera

import org.slf4j.LoggerFactory
import zel.content.ClientStrings
import zel.core.Configuration
import zel.core.config.GameEnvironment
import zel.db.Sql
import zel.db.SqlFailure
import zel.db.noneAsNull
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import javax.sql.DataSource

class CameraService(
    private val config: Configuration,
    dataSource: DataSource,
) {
    data class Preview(
        val file: Path,
        val createdMs: Long,
        val roomId: Int,
    )

    data class Stored(
        val id: String,
        val createdMs: Long,
    )

    /** The renderer and the assets behind it, rebuilt once an hour so new assets get picked up. */
    private class Fresh(
        val renderer: PhotoRenderer,
        val builtAt: Long,
    )

    private val sql = Sql(dataSource)
    private val previews = ConcurrentHashMap<Int, Preview>()
    private val lastRender = ConcurrentHashMap<Int, Long>()
    private val drawing: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private val photos = ConcurrentHashMap<Path, Stored>()
    private val rendering = Semaphore(RENDER_THREADS)
    private val work = workers()
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(HTTP_CONNECT_SECONDS)).build()
    private var fresh: Fresh? = null

    fun submit(job: Runnable): Boolean =
        try {
            work.execute(job)
            true
        } catch (_: RejectedExecutionException) {
            false
        }

    /** One frame at a time per player, one start per [MIN_RENDER_INTERVAL_MS] (refusals count); [release] ends it. */
    internal fun claim(
        userId: Int,
        now: Long,
    ): Boolean {
        if (!drawing.add(userId)) return false
        val last = lastRender[userId]
        if (last != null && now - last < MIN_RENDER_INTERVAL_MS) {
            drawing.remove(userId)
            return false
        }
        lastRender[userId] = now
        return true
    }

    internal fun release(userId: Int) {
        drawing.remove(userId)
    }

    /** Null when refused, rate-limited or both render slots are taken. */
    fun render(
        userId: Int,
        roomId: Int,
        frameJson: String,
    ): String? {
        val now = System.currentTimeMillis()
        return inTurn(userId, now, frameJson) { frame -> savePreview(frame, userId, roomId, now) }
    }

    /** Also the room's full picture: the web serves the public API image from this file, so the path stays. */
    fun renderThumbnail(
        userId: Int,
        roomId: Int,
        frameJson: String,
    ): Boolean = inTurn(userId, System.currentTimeMillis(), frameJson) { frame -> saveThumbnail(frame, roomId) } == true

    /** Runs [draw] on the parsed frame in the player's turn and a render slot; null when either is refused. */
    @Suppress("TooGenericExceptionCaught") // a frame that cannot be drawn only fails that frame
    private fun <T : Any> inTurn(
        userId: Int,
        now: Long,
        frameJson: String,
        draw: (PhotoFrame) -> T?,
    ): T? {
        if (!claim(userId, now)) return null
        try {
            if (!rendering.tryAcquire()) return null
            try {
                return read(userId, frameJson)?.let(draw)
            } catch (e: Exception) {
                logger.warn("Camera frame of user {} could not be drawn: {}", userId, e.toString())
                return null
            } finally {
                rendering.release()
            }
        } finally {
            release(userId)
        }
    }

    private fun savePreview(
        frame: PhotoFrame,
        userId: Int,
        roomId: Int,
        now: Long,
    ): String? {
        if (frame.roomId != roomId) return null
        val relative = "servercamera/preview/${siteCode()}/$userId-$now.png"
        val file = storage().resolve(relative)
        writePng(renderer().render(frame), file)
        previews.put(userId, Preview(file, now, roomId))?.let(::discard)
        forgetOld(now)
        return relative
    }

    private fun forgetOld(now: Long) {
        previews.entries.removeIf { (_, preview) ->
            (now - preview.createdMs > PREVIEW_LIFETIME_MS).also { old -> if (old) discard(preview) }
        }
        lastRender.entries.removeIf { (_, at) -> now - at >= MIN_RENDER_INTERVAL_MS }
    }

    private fun discard(preview: Preview) {
        photos.remove(preview.file)
        Files.deleteIfExists(preview.file)
    }

    private fun saveThumbnail(
        frame: PhotoFrame,
        roomId: Int,
    ): Boolean {
        if (frame.roomId != roomId) return false
        writePng(renderer().render(frame), storage().resolve("navigator-thumbnail/${siteCode()}/$roomId.png"))
        return true
    }

    @Suppress("TooGenericExceptionCaught") // a malformed frame is refused whatever way it fails to parse
    private fun read(
        userId: Int,
        frameJson: String,
    ): PhotoFrame? {
        if (!FrameChecksum.valid(frameJson)) {
            logger.info("Camera frame of user {} refused: not sealed by the client", userId)
            return null
        }
        return try {
            PhotoFrame.parse(frameJson)
        } catch (e: RuntimeException) {
            logger.info("Camera frame of user {} refused: {}", userId, e.message)
            null
        }
    }

    fun preview(userId: Int): Preview? = previews[userId]?.takeIf { Files.exists(it.file) }

    /** The first use gives the preview its id, file and camera_photos row; later uses return the same photo. */
    @Suppress("TooGenericExceptionCaught") // a photo that cannot be stored only fails that purchase
    fun photo(userId: Int): Stored? {
        val preview = preview(userId) ?: return null
        photos[preview.file]?.let { return it }
        val id = UUID.randomUUID().toString()
        val target = storage().resolve("servercamera/photos/${siteCode()}/$id.png")
        val thumbnail = target.resolveSibling("${id}_small.png")
        return try {
            Files.createDirectories(target.parent)
            Files.copy(preview.file, target, StandardCopyOption.REPLACE_EXISTING)
            val picture = ImageIO.read(target.toFile()) ?: throw IOException("the preview is no readable picture")
            writePng(small(picture), thumbnail)
            val stored = Stored(id, preview.createdMs)
            sql.write(
                "INSERT INTO camera_photos (id, player_id, room_id, created_at_ms) VALUES (?, ?, ?, ?)",
                stored.id,
                userId,
                noneAsNull(preview.roomId),
                stored.createdMs,
            )
            photos[preview.file] = stored
            stored
        } catch (e: Exception) {
            logger.warn("Camera photo of user {} could not be stored: {}", userId, e.toString())
            runCatching {
                Files.deleteIfExists(target)
                Files.deleteIfExists(thumbnail)
            }
            null
        }
    }

    fun purchased(photo: Stored) {
        update("UPDATE camera_photos SET purchases = purchases + 1 WHERE id = ?", photo.id)
    }

    fun published(photo: Stored): Boolean =
        update(
            "UPDATE camera_photos SET published = 1, published_at = UNIX_TIMESTAMP() WHERE id = ? AND published = 0",
            photo.id,
        )

    private fun update(
        statement: String,
        id: String,
    ): Boolean =
        try {
            sql.write(statement, id) == 1
        } catch (failure: SqlFailure) {
            logger.error("Camera photo {} could not be updated", id, failure)
            false
        }

    private fun storage(): Path = Path.of(config.getValue("camera.storage.folder", "usercontent"))

    private fun siteCode(): String =
        checkNotNull(ClientStrings[ClientStrings.SERVER_ID]) { "the client profile names no server id" }

    private fun profilePath(name: String): String = ClientStrings[name]?.let { "/assets/$it/" }.orEmpty()

    @Synchronized
    private fun renderer(): PhotoRenderer {
        val now = System.currentTimeMillis()
        val current = fresh?.takeIf { now - it.builtAt <= ASSETS_REFRESH_MS }
        return (current ?: Fresh(buildRenderer(), now).also { fresh = it }).renderer
    }

    private fun buildRenderer(): PhotoRenderer {
        val web = config.getValue(GameEnvironment.LOCAL_SITE_ROW, GameEnvironment.DEFAULT_LOCAL_SITE)
        val source: (String) -> ByteArray? = { path -> fetch(web + path, CameraSources.MAX_ASSET_BYTES) }
        val variables = CameraSources.variables(source("/gamedata/external_variables"))
        val locations =
            CameraAssets.Locations(
                furni =
                    CameraSources.path(
                        variables["flash.dynamic.download.url"] ?: profilePath(ClientStrings.FURNI_PATH),
                    ),
                figure = CameraSources.path(variables["flash.client.url"].orEmpty()),
                imageLibrary =
                    CameraSources.path(variables["image.library.url"] ?: profilePath(ClientStrings.IMAGE_ROOT)),
                roomContent = ClientStrings[ClientStrings.ROOM_CONTENT_FILE].orEmpty(),
            )
        val assets = CameraAssets(source, locations, source("/gamedata/furnidata"), CameraAssets.pets(variables))
        return PhotoRenderer(assets) { url -> ownImage(web, url) }
    }

    /**
     * Draws a URL sprite only for this game's photos, group badges or mirrored images: photos and badges from the
     * storage folder, the rest fetched from the game's web server.
     */
    @Suppress("TooGenericExceptionCaught") // an image that cannot be fetched or read is simply not drawn
    private fun ownImage(
        web: String,
        url: String,
    ): BufferedImage? =
        try {
            CameraSources
                .mirrorPath(url)
                ?.let { path ->
                    val stored = CameraSources.storedFile(path)
                    if (stored == null) fetch(web + path, CameraSources.MAX_PICTURE_BYTES) else readStored(stored)
                }?.let { CameraSources.picture(it, CameraSources.MAX_PICTURE_SIDE) }
        } catch (_: Exception) {
            null
        }

    /** A file of the storage folder, or null when it is missing, too big or outside the folder. */
    private fun readStored(relative: String): ByteArray? {
        val root = storage().toAbsolutePath().normalize()
        val file = root.resolve(relative).normalize()
        val inside = file.startsWith(root) && Files.isRegularFile(file)
        return if (inside && Files.size(file) <= CameraSources.MAX_PICTURE_BYTES) Files.readAllBytes(file) else null
    }

    private fun fetch(
        url: String,
        maxBytes: Int,
    ): ByteArray? {
        val request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(FETCH_TIMEOUT_SECONDS)).build()
        val response =
            try {
                http.send(request, HttpResponse.BodyHandlers.ofInputStream())
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException(e)
            }
        return response.body().use { body ->
            val declared = response.headers().firstValueAsLong("content-length").orElse(-1)
            if (response.statusCode() != HTTP_OK ||
                declared > maxBytes
            ) {
                null
            } else {
                CameraSources.readCapped(body, maxBytes)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CameraService::class.java)
        private const val SMALL_SIZE = 100
        private const val ASSETS_REFRESH_MS = 60L * 60 * 1000
        private const val HTTP_CONNECT_SECONDS = 5L
        private const val FETCH_TIMEOUT_SECONDS = 20L
        private const val HTTP_OK = 200

        private const val PREVIEW_LIFETIME_MS = 60L * 60 * 1000
        private const val MIN_RENDER_INTERVAL_MS = 250L
        internal const val RENDER_THREADS = 2
        internal const val MAX_QUEUED = 8

        fun itemData(
            stored: Stored,
            boughtMs: Long = System.currentTimeMillis(),
        ): String = "{\"t\":$boughtMs,\"id\":\"${stored.id}\"}"

        private fun workers(): ThreadPoolExecutor {
            val count = AtomicInteger()
            return ThreadPoolExecutor(
                RENDER_THREADS,
                RENDER_THREADS,
                0,
                TimeUnit.MILLISECONDS,
                ArrayBlockingQueue(MAX_QUEUED),
                { job -> Thread(job, "camera-render-${count.incrementAndGet()}").apply { isDaemon = true } },
                ThreadPoolExecutor.AbortPolicy(),
            )
        }

        private fun writePng(
            picture: BufferedImage,
            file: Path,
        ) {
            Files.createDirectories(file.parent)
            ImageIO.write(picture, "png", file.toFile())
        }

        private fun small(full: BufferedImage): BufferedImage {
            val out = BufferedImage(SMALL_SIZE, SMALL_SIZE, BufferedImage.TYPE_INT_ARGB)
            val g = out.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(full, 0, 0, SMALL_SIZE, SMALL_SIZE, null)
            g.dispose()
            return out
        }
    }
}
