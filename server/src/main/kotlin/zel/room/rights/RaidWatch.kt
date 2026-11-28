package zel.room.rights

import zel.core.Configuration
import zel.game.players.Player
import zel.net.server.proxy.ClientAddress
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.entry.Pending
import zel.room.entry.RoomLeaving
import zel.room.event.RoomEvent
import zel.room.unit.UnitKind
import zel.security.Suspicion
import zel.security.SuspicionLog
import java.util.concurrent.ConcurrentHashMap

class RaidWatch(
    private val store: RightsStore,
    private val config: Configuration,
    private val leaving: RoomLeaving,
    private val online: (Int) -> Player?,
    private val persistInOrder: (Any, Runnable) -> Unit,
    private val nowSeconds: () -> Int,
    private val suspicions: SuspicionLog,
    private val remove: (RoomScope, Int, Int?) -> Unit,
) : RoomPart {
    private val rooms = ConcurrentHashMap<Room, Watch>()

    val listener =
        RoomListener { scope, event ->
            when (event) {
                is RoomEvent.Entered -> playerAt(scope, event.unitIndex)?.let { entered(scope, it) }
                is RoomEvent.Says -> playerAt(scope, event.unitIndex)?.let { said(scope, it, event.text) }
                else -> Unit
            }
        }

    override fun load(room: Room) {
        val roomId = room.info.id
        rooms[room] = Watch(store.raid(roomId) ?: RaidSettings.DEFAULT, store.lastRaid(roomId))
    }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
    }

    fun saved(
        roomId: Int,
        settings: RaidSettings,
    ) {
        watchOf(roomId)?.settings = settings
    }

    fun active(roomId: Int): Boolean = (watchOf(roomId)?.incidentUntil ?: 0) > nowSeconds()

    fun lastRaid(roomId: Int): Int = watchOf(roomId)?.lastRaid ?: store.lastRaid(roomId)

    fun refuses(
        scope: RoomScope,
        pending: Pending,
    ): Boolean {
        val watch = rooms[scope.room] ?: return false
        val settings = watch.settings
        if (!settings.enabled || !settings.guardEnabled || nowSeconds() >= watch.guardUntil) return false
        val visitor = pending.visitor
        if (visitor.entersAnyRoom || visitor.level >= DoorCheck.RIGHTS) return false
        val player = online(pending.card.id) ?: return false
        val trusted = visitor.groupMember || player.messenger.friends.containsKey(scope.room.info.ownerId)
        val fresh = isNew(player)
        return when (settings.guardSensitivity) {
            LOW -> fresh
            MEDIUM -> fresh || !trusted
            else -> !trusted
        }
    }

    private fun playerAt(
        scope: RoomScope,
        unitIndex: Int,
    ): Player? {
        val kind = scope.room.units[unitIndex]?.kind as? UnitKind.Player ?: return null
        return online(kind.playerId)
    }

    private fun entered(
        scope: RoomScope,
        player: Player,
    ) {
        val watch = rooms[scope.room] ?: return
        if (exempt(scope, player)) return
        val client = player.client
        val address = client?.let { ClientAddress.of(it.channel) }
        val device = player.device?.usable("id")
        watch.arrivals += Arrival(player.profile.id, nowSeconds(), address, device)
        look(scope, watch)
    }

    private fun said(
        scope: RoomScope,
        player: Player,
        text: String,
    ) {
        val watch = rooms[scope.room] ?: return
        val id = player.profile.id
        if (watch.arrivals.none { it.playerId == id }) return
        val line = text.lowercase().replace(NOT_WORD, " ").trim()
        if (line.isEmpty()) return
        watch.lines += Line(id, line, nowSeconds())
        look(scope, watch)
    }

    private fun exempt(
        scope: RoomScope,
        player: Player,
    ): Boolean {
        val id = player.profile.id
        val owner = scope.room.info.ownerId
        return id == owner ||
            leaving.doorOf(scope.room).levelOf(id) >= DoorCheck.RIGHTS ||
            player.messenger.friends.containsKey(owner)
    }

    private fun look(
        scope: RoomScope,
        watch: Watch,
    ) {
        val now = nowSeconds()
        val since = now - setting(WINDOW_KEY, WINDOW_SECONDS)
        watch.arrivals.removeIf { it.at < since }
        watch.lines.removeIf { it.at < since }
        val suspects =
            watch.arrivals
                .map { it.playerId }
                .distinct()
                .filter { suspicious(watch, it) }
        if (suspects.size >= threshold(watch.settings.sensitivity)) raid(scope, watch, suspects, now)
    }

    private fun suspicious(
        watch: Watch,
        playerId: Int,
    ): Boolean {
        val player = online(playerId)
        if (player != null && isNew(player)) return true
        val sameLine = setting(SAME_LINE_KEY, SAME_LINE_PLAYERS)
        val repeats =
            watch.lines.filter { it.playerId == playerId }.any { own ->
                watch.lines
                    .filter { it.text == own.text }
                    .map { it.playerId }
                    .distinct()
                    .size >= sameLine
            }
        if (repeats) return true
        val mine = watch.arrivals.filter { it.playerId == playerId }
        return watch.arrivals.any { other ->
            other.playerId != playerId &&
                mine.any {
                    (it.address != null && it.address == other.address) ||
                        (it.device != null && it.device == other.device)
                }
        }
    }

    private fun raid(
        scope: RoomScope,
        watch: Watch,
        suspects: List<Int>,
        now: Int,
    ) {
        val starting = now >= watch.incidentUntil
        if (starting) {
            watch.handled.clear()
            watch.recorded.clear()
        }
        watch.incidentUntil = now + setting(WINDOW_KEY, WINDOW_SECONDS)
        suspects.filter { watch.recorded.add(it) }.forEach { note(scope, watch, it) }
        val settings = watch.settings
        if (!settings.enabled) {
            if (starting) tell(scope, SHADOW_ALERT)
            return
        }
        if (starting) {
            watch.lastRaid = now
            val roomId = scope.room.info.id
            persistInOrder(RaidKey(roomId), Runnable { store.saveLastRaid(roomId, now) })
            if (settings.guardEnabled) watch.guardUntil = now + settings.guardSeconds
            tell(scope, ACTIVE_ALERT)
        }
        val ban = if (settings.actionType == BAN_ACTION) settings.banSeconds else null
        suspects.filter { watch.handled.add(it) }.forEach { remove(scope, it, ban) }
    }

    private fun note(
        scope: RoomScope,
        watch: Watch,
        playerId: Int,
    ) {
        val arrival = watch.arrivals.lastOrNull { it.playerId == playerId }
        suspicions.record(playerId, arrival?.address, arrival?.device, Suspicion.RAID, "room ${scope.room.info.id}")
    }

    private fun tell(
        scope: RoomScope,
        type: String,
    ) {
        val notice = NotificationDialogOut(type, emptyList())
        val owner = scope.room.info.ownerId
        val door = leaving.doorOf(scope.room)
        val told = door.present.filter { (id, present) -> id != owner && present.level >= DoorCheck.RIGHTS }.keys
        told.forEach { id -> online(id)?.client?.send(NotificationDialogOutCodec, notice) }
        online(owner)?.client?.send(NotificationDialogOutCodec, notice)
    }

    private fun isNew(player: Player): Boolean =
        player.profile.createdAt > nowSeconds() - setting(NEW_ACCOUNT_KEY, NEW_ACCOUNT_HOURS) * SECONDS_PER_HOUR

    private fun threshold(sensitivity: Int): Int =
        when (sensitivity) {
            LOW -> setting(SUSPECTS_LOW_KEY, SUSPECTS_LOW)
            MEDIUM -> setting(SUSPECTS_MEDIUM_KEY, SUSPECTS_MEDIUM)
            else -> setting(SUSPECTS_HIGH_KEY, SUSPECTS_HIGH)
        }.coerceAtLeast(1)

    private fun setting(
        key: String,
        default: Int,
    ): Int = config.getInt(key, default)

    private fun watchOf(roomId: Int): Watch? = rooms.entries.firstOrNull { it.key.info.id == roomId }?.value

    private class Watch(
        @Volatile var settings: RaidSettings,
        @Volatile var lastRaid: Int,
    ) {
        val arrivals = ArrayList<Arrival>()
        val lines = ArrayList<Line>()
        val handled = HashSet<Int>()
        val recorded = HashSet<Int>()

        @Volatile
        var incidentUntil = 0

        @Volatile
        var guardUntil = 0
    }

    private data class Arrival(
        val playerId: Int,
        val at: Int,
        val address: String?,
        val device: String?,
    )

    private data class Line(
        val playerId: Int,
        val text: String,
        val at: Int,
    )

    private data class RaidKey(
        val roomId: Int,
    )

    companion object {
        const val WINDOW_KEY = "room.raid.window.seconds"
        const val SUSPECTS_LOW_KEY = "room.raid.suspects.low"
        const val SUSPECTS_MEDIUM_KEY = "room.raid.suspects.medium"
        const val SUSPECTS_HIGH_KEY = "room.raid.suspects.high"
        const val NEW_ACCOUNT_KEY = "room.raid.new_account.hours"
        const val SAME_LINE_KEY = "room.raid.same_line.players"
        const val WINDOW_SECONDS = 30
        const val SUSPECTS_LOW = 12
        const val SUSPECTS_MEDIUM = 8
        const val SUSPECTS_HIGH = 5
        const val NEW_ACCOUNT_HOURS = 24
        const val SAME_LINE_PLAYERS = 3
        private const val SECONDS_PER_HOUR = 3600
        private const val LOW = 0
        private const val MEDIUM = 1
        private const val BAN_ACTION = 1
        private const val ACTIVE_ALERT = "raid.protection.active_alert"
        private const val SHADOW_ALERT = "raid.protection.shadow_alert"
        private val NOT_WORD = Regex("[^\\p{L}\\p{N}]+")
    }
}
