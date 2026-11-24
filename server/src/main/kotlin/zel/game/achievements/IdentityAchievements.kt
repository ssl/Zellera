package zel.game.achievements

import zel.game.players.ChatStyles
import zel.game.players.Player
import zel.game.players.WalletWrites
import zel.game.players.subscriptions.Subscription
import zel.util.SECONDS_PER_DAY

/** Achievements that mirror an account fact; progress only rises. */
class IdentityAchievements(
    private val progress: AchievementProgress,
    private val unixTime: () -> Int,
    private val ownedChatStyles: (Int) -> Collection<Int>,
) {
    fun atLogin(player: Player?) {
        mirrorBuildPass(player, announce = true)
        mirrorChatStyles(player)
    }

    /** With [announce] the achievement's state goes out even when nothing moved. */
    fun mirrorBuildPass(
        player: Player?,
        announce: Boolean,
    ) = mirror(player, GameEvents.CLUB_BUILD_PASS_DAYS, announce) { membershipDay(memberships(it), unixTime()) }

    fun mirrorChatStyles(player: Player?) =
        mirror(player, GameEvents.CHAT_STYLES_OWNED, false) { boughtStyles(ownedChatStyles(it.profile.id)) }

    fun giftsClaimed(
        player: Player?,
        count: Int,
    ) {
        if (count > 0) progress.count(player, GameEvents.EARNINGS_DAILY_GIFT, count)
    }

    private fun mirror(
        player: Player?,
        event: String,
        announce: Boolean,
        fact: (Player) -> Int,
    ) {
        if (player?.client == null) return
        progress.forEvent(event).forEach { mirror(player, it, announce) { fact(player) } }
    }

    internal fun mirror(
        player: Player,
        achievement: Achievement,
        announce: Boolean,
        fact: () -> Int,
    ) = progress.raiseTo(player, achievement, announce, fact)

    private fun memberships(player: Player): List<Subscription> {
        val stats = player.stats
        return synchronized(stats) { stats.club.subscriptions.toList() }
    }

    companion object {
        /** Which day of the Build Pass membership running at [now] it is: 1 on its first second, 0 without one. */
        internal fun membershipDay(
            running: Collection<Subscription?>,
            now: Int,
        ): Int {
            val since = running.filterNotNull().filter { it.coversClubAt(now) }.minOfOrNull { it.startedAt } ?: return 0
            return ((now.toLong() - since) / SECONDS_PER_DAY + 1).toInt()
        }

        private fun Subscription.coversClubAt(now: Int): Boolean =
            isActive &&
                type.equals(Subscription.BUILD_PASS, ignoreCase = true) &&
                startedAt <= now &&
                now < startedAt + maxOf(0, duration).toLong()

        internal fun boughtStyles(owned: Collection<Int?>): Int =
            owned.count { it != null && (it in ChatStyles.SHOP_STYLES || it in ChatStyles.NFT_STYLES) }

        /** [reached] is -1 for no progress. */
        internal fun gap(
            reached: Int,
            target: Int,
        ): Int = maxOf(0, target - maxOf(0, reached))
    }
}

/** Runs under the wallet lock so concurrent raises serialize. */
internal fun AchievementProgress.raiseTo(
    player: Player,
    achievement: Achievement,
    announce: Boolean,
    target: () -> Int,
) = WalletWrites.locked(player) {
    val had = player.stats.progressOf(achievement)
    val owed = IdentityAchievements.gap(had, target())
    if (owed > 0) progress(player, achievement, owed)
    if (announce && player.stats.progressOf(achievement) == had) announce(player, achievement)
}
