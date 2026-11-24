package zel.game.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.SqlFailure
import zel.game.players.Player
import javax.sql.DataSource

/** The stored count moves before anything is handed out, so two claims of the last gift cannot both win. */
class ClubGiftClaims(
    private val dataSource: DataSource,
) {
    fun take(player: Player): Boolean {
        val club = player.stats.club
        return synchronized(club) {
            val claimed = club.giftsClaimed
            val taken = club.remainingGifts > 0 && moved(player.profile.id, claimed, claimed + 1)
            if (taken) club.giftsClaimed = claimed + 1
            taken
        }
    }

    /** For a claim that handed out nothing. */
    fun giveBack(player: Player) {
        val club = player.stats.club
        synchronized(club) {
            val claimed = club.giftsClaimed
            if (claimed > 0 && moved(player.profile.id, claimed, claimed - 1)) club.giftsClaimed = claimed - 1
        }
    }

    private fun moved(
        playerId: Int,
        from: Int,
        to: Int,
    ): Boolean =
        try {
            Sql(dataSource).write(MOVE, to, playerId, from) == 1
        } catch (e: SqlFailure) {
            log.error("Club gift count of player {} not stored", playerId, e)
            false
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ClubGiftClaims::class.java)
        const val MOVE = "UPDATE player_stats SET club_gifts_claimed = ? WHERE player_id = ? AND club_gifts_claimed = ?"
    }
}
