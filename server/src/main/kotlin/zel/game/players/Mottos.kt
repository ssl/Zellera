package zel.game.players

import zel.core.Configuration

/** A motto change, from the client and from a web command alike: the word filter, then the trim, then the limit. */
object Mottos {
    const val STORE_SQL = "UPDATE players SET motto = ? WHERE id = ?"
    private const val MAX_LENGTH_SETTING = "player.motto.max.characters"
    private const val DEFAULT_MAX_LENGTH = 38

    fun maxLength(config: Configuration): Int = config.getInt(MAX_LENGTH_SETTING, DEFAULT_MAX_LENGTH)

    /** The motto to store for [typed], or null when the filtered, trimmed text is longer than [maxLength]. */
    fun accepted(
        typed: String,
        filter: (String) -> String?,
        maxLength: Int,
    ): String? = filter(typed).orEmpty().trim().takeIf { it.length <= maxLength }
}
