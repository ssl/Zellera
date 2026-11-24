package zel.game.permissions

/** Immutable; a reload builds a new one. */
class RankBook(
    private val ranks: Map<Int, Rank>,
) {
    private val byBadge: Map<String, List<Rank>> = ranks.values.filter { it.badge.isNotEmpty() }.groupBy { it.badge }

    val all: Collection<Rank> get() = ranks.values

    val staffBadges: Set<String> get() = byBadge.keys

    operator fun get(rankId: Int): Rank? = ranks[rankId]

    operator fun contains(rankId: Int): Boolean = rankId in ranks

    fun withBadge(code: String): List<Rank> = byBadge[code].orEmpty()

    private val topId: Int = ranks.keys.maxOrNull() ?: 0

    /** A higher rank outranks a lower one; the top rank also reaches its peers, who have nobody above them. */
    fun outranks(
        rankId: Int,
        otherRankId: Int,
    ): Boolean = rankId > otherRankId || (rankId == otherRankId && topId > 0 && rankId >= topId)
}
