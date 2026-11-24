package zel.protocol.outgoing.users

import zel.game.players.inventory.BadgeLeaderboard
import zel.protocol.generated.BadgeLeaderboardResultOut
import zel.protocol.generated.BadgeLeaderboardResultOutCodec
import zel.protocol.outgoing.OutgoingMessage

class BadgeLeaderboardResult(
    private val type: Int,
    private val rarity: Int,
    private val page: Int,
    private val size: Int,
    private val total: Int,
    private val entries: List<BadgeLeaderboard.Entry>,
    private val own: BadgeLeaderboard.Entry?,
    private val ownRank: Int,
) : OutgoingMessage<BadgeLeaderboardResultOut>(BadgeLeaderboardResultOutCodec) {
    private val firstRank = page * size + 1

    override fun message() =
        BadgeLeaderboardResultOut(
            type,
            rarity,
            page,
            size,
            total,
            entries.mapIndexed { index, entry ->
                BadgeLeaderboardResultOut.List1Item(
                    entry.userId,
                    entry.username,
                    entry.figure,
                    firstRank + index,
                    entry.score,
                )
            },
            own != null,
            own?.let {
                BadgeLeaderboardResultOut.Branch1.Then(
                    it.userId,
                    it.username,
                    it.figure,
                    ownRank,
                    it.score,
                )
            } ?: BadgeLeaderboardResultOut.Branch1.Else,
        )
}
