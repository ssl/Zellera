package zel.ext

interface PlayerValues {
    /** Null while the player's numbers still load. */
    fun get(
        playerId: Int,
        name: String,
    ): Long?

    fun set(
        playerId: Int,
        name: String,
        value: Long,
    )

    fun add(
        playerId: Int,
        name: String,
        delta: Long,
    ): Long?

    fun prefetch(playerId: Int)
}
