package zel.game.webcommands

import zel.core.Configuration
import zel.core.threading.GameThreads
import zel.db.first
import zel.game.economy.EconomyLedger
import zel.game.moderation.WordFilter
import zel.game.permissions.PermissionActor
import zel.game.permissions.Permissions
import zel.game.permissions.Privilege
import zel.game.players.Mottos
import zel.game.players.Players
import zel.game.players.inventory.BadgeCaches
import zel.room.port.RoomPort
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

@Suppress("LongParameterList") // the composition point: every service a command reaches
class WebCommandsModule(
    dataSource: DataSource,
    players: Players,
    permissions: Permissions,
    wordFilter: WordFilter,
    config: Configuration,
    threading: GameThreads,
    now: () -> Int,
    ready: () -> Boolean,
    badgeCaches: BadgeCaches,
    accountLevelChanged: (Int) -> Unit,
    reloadTvPlaylists: () -> Unit,
    rooms: () -> RoomPort,
) {
    val processor: WebCommandProcessor

    init {
        val applier =
            WebCommandApplier(
                WebCommandApplier.Services(
                    dataSource = dataSource,
                    online = { id -> players.find(id) },
                    pointTypes = { EconomyLedger.pointTypes().toSet() },
                    mottoMaxLength = { Mottos.maxLength(config) },
                    wordFilter = { text -> wordFilter.filter(text, null) },
                    rankBadges = { permissions.ranks.staffBadges },
                    ranks = GameRanks(dataSource, players, permissions),
                    reloadSettings = { config.reload() },
                    reloadTvPlaylists = reloadTvPlaylists,
                    invalidateBadgeCaches = badgeCaches::invalidate,
                    rooms = rooms,
                    accountLevelChanged = accountLevelChanged,
                ),
            )
        processor =
            WebCommandProcessor(
                JdbcWebCommandStore(dataSource),
                applier::apply,
                now,
                { task, delayMs -> threading.run(task, delayMs) },
                ready,
            )
    }

    private class GameRanks(
        private val dataSource: DataSource,
        private val players: Players,
        private val permissions: Permissions,
    ) : WebCommandApplier.Ranks {
        override fun exists(level: Int): Boolean = level in permissions.ranks

        override fun stored(userId: Int): Int? =
            try {
                dataSource.connection.use { connection -> storedRank(connection, userId) }
            } catch (e: SQLException) {
                throw IllegalStateException(e)
            }

        override fun holds(
            userId: Int,
            right: Privilege,
        ): Boolean = stored(userId)?.let { permissions.allows(it, right.key) } == true

        private fun storedRank(
            connection: Connection,
            userId: Int,
        ): Int? = connection.first(STORED, { int(userId) }) { it.getInt(1) }

        override fun set(
            userId: Int,
            level: Int,
            actorId: Int?,
        ) {
            val actor = actorId ?: PermissionActor.SYSTEM_ID
            val info = actorId?.let { players.profileOf(it) }
            players.changeRank(userId, level, actor, info?.name ?: "housekeeping", WebCommandApplier.VIA_HOUSEKEEPING)
        }

        private companion object {
            const val STORED = "SELECT rank_id FROM players WHERE id = ?"
        }
    }
}
