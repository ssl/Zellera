package zel.game.treasurehunt

import zel.db.SqlFailure
import zel.game.items.Items
import zel.game.players.Player
import zel.game.players.Players
import zel.protocol.treasurehunt.TreasureHuntMessages
import org.slf4j.LoggerFactory
import java.util.function.IntSupplier

class TreasureHunts(
    private val records: HuntRecords,
    items: Items,
    private val players: Players,
    private val messages: TreasureHuntMessages,
    private val unixTime: IntSupplier,
) {
    private val prizes = HuntPrizes(items)

    @Volatile
    private var season = HuntSeason.EMPTY

    /** A season the database cannot give leaves the hunts off. */
    fun reload() {
        season =
            try {
                HuntSeason(records.openHunts())
            } catch (failure: SqlFailure) {
                LOGGER.warn("Treasure hunts could not be read; the hunts stay off", failure)
                HuntSeason.EMPTY
            }
    }

    fun dispose() {
        season = HuntSeason.EMPTY
    }

    /** True when [furniId] is a clue of a hunt, whether or not the player was let in. */
    fun takeClick(
        player: Player,
        furniId: Int,
    ): Boolean {
        val hidden = season.behind(furniId) ?: return false
        advance(player, hidden)
        return true
    }

    /** A step the client reports under a hunt code; a code no open hunt has gets no answer. */
    fun takeStep(
        player: Player,
        huntCode: String,
        stepCode: String,
    ) {
        season.behind(huntCode, stepCode)?.let { advance(player, it) }
    }

    private fun advance(
        player: Player,
        hidden: HuntSeason.Hidden,
    ) {
        val client = player.client ?: return
        val hunt = hidden.hunt
        if (!hunt.entry.admits(player.accountLevel, player.stats.club.isMember)) {
            messages.failed(client, hunt.code, hunt.entry.level, hunt.entry.clubLevel)
            return
        }
        val standing = records.found(hunt, hidden.clue, player.profile.id, unixTime.asInt)
        messages.update(client, hunt.code, standing.found, hunt.steps, standing.found >= hunt.steps)
        if (standing.finishedNow) prizes.give(player, hunt.prizes)
        if (standing.firstOfAll) announceWinner(hunt, player)
    }

    private fun announceWinner(
        hunt: TreasureHunt,
        winner: Player,
    ) {
        val profile = winner.profile
        messages.firstWinner(
            players.all.mapNotNull { it.client },
            hunt.code,
            profile.id,
            profile.name,
            profile.figure,
            profile.gender.name,
        )
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TreasureHunts::class.java)

        const val SETTING = "treasure_hunt.is_on"
    }
}
