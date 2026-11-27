package zel.ext

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.net.clients.ClientConnections
import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.OutgoingCodec
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.entry.ArrivalSpot
import zel.room.entry.ArrivalSpots
import zel.room.map.Point
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.walk.WalkHooks
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import javax.sql.DataSource

class GameExtensions private constructor(
    private val found: List<Found>,
    private val config: Configuration?,
) {
    private class Found(
        val manifest: ExtensionManifest,
        val folder: Path,
        val extension: GameExtension,
    )

    private val listeners = CopyOnWriteArrayList<Owned<RoomListener>>()
    private val ticks = CopyOnWriteArrayList<Owned<TickPart>>()
    private val parts = CopyOnWriteArrayList<Owned<RoomPart>>()
    private val claims = CopyOnWriteArrayList<Owned<ClickClaim>>()
    private val spots = CopyOnWriteArrayList<Owned<ArrivalSpots>>()
    private val started = CopyOnWriteArrayList<Found>()

    private class Owned<T>(
        val name: String,
        val hook: T,
    )

    val names: List<String> get() = found.map { it.manifest.name }

    val listener: RoomListener =
        RoomListener { scope, event -> listeners.forEach { guarded(it.name) { it.hook.on(scope, event) } } }

    val tick: TickPart = TickPart { scope -> ticks.forEach { guarded(it.name) { it.hook.run(scope) } } }

    val part: RoomPart =
        object : RoomPart {
            override fun load(room: Room) = parts.forEach { guarded(it.name) { it.hook.load(room) } }

            override fun unload(scope: RoomScope) = parts.forEach { guarded(it.name) { it.hook.unload(scope) } }
        }

    val arrivals: ArrivalSpots =
        ArrivalSpots { scope, playerId ->
            spots.firstNotNullOfOrNull { guarded<ArrivalSpot?>(it.name, null) { it.hook.spotOf(scope, playerId) } }
        }

    fun walkHooks(next: WalkHooks): WalkHooks =
        object : WalkHooks by next {
            override fun takesClick(
                scope: RoomScope,
                unit: RoomUnit,
                clicked: Point,
            ): Boolean =
                claims.any { guarded(it.name, false) { it.hook.takes(scope, unit, clicked) } } ||
                    next.takesClick(scope, unit, clicked)
        }

    fun start(
        rooms: ExtensionRooms,
        dataSource: DataSource,
        inOrder: (key: Any, job: Runnable) -> Unit,
        clients: ClientConnections,
    ) {
        found.forEach { each ->
            val name = each.manifest.name
            val host = Host(each, rooms, StoredPlayerValues(name, dataSource, inOrder), clients)
            try {
                each.extension.start(host)
                started += each
                log.info("game extension {} {} started", name, each.manifest.version)
            } catch (e: LinkageError) {
                log.error("game extension {} does not fit this server", name, e)
                drop(name)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: RuntimeException,
            ) {
                log.error("game extension {} failed to start", name, e)
                drop(name)
            }
        }
    }

    fun stop() {
        started.forEach { each -> guarded(each.manifest.name) { each.extension.stop() } }
        started.clear()
    }

    private fun drop(name: String) {
        listOf(listeners, ticks, parts, claims, spots).forEach { list -> list.removeIf { it.name == name } }
        ExtensionBus.drop(name)
    }

    private fun guarded(
        name: String,
        block: () -> Unit,
    ) = guarded(name, Unit, block)

    private fun <T> guarded(
        name: String,
        fallback: T,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (
            @Suppress("TooGenericExceptionCaught") e: RuntimeException,
        ) {
            log.error("game extension {}: a hook failed", name, e)
            fallback
        }

    private inner class Host(
        private val each: Found,
        override val rooms: ExtensionRooms,
        override val values: PlayerValues,
        private val clients: ClientConnections,
    ) : ExtensionHost {
        override val name: String = each.manifest.name
        override val folder: Path = each.folder
        override val log: Logger = LoggerFactory.getLogger("zel.ext.$name")

        override fun setting(key: String): String? =
            config?.valueOrNull("extensions.$name.$key") ?: each.manifest.settings[key]

        override fun onEvent(listener: RoomListener) {
            listeners += Owned(name, listener)
        }

        override fun onTick(part: TickPart) {
            ticks += Owned(name, part)
        }

        override fun onRoom(part: RoomPart) {
            parts += Owned(name, part)
        }

        override fun onClick(claim: ClickClaim) {
            claims += Owned(name, claim)
        }

        override fun onArrival(spots: ArrivalSpots) {
            this@GameExtensions.spots += Owned(name, spots)
        }

        override fun <T> onPacketIn(
            codec: IncomingCodec<T>,
            hook: PacketHook<T>,
        ) = ExtensionBus.hookIncoming(name, codec, hook)

        override fun <T> onPacketOut(
            codec: OutgoingCodec<T>,
            hook: PacketHook<T>,
        ) = ExtensionBus.hookOutgoing(name, codec, hook)

        override fun onServerEvent(listener: (ServerEvent) -> Unit) = ExtensionBus.listen(name, listener)

        override fun <T> send(
            playerId: Int,
            codec: OutgoingCodec<T>,
            message: T,
        ): Boolean {
            val client = clients.owner(playerId)?.takeUnless { it.isClosed } ?: return false
            client.send(codec, message)
            return true
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GameExtensions::class.java)

        const val LIST_KEY = "game.extensions"

        const val FOLDER_KEY = "game.extensions.folder"

        val None = GameExtensions(emptyList(), null)

        private val NAME = Regex("^[a-z][a-z0-9]{1,31}$")

        fun load(
            config: Configuration,
            serverVersion: String,
        ): GameExtensions {
            val list = config.startupValueOrNull(LIST_KEY).orEmpty()
            val custom = config.startupValueOrNull(FOLDER_KEY)?.let(Path::of)
            val found =
                folders(list, custom).mapNotNull { folder ->
                    try {
                        loadOne(folder, serverVersion)
                    } catch (e: ExtensionError) {
                        log.error("game extension in {} left out: {}", folder, e.message)
                        null
                    } catch (e: ReflectiveOperationException) {
                        log.error("game extension in {} left out: its main class cannot be made", folder, e)
                        null
                    } catch (e: LinkageError) {
                        log.error("game extension in {} left out: it does not fit this server", folder, e)
                        null
                    }
                }
            if (found.isNotEmpty()) log.info("game extensions: {}", found.joinToString { it.manifest.name })
            return GameExtensions(found, config)
        }

        internal fun folders(
            list: String,
            custom: Path?,
        ): List<Path> =
            list
                .split(',', ' ', '\t', '\n')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .mapNotNull { entry ->
                    when {
                        NAME.matches(entry) && custom != null -> {
                            custom.resolve(entry)
                        }

                        entry.startsWith("/") -> {
                            Path.of(entry).normalize()
                        }

                        else -> {
                            null.also {
                                log.error(
                                    "game extension {} is neither a name nor an absolute folder",
                                    entry,
                                )
                            }
                        }
                    }
                }.distinct()

        private fun loadOne(
            folder: Path,
            serverVersion: String,
        ): Found {
            val manifest = ExtensionManifest.read(folder)
            need(manifest.server.isEmpty() || ServerVersions.holds(serverVersion, manifest.server) != false) {
                "${manifest.name} needs server ${manifest.server}, this is $serverVersion"
            }
            val jar = folder.resolve(manifest.jar)
            need(Files.isRegularFile(jar)) { "${manifest.name}: ${manifest.jar} is missing" }
            val loader = URLClassLoader(arrayOf(jar.toUri().toURL()), GameExtensions::class.java.classLoader)
            val type = Class.forName(manifest.main, true, loader)
            need(GameExtension::class.java.isAssignableFrom(type)) {
                "${manifest.name}: ${manifest.main} is no ${GameExtension::class.java.name}"
            }
            val extension = type.getDeclaredConstructor().newInstance() as GameExtension
            return Found(manifest, folder, extension)
        }
    }
}
