package zel.protocol.navigator

import zel.db.Database
import zel.db.DeadlockRetry
import zel.db.orFallback
import zel.game.guilds.Guild
import zel.game.guilds.GuildDirectory
import zel.net.clients.ClientConnection
import zel.room.port.RoomPort
import org.slf4j.LoggerFactory

class RoomDeletion(
    private val guilds: GuildDirectory,
    private val database: Database,
    private val live: () -> RoomPort,
) {
    fun delete(
        client: ClientConnection,
        roomId: Int,
    ) {
        val player = client.player ?: return
        val port = live()
        val room = port.summary(roomId) ?: return
        if (room.ownerId != player.profile.id) {
            client.warnings.refused("not the owner")
        } else if (roomId != player.profile.homeRoomId && port.mayDelete(roomId) &&
            port.emptyForDelete(roomId, player)
        ) {
            port.close(roomId)
            if (!remove(roomId)) return
            if (room.groupId > 0) homeGuild(roomId, guilds.byId(room.groupId))?.let(guilds::delete)
        }
    }

    /** False when the row stays, so its guild stays with it. */
    private fun remove(roomId: Int): Boolean =
        LOGGER.orFallback("deleting room $roomId", false) {
            DeadlockRetry.run { database.queries.write(DELETE_ROOM) { int(roomId) } }
            true
        }

    internal companion object {
        fun homeGuild(
            roomId: Int,
            guild: Guild?,
        ): Guild? = guild?.takeIf { it.roomId == roomId }

        private val LOGGER = LoggerFactory.getLogger(RoomDeletion::class.java)
        private const val DELETE_ROOM = "DELETE FROM rooms WHERE id = ?"
    }
}
