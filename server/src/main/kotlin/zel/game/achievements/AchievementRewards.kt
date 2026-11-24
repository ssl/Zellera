package zel.game.achievements

import zel.db.first
import zel.db.insertForId
import zel.db.rows
import zel.db.write
import zel.game.economy.BalanceChange
import zel.game.economy.LedgerTransaction
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant

/**
 * The stored side of an achievement. Progress is a sum the database keeps: every count is added to it, for a
 * player who is away as for one who plays. What a player was handed for it is the row's rewarded level, so a
 * level is owed while the progress reaches past that level and is handed out exactly once.
 */
internal object AchievementRewards {
    data class Badge(
        val id: Int,
        val code: String,
        val slot: Int,
    )

    /** [replacedBadge] is the code of the lower level's badge that [badge] took the place of. */
    data class HandedOut(
        val levels: List<AchievementLevel>,
        val badge: Badge,
        val replacedBadge: String?,
        val score: Int,
        val pay: LevelRewards,
        val walletAfter: List<BalanceChange>,
    )

    data class Settled(
        val progress: Int,
        val handedOut: HandedOut?,
    )

    /** Adds [amount] to the stored progress and nothing else; a finished achievement keeps its progress. */
    @Throws(SQLException::class)
    fun count(
        connection: Connection,
        playerId: Int,
        achievement: Achievement,
        amount: Int,
    ) {
        connection.write(COUNT) {
            int(playerId)
            int(achievement.id)
            int(amount)
            int(achievement.lastLevel?.progressNeeded ?: Int.MAX_VALUE)
        }
    }

    /**
     * Adds [amount] (0: nothing) and hands out every level the stored progress then owes, in one transaction.
     * Read committed keeps the upsert from taking gap locks other players' counts would wait on.
     */
    @Throws(SQLException::class)
    @Suppress("TooGenericExceptionCaught") // any failure rolls back and goes on to the caller
    fun settle(
        connection: Connection,
        playerId: Int,
        hasClub: Boolean,
        achievement: Achievement,
        amount: Int,
    ): Settled {
        val restore = connection.autoCommit
        val isolation = connection.transactionIsolation
        connection.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
        connection.autoCommit = false
        try {
            val settled = Settlement(connection, playerId, hasClub, achievement).run(amount)
            connection.commit()
            return settled
        } catch (cause: Exception) {
            runCatching { connection.rollback() }.onFailure(cause::addSuppressed)
            throw cause
        } finally {
            connection.autoCommit = restore
            connection.transactionIsolation = isolation
        }
    }

    private class Settlement(
        private val db: Connection,
        private val playerId: Int,
        private val hasClub: Boolean,
        private val achievement: Achievement,
    ) {
        fun run(amount: Int): Settled {
            // The ledger locks the player row before anything else; so does whatever may end in the ledger.
            db.first(LOCK_PLAYER, { int(playerId) }) { true }
            if (amount > 0) count(db, playerId, achievement, amount)
            val (progress, rewarded) =
                db.first(STANDING, {
                    int(playerId)
                    int(achievement.id)
                }) { it.getInt("progress") to it.getInt("rewarded_level") } ?: return Settled(0, null)
            val owed = AchievementProgression.owed(achievement, progress, rewarded)
            if (owed.isEmpty()) return Settled(progress, null)
            return Settled(progress, handOut(owed, rewarded))
        }

        private fun handOut(
            owed: List<AchievementLevel>,
            rewarded: Int,
        ): HandedOut {
            val top = owed.last().level
            db.write(MARK_REWARDED) {
                int(top)
                int(playerId)
                int(achievement.id)
            }
            val score = addScore(owed.sumOf { it.scorePoints.toLong() })
            val replaced = if (rewarded > 0) achievement.badgeCode(rewarded) else null
            val badge = badgeOf(achievement.badgeCode(top), replaced)
            val pay = LevelRewards.of(playerId, hasClub, achievement, owed)
            val walletAfter = if (pay.wallet.isEmpty()) emptyList() else LedgerTransaction(db).postAll(pay.wallet)
            pay.bookIncome(db, playerId, Instant.now().epochSecond.toInt())
            return HandedOut(owed, badge, replaced, score, pay, walletAfter)
        }

        private fun addScore(gain: Long): Int {
            db.write(ADD_SCORE) {
                int(playerId)
                int(gain.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            }
            return db.first(SELECT_SCORE, { int(playerId) }) { it.getInt(1) } ?: 0
        }

        /** The lower level's badge becomes the new one and keeps its row and slot; without one a row is added. */
        private fun badgeOf(
            code: String,
            replaced: String?,
        ): Badge {
            val held =
                db.rows(SELECT_BADGES, {
                    int(playerId)
                    string(code)
                    string(replaced ?: code)
                }) { Badge(it.getInt("id"), it.getString("badge_code"), it.getInt("slot")) }
            val same = held.firstOrNull { it.code.equals(code, ignoreCase = true) }
            val lower = held.firstOrNull { it !== same }
            return when {
                same != null -> same.also { lower?.let { gone -> db.write(DELETE_BADGE) { int(gone.id) } } }
                lower != null -> lower.copy(code = code).also { renameBadge(it) }
                else -> Badge(insertBadge(code), code, 0)
            }
        }

        private fun renameBadge(badge: Badge) {
            db.write(RENAME_BADGE) {
                string(badge.code)
                int(badge.id)
            }
        }

        private fun insertBadge(code: String): Int =
            db.insertForId(INSERT_BADGE) {
                int(playerId)
                string(code)
            } ?: throw SQLException("No badge id")
    }

    private const val LOCK_PLAYER = "SELECT id FROM players WHERE id = ? FOR UPDATE"
    private const val COUNT =
        "INSERT INTO player_achievements (player_id, achievement_id, progress) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE progress = " +
            "IF(progress >= ?, progress, LEAST(progress + VALUES(progress), ${Int.MAX_VALUE}))"
    private const val STANDING =
        "SELECT progress, rewarded_level FROM player_achievements " +
            "WHERE player_id = ? AND achievement_id = ? FOR UPDATE"
    private const val MARK_REWARDED =
        "UPDATE player_achievements SET rewarded_level = ? WHERE player_id = ? AND achievement_id = ?"
    private const val ADD_SCORE =
        "INSERT INTO player_stats (player_id, achievement_score) VALUES (?, ?) ON DUPLICATE KEY UPDATE " +
            "achievement_score = LEAST(achievement_score + VALUES(achievement_score), ${Int.MAX_VALUE})"
    private const val SELECT_SCORE = "SELECT achievement_score FROM player_stats WHERE player_id = ?"
    private const val SELECT_BADGES =
        "SELECT id, badge_code, slot FROM player_badges WHERE player_id = ? AND badge_code IN (?, ?) FOR UPDATE"
    private const val RENAME_BADGE = "UPDATE player_badges SET badge_code = ? WHERE id = ?"
    private const val DELETE_BADGE = "DELETE FROM player_badges WHERE id = ?"
    private const val INSERT_BADGE = "INSERT INTO player_badges (player_id, badge_code, slot) VALUES (?, ?, 0)"
}
