package zel.protocol.camera

import org.slf4j.LoggerFactory
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.camera.CameraService
import zel.game.camera.PhotoCompetition
import zel.game.economy.LedgerEntry
import zel.game.permissions.isAnyOwner
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.CameraPublishStatusOut
import zel.protocol.generated.CameraPublishStatusOutCodec
import zel.protocol.generated.CameraPurchaseOKOut
import zel.protocol.generated.CameraPurchaseOKOutCodec
import zel.protocol.generated.CameraStorageUrlOut
import zel.protocol.generated.CameraStorageUrlOutCodec
import zel.protocol.generated.CompetitionStatusOut
import zel.protocol.generated.CompetitionStatusOutCodec
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.InitCameraOut
import zel.protocol.generated.InitCameraOutCodec
import zel.protocol.generated.PhotoCompetitionInCodec
import zel.protocol.generated.PublishPhotoInCodec
import zel.protocol.generated.PurchasePhotoInCodec
import zel.protocol.generated.RenderRoomInCodec
import zel.protocol.generated.RenderRoomThumbnailInCodec
import zel.protocol.generated.RequestCameraConfigurationInCodec
import zel.protocol.generated.ThumbnailStatusOut
import zel.protocol.generated.ThumbnailStatusOutCodec
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.server.GameServices
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.DataFormatException
import java.util.zip.Inflater

class CameraPackets(
    private val services: GameServices,
    private val camera: CameraService,
    private val competition: PhotoCompetition,
) : PacketPackage {
    private val log = LoggerFactory.getLogger(CameraPackets::class.java)
    private val pictureCooldown = ClientCooldown("camera.picture", FRAME_GAP_MS)
    private val thumbnailCooldown = ClientCooldown("camera.thumbnail", FRAME_GAP_MS)
    private val publishCooldown = ClientCooldown("camera.publish", PUBLISH_GAP_MS)
    private val competitionCooldown = ClientCooldown("camera.competition", COMPETITION_GAP_MS)

    override val name = "camera"
    override val routes =
        listOf(
            PacketRoute(RequestCameraConfigurationInCodec) { session, _ ->
                val prices = CameraPrices.from(services.config)
                session.send(
                    InitCameraOutCodec,
                    InitCameraOut(prices.photo.credits, prices.photo.points, prices.publish.points),
                )
            },
            PacketRoute(RenderRoomInCodec) { session, m ->
                if (!pictureCooldown.acquire(session.client)) return@PacketRoute
                shutter(session, m.length, m.frame)
            },
            PacketRoute(RenderRoomThumbnailInCodec) { session, m ->
                if (!thumbnailCooldown.acquire(session.client)) return@PacketRoute
                thumbnail(session, m.length, m.frame)
            },
            PacketRoute(PurchasePhotoInCodec) { session, _ -> purchase(session) },
            PacketRoute(PublishPhotoInCodec) { session, _ ->
                if (!publishCooldown.acquire(session.client)) return@PacketRoute
                publish(session)
            },
            PacketRoute(PhotoCompetitionInCodec) { session, _ ->
                if (!competitionCooldown.acquire(session.client)) return@PacketRoute
                enterCompetition(session)
            },
        )

    private fun shutter(
        session: PacketSession,
        length: Int,
        frame: ByteArray,
    ) {
        val player = session.client.player ?: return
        val userId = player.profile.id
        val roomId = services.game.rooms.roomOf(userId) ?: return
        val json = inflateFrame(length, frame) ?: return
        keepSample(userId, json)
        val queued =
            camera.submit {
                val url = camera.render(userId, roomId, json) { services.game.rooms.furniClasses(roomId) }
                session.url(url.orEmpty())
            }
        if (!queued) session.url("")
    }

    private fun thumbnail(
        session: PacketSession,
        length: Int,
        frame: ByteArray,
    ) {
        val player = session.client.player ?: return
        val userId = player.profile.id
        val rooms = services.game.rooms
        val roomId = rooms.roomOf(userId) ?: return
        val owns = rooms.summary(roomId)?.ownerId == userId || player.isAnyOwner
        if (!owns) return
        val json = inflateFrame(length, frame) ?: return
        camera.submit {
            if (camera.renderThumbnail(userId, roomId, json) { rooms.furniClasses(roomId) }) {
                session.send(ThumbnailStatusOutCodec, ThumbnailStatusOut(null))
            }
        }
    }

    private fun PacketSession.url(url: String) = send(CameraStorageUrlOutCodec, CameraStorageUrlOut(url))

    private fun purchase(session: PacketSession) {
        val client = session.client
        val player = client.player ?: return
        val userId = player.profile.id
        if (camera.preview(userId) == null || !player.stats.standing.safetyAllows()) return

        val price = CameraPrices.from(services.config).photo
        if (player.profile.credits < price.credits) {
            CatalogReplies.notEnoughBalance(client, true, false, 0)
            return
        }
        if (price.points > 0 && player.profile.balance(price.pointsType) < price.points) {
            CatalogReplies.notEnoughBalance(client, false, true, price.pointsType)
            return
        }
        val items = checkNotNull(services.game.items)
        val item = items.getItem(services.config.getInt("camera.photo.furni_id"))
        if (item == null || "display.image" != item.interaction.name) return
        val stored = camera.photo(userId) ?: return
        if (!price.takeFrom(player.wallet, LedgerEntry.newKey("camera:buy:$userId"))) return
        val photo = items.newFurni(userId, item, 0, 0, CameraService.itemData(stored))
        if (photo == null) {
            price.refundTo(player.wallet)
            return
        }

        camera.purchased(stored)
        player.inventory.furni.put(photo)
        session.send(CameraPurchaseOKOutCodec, CameraPurchaseOKOut)
        client.send(AddRoomItem(photo))
        client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
        checkNotNull(services.game.achievements).count(player, GameEvents.CAMERA_PHOTO)
    }

    private fun publish(session: PacketSession) {
        val client = session.client
        val player = client.player ?: return
        val userId = player.profile.id
        val stored = camera.photo(userId).takeIf { camera.preview(userId) != null }
        if (stored == null || !player.stats.standing.safetyAllows()) {
            session.publishStatus(false, "")
            return
        }
        val price = CameraPrices.from(services.config).publish
        if (player.profile.balance(price.pointsType) < price.points) {
            CatalogReplies.notEnoughBalance(client, false, true, price.pointsType)
            return
        }
        if (!price.takeFrom(player.wallet, LedgerEntry.newKey("camera:publish:$userId"))) {
            session.publishStatus(false, "")
            return
        }
        if (!camera.published(stored)) {
            price.refundTo(player.wallet)
            session.publishStatus(false, "")
            return
        }
        session.publishStatus(true, stored.id)
        PlayerActions.progress(player, CountedAction.PUBLISH_PICTURE, 1)
    }

    private fun PacketSession.publishStatus(
        ok: Boolean,
        photoId: String,
    ) = send(
        CameraPublishStatusOutCodec,
        CameraPublishStatusOut(ok, 0, photoId.ifEmpty { null }),
    )

    private fun enterCompetition(session: PacketSession) {
        val player = session.client.player ?: return
        val userId = player.profile.id
        val preview = camera.preview(userId)
        val door = competition.door(userId)
        if (preview == null || door == PhotoCompetition.Door.CLOSED) {
            session.competition(false, FAILED)
            return
        }
        if (door == PhotoCompetition.Door.DAILY_LIMIT_REACHED) {
            session.competition(false, TOO_MANY)
            return
        }
        val copy = camera.photo(userId)
        val stored = copy != null && competition.submit(userId, preview.roomId, copy.id)
        session.competition(stored, if (stored) "" else FAILED)
    }

    private fun PacketSession.competition(
        accepted: Boolean,
        status: String,
    ) = send(CompetitionStatusOutCodec, CompetitionStatusOut(accepted, status))

    private fun inflateFrame(
        length: Int,
        frame: ByteArray,
    ): String? {
        if (length <= 0 || length > MAX_COMPRESSED_BYTES || length > frame.size) return null
        return inflate(frame.copyOf(length))
    }

    /** Development only (`camera.samples.folder`): keeps the newest [KEPT_SAMPLES] frames for inspection. */
    private fun keepSample(
        userId: Int,
        json: String,
    ) {
        val dir = services.config.getValue("camera.samples.folder", "")
        if (dir.isBlank()) return
        val out = File(dir, "${userId}_${services.unixTimestamp()}.json")
        try {
            Files.createDirectories(out.parentFile.toPath())
            Files.writeString(out.toPath(), json)
            dropOldSamples(out.parentFile)
        } catch (e: IOException) {
            log.warn("Camera frame could not be kept: {}", e.message)
        }
    }

    private fun dropOldSamples(dir: File) {
        val kept = dir.listFiles { _, name -> name.endsWith(".json") }.orEmpty()
        if (kept.size <= KEPT_SAMPLES) return
        kept
            .sortedByDescending(File::lastModified)
            .drop(KEPT_SAMPLES)
            .forEach { Files.deleteIfExists(it.toPath()) }
    }

    companion object {
        const val MAX_COMPRESSED_BYTES = 1024 * 1024
        const val MAX_JSON_BYTES = 1024 * 1024

        /** One frame per 250 ms per connection; a faster one is dropped unanswered before it is inflated. */
        const val FRAME_GAP_MS = 250L
        private const val PUBLISH_GAP_MS = 1000L
        private const val COMPETITION_GAP_MS = 2000L
        private const val TOO_MANY = "too-many-submits"
        private const val FAILED = "error"
        private const val CHUNK = 16384
        private const val KEPT_SAMPLES = 200

        fun inflate(compressed: ByteArray): String? {
            val inflater = Inflater()
            return try {
                inflater.setInput(compressed)
                val out = ByteArrayOutputStream(compressed.size * 4)
                val chunk = ByteArray(CHUNK)
                while (!inflater.finished()) {
                    val n = inflater.inflate(chunk)
                    if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) return null
                    out.write(chunk, 0, n)
                    if (out.size() > MAX_JSON_BYTES) return null
                }
                out.toString(StandardCharsets.UTF_8)
            } catch (ignored: DataFormatException) {
                null
            } finally {
                inflater.end()
            }
        }
    }
}
