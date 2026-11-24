package zel.game.players

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.SqlFailure
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/** The devices players logged in from: one row per player and reading, counted on every login. */
internal class PlayerDevices(
    private val sql: Sql,
    private val writer: Executor,
) {
    fun loggedIn(
        playerId: Int,
        device: DeviceReading,
    ) {
        try {
            writer.execute { write(playerId, device) }
        } catch (refused: RejectedExecutionException) {
            log.debug("writer refuses the device of player {}; writing it inline", playerId, refused)
            write(playerId, device)
        }
    }

    private fun write(
        playerId: Int,
        device: DeviceReading,
    ) {
        try {
            sql.write(RECORD) {
                int(playerId)
                device.pieces.forEach(::string)
            }
        } catch (e: SqlFailure) {
            log.error("The device of player {} could not be recorded", playerId, e)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PlayerDevices::class.java)
        val RECORD =
            "INSERT INTO player_devices (player_id, ${DeviceReading.NAMES.joinToString { "`$it`" }}, " +
                "first_seen_at, last_seen_at) VALUES (?, ${DeviceReading.NAMES.joinToString { "?" }}, " +
                "UNIX_TIMESTAMP(), UNIX_TIMESTAMP()) " +
                "ON DUPLICATE KEY UPDATE last_seen_at = VALUES(last_seen_at), logins = logins + 1"
    }
}
