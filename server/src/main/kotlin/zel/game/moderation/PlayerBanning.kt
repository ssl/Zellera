package zel.game.moderation

import zel.core.Configuration
import zel.core.threading.GameThreads
import zel.game.players.DeviceReading
import zel.game.players.Player
import zel.game.players.Players
import zel.net.clients.ClientConnection
import zel.net.clients.ClientConnections
import zel.protocol.outgoing.handshake.DisconnectReason
import zel.protocol.outgoing.users.BanInfo
import javax.sql.DataSource

@Suppress("LongParameterList") // the services a ban reaches
class PlayerBanning(
    dataSource: DataSource,
    private val players: Players,
    private val reach: StaffReach,
    private val standing: StandingCounters,
    private val clients: () -> ClientConnections,
    private val threading: GameThreads,
    private val notice: SanctionNotice = SanctionNotice.PLAIN,
    config: Configuration = Configuration(emptyMap()),
    private val clock: () -> Int,
) {
    private val store = BanStore(dataSource)
    private val devices = DeviceBans(dataSource, config)

    /**
     * Bans the player for [seconds], disconnecting them if online, and on a game thread the players online on their
     * devices; empty if [moderator] may not or [seconds] <= 0.
     */
    fun ban(
        playerId: Int,
        moderator: Player?,
        reason: String?,
        seconds: Int,
        topicId: Int,
    ): List<PlayerBan> {
        if (seconds <= 0 || moderator == null || !reach.canModerate(moderator, playerId)) return emptyList()
        if (players.profileOf(playerId) == null) return emptyList()
        val now = clock()
        val ban =
            PlayerBan(
                playerId = playerId,
                issuedBy = moderator.profile.id,
                issuedAt = now,
                endsAt = minOf(now.toLong() + seconds, Int.MAX_VALUE.toLong()).toInt(),
                reason = reason.orEmpty(),
                topicId = topicId,
            )
        if (!store.insert(ban)) return emptyList()
        standing.count(playerId, StandingCounter.BANS_RECEIVED)
        players.find(playerId)?.client?.let { client -> disconnect(client, ban, now) }
        threading.run { disconnectDevicesOf(ban, now) }
        return listOf(ban)
    }

    fun checkForBan(playerId: Int): PlayerBan? = store.runningFor(playerId, clock())

    fun kickBanned(playerId: Int): Boolean {
        val now = clock()
        val ban = checkForBan(playerId)
        val client = players.find(playerId)?.client
        if (ban != null && client != null) disconnect(client, ban, now)
        threading.run { kickDevices(now) }
        return ban != null && client != null
    }

    private fun kickDevices(now: Int) =
        players.all.forEach { player ->
            val id = player.profile.id
            val device = player.device
            if (device != null && checkForBan(id) == null && devices.refusal(id, device, now) != null) {
                player.client?.let { client -> clients().disconnect(client, DisconnectReason.JUST_BANNED) }
            }
        }

    fun banInfo(ban: PlayerBan): BanInfo = notice.ban(ban, clock())

    fun hasMachineBan(client: ClientConnection?): Boolean {
        val machineId = client?.machineId.orEmpty()
        return machineId.isNotEmpty() && store.barsMachine(machineId, clock())
    }

    fun hasIpBan(ip: String): Boolean = ip.isNotEmpty() && store.barsAddress(ip, clock())

    /** What refuses [device] for [playerId]: `rule 1`, `rule 2` or `pattern <number>`; null if nothing does. */
    fun deviceBan(
        playerId: Int,
        device: DeviceReading,
    ): String? = devices.refusal(playerId, device, clock())

    /** The players online on a device of the banned one, whom the device rules keep out from now on. */
    private fun disconnectDevicesOf(
        ban: PlayerBan,
        now: Int,
    ) = devices.keptOutBy(ban.playerId, players.all, now).forEach { player ->
        // without the notice: the reason and the end are the banned player's own
        player.client?.let { client -> clients().disconnect(client, DisconnectReason.JUST_BANNED) }
    }

    private fun disconnect(
        client: ClientConnection,
        ban: PlayerBan,
        now: Int,
    ) {
        client.send(notice.ban(ban, now))
        threading.run({ clients().disconnect(client, DisconnectReason.JUST_BANNED) }, DISCONNECT_DELAY_MILLIS)
    }

    private companion object {
        const val DISCONNECT_DELAY_MILLIS = 1000L
    }
}
