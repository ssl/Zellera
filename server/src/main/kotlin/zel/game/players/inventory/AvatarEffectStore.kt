package zel.game.players.inventory

import org.slf4j.LoggerFactory
import zel.db.Params
import zel.db.Sql
import zel.db.SqlFailure
import zel.db.first
import zel.db.write
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(AvatarEffectStore::class.java)

private const val EFFECTS_SQL =
    "SELECT effect_id, duration_seconds, activated_at, remaining FROM player_effects WHERE player_id = ?"
private const val EFFECT_SQL = "$EFFECTS_SQL AND effect_id = ?"
private const val ADD_SQL =
    "INSERT INTO player_effects (player_id, effect_id, duration_seconds, activated_at, remaining) VALUES (?, ?, ?, ?, ?)"
private const val WRITE_SQL =
    "UPDATE player_effects SET duration_seconds = ?, activated_at = ?, remaining = ? WHERE player_id = ? AND effect_id = ?"
private const val ACTIVATE_SQL =
    "UPDATE player_effects SET activated_at = ? WHERE player_id = ? AND effect_id = ? AND activated_at IS NULL"
private const val REMOVE_SQL = "DELETE FROM player_effects WHERE player_id = ? AND effect_id = ?"

/** A bought copy is added to the row; a permanent row stays permanent, and a permanent purchase makes it one. */
private const val BUY_SQL =
    "INSERT INTO player_effects (player_id, effect_id, duration_seconds, remaining) VALUES (?, ?, ?, 1) " +
        "ON DUPLICATE KEY UPDATE " +
        "remaining = IF(duration_seconds <= 0 OR VALUES(duration_seconds) <= 0, 1, remaining + 1), " +
        "activated_at = IF(duration_seconds <= 0 OR VALUES(duration_seconds) <= 0, NULL, activated_at), " +
        "duration_seconds = IF(duration_seconds <= 0 OR VALUES(duration_seconds) <= 0, 0, VALUES(duration_seconds))"

internal class AvatarEffectStore(
    pool: DataSource,
) {
    private val sql = Sql(pool)

    /** The columns of one row after the key, in the order [ADD_SQL] and [WRITE_SQL] take them. */
    private fun Params.values(effect: AvatarEffect) {
        int(effect.duration)
        intOrNullWhenNotPositive(effect.activatedAt)
        int(effect.copies)
    }

    private fun Params.key(effect: AvatarEffect) {
        int(effect.playerId)
        int(effect.id)
    }

    fun allOf(playerId: Int): List<AvatarEffect> =
        try {
            sql.select(EFFECTS_SQL) { int(playerId) }.rows { effectOf(playerId, it) }
        } catch (e: SqlFailure) {
            log.error("effects of player {} not read", playerId, e)
            emptyList()
        }

    fun add(effect: AvatarEffect) =
        writeLogged(ADD_SQL, effect, "stored") {
            key(effect)
            values(effect)
        }

    fun write(effect: AvatarEffect) =
        writeLogged(WRITE_SQL, effect, "written") {
            values(effect)
            key(effect)
        }

    fun activate(effect: AvatarEffect) =
        writeLogged(ACTIVATE_SQL, effect, "activated") {
            intOrNullWhenNotPositive(effect.activatedAt)
            key(effect)
        }

    fun remove(effect: AvatarEffect) = writeLogged(REMOVE_SQL, effect, "deleted") { key(effect) }

    private fun writeLogged(
        sql: String,
        effect: AvatarEffect,
        outcome: String,
        bind: Params.() -> Unit,
    ) {
        try {
            this.sql.write(sql, bind)
        } catch (e: SqlFailure) {
            log.error("effect {} of player {} not {}", effect.id, effect.playerId, outcome, e)
        }
    }

    companion object {
        @Throws(SQLException::class)
        fun addBoughtCopy(
            connection: Connection,
            playerId: Int,
            effectId: Int,
            duration: Int,
        ): AvatarEffect {
            connection.write(BUY_SQL) {
                int(playerId)
                int(effectId)
                int(duration)
            }
            val stored =
                connection.first(EFFECT_SQL, {
                    int(playerId)
                    int(effectId)
                }) { effectOf(playerId, it) }
            return stored ?: AvatarEffect(effectId, playerId, duration)
        }

        private fun effectOf(
            playerId: Int,
            result: ResultSet,
        ): AvatarEffect {
            val activatedAt = result.getInt("activated_at")
            return AvatarEffect(
                id = result.getInt("effect_id"),
                playerId = playerId,
                duration = result.getInt("duration_seconds"),
                activatedAt = if (activatedAt <= 0) AvatarEffect.NOT_ACTIVATED else activatedAt,
                copies = result.getInt("remaining"),
            )
        }
    }
}
