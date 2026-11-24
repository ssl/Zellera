package zel.game.achievements

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.DeadlockRetry
import zel.game.achievements.AchievementProgression.Count
import zel.game.players.Player
import zel.game.players.PlayerBadge
import zel.game.players.WalletWrites
import zel.game.players.inventory.BadgeCaches
import zel.protocol.achievements.AchievementMessages
import java.sql.SQLException
import javax.sql.DataSource

/** Counts for a player who is online and hands out the levels reached; callers hold the player's wallet lock. */
internal class AchievementSteps(
    private val dataSource: DataSource,
    private val outbox: AchievementMessages,
    private val announcer: LevelUpAnnouncer,
    private val badgeCaches: BadgeCaches,
) {
    /** A count that reaches no level is one statement; one that does is settled with its rewards. */
    fun count(
        player: Player,
        achievement: Achievement,
        amount: Int,
    ) {
        val count = AchievementProgression.count(achievement, player.stats.progressOf(achievement), amount)
        if (count !is Count.Added) return
        if (count.reachesLevel) {
            settle(player, achievement, amount)
        } else if (added(player, achievement, amount)) {
            player.stats.recordProgress(achievement, count.to)
            tell(player, achievement)
        }
    }

    /** Hands out what the stored progress owes after [amount] more; the session then knows the stored progress. */
    fun settle(
        player: Player,
        achievement: Achievement,
        amount: Int,
    ) {
        val playerId = player.profile.id
        val hasClub = player.stats.club.isMember
        val settled =
            try {
                DeadlockRetry.run {
                    dataSource.connection.use { AchievementRewards.settle(it, playerId, hasClub, achievement, amount) }
                }
            } catch (cause: SQLException) {
                log.error("achievement {} of player {} not settled", achievement.name, playerId, cause)
                return
            }
        val moved = settled.progress != maxOf(0, player.stats.progressOf(achievement))
        if (moved) player.stats.recordProgress(achievement, settled.progress)
        val handedOut = settled.handedOut
        if (handedOut != null) {
            player.stats.tally.achievementScore = handedOut.score
            handedOut.pay.wallet.zip(handedOut.walletAfter).forEach { (entry, balance) ->
                WalletWrites.publish(player, entry.currency, balance.after)
            }
            announcer.announce(player, achievement, handedOut.levels, inHand(player, handedOut), handedOut.pay)
        }
        if (moved || handedOut != null) tell(player, achievement)
    }

    private fun added(
        player: Player,
        achievement: Achievement,
        amount: Int,
    ): Boolean =
        try {
            dataSource.connection.use { AchievementRewards.count(it, player.profile.id, achievement, amount) }
            true
        } catch (cause: SQLException) {
            log.error("achievement {} of player {} not counted", achievement.name, player.profile.id, cause)
            false
        }

    private fun tell(
        player: Player,
        achievement: Achievement,
    ) {
        player.client?.let { outbox.progress(it, AchievementEntries.entry(player, achievement)) }
    }

    private fun inHand(
        player: Player,
        handedOut: AchievementRewards.HandedOut,
    ): PlayerBadge {
        val stored = handedOut.badge
        val badges = player.inventory.badges
        val codes = listOfNotNull(stored.code, handedOut.replacedBadge)
        val held = badges.all.filter { badge -> codes.any { it.equals(badge.code, ignoreCase = true) } }
        held.filter { it.id != stored.id }.forEach(badges::remove)
        val shown =
            held.firstOrNull { it.id == stored.id }?.apply { code = stored.code }
                ?: PlayerBadge(stored.id, stored.code, stored.slot).also(badges::add)
        badgeCaches.invalidate()
        return shown
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(AchievementSteps::class.java)
    }
}
