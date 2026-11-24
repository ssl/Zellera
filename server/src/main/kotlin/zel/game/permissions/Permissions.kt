package zel.game.permissions

import zel.game.players.Player

/** Answers what a player may do, by their rank. */
class Permissions internal constructor(
    val ranks: RankBook,
    val rankLog: RankLog,
    private val effectMinRanks: Map<Int, Int>,
    private val competitionVoteMinRank: () -> Int = { STANDARD_VOTE_MIN_RANK },
) {
    fun allows(
        player: Player?,
        key: String?,
    ): Boolean = player != null && holds(player.profile.rank, key)

    fun allows(
        rankId: Int,
        key: String,
    ): Boolean = ranks[rankId]?.let { holds(it, key) } == true

    /** The competition vote is held from a rank on, by setting; every other key is granted rank by rank. */
    private fun holds(
        rank: Rank,
        key: String?,
    ): Boolean =
        if (key == Privilege.PERK_VOTE_IN_COMPETITIONS.key) rank.id >= competitionVoteMinRank() else rank.allows(key)

    fun outranks(
        rankId: Int,
        otherRankId: Int,
    ): Boolean = ranks.outranks(rankId, otherRankId)

    fun blocksEffect(
        effectId: Int,
        rankId: Int,
    ): Boolean = (effectMinRanks[effectId] ?: return false) > rankId

    companion object {
        const val VOTE_MIN_RANK_KEY = "room.competition.vote.min_rank"
        const val STANDARD_VOTE_MIN_RANK = 0
    }
}
