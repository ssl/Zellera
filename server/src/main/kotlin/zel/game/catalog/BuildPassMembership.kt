package zel.game.catalog

import zel.core.rules.rules
import zel.db.Database
import zel.game.players.ClubMembership
import zel.game.players.Player
import zel.game.players.PlayerProfile
import zel.game.players.subscriptions.Subscription
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.room.furni.buildpass.JdbcBuildPassStore

object BuildPassMembership {
    fun secondsLeft(profile: PlayerProfile): Int = secondsLeft(profile.loadStats().club)

    private fun secondsLeft(club: ClubMembership): Int =
        club
            .get(Subscription.BUILD_PASS)
            ?.secondsLeft
            ?.coerceAtLeast(0) ?: 0

    fun furniLimit(profile: PlayerProfile): Int {
        val club = profile.loadStats().club
        return if (secondsLeft(club) > 0) {
            (rules.memberFurniLimit.toLong() + club.buildPassBonusFurni).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
        } else {
            rules.nonMemberFurniLimit
        }
    }

    fun maxFurniLimit(profile: PlayerProfile): Int = maxOf(furniLimit(profile), rules.nonMemberMaxFurniLimit)

    fun monthFurniBonus(days: Int): Int = BuildPassBuyPages.monthFurniBonus(days, rules.monthFurniBonus)

    fun trackedFurniCount(
        database: Database,
        playerId: Int,
    ): Int = JdbcBuildPassStore(database.dataSource, { rules.memberFurniLimit }).account(playerId).furniCount

    fun tellStatus(
        player: Player,
        database: Database,
    ) {
        val client = player.client ?: return
        CatalogReplies.buildPassFurniCount(client, trackedFurniCount(database, player.profile.id))
        CatalogReplies.buildPassStatus(client, player)
    }
}
