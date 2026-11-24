package zel.game.games.snowball

class SnowballPlayerStats {
    var kills: Int = 0
        internal set

    var deaths: Int = 0
        internal set

    var snowballHits: Int = 0
        internal set
    var snowballHitsTaken: Int = 0
        internal set
    var snowballsThrown: Int = 0
        internal set
    var snowballsCreated: Int = 0
        internal set

    var snowballsFromMachine: Int = 0
        internal set

    var friendlyHits: Int = 0
        internal set

    var treesHit: Int = 0
        internal set

    /** friendlyKills is always 0: a team mate's ball does no damage. */
    fun toWire(score: Int): IntArray =
        intArrayOf(
            score,
            kills,
            deaths,
            snowballHits,
            snowballHitsTaken,
            snowballsThrown,
            snowballsCreated,
            snowballsFromMachine,
            friendlyHits,
            0,
        )
}
