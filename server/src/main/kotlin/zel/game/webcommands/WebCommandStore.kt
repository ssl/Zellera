package zel.game.webcommands

import java.sql.SQLException

/** A row is claimed by setting `processed_at` while pending, so only one taker wins. */
interface WebCommandStore {
    @Throws(SQLException::class)
    fun pending(limit: Int): List<WebCommand>

    @Throws(SQLException::class)
    fun claim(
        id: Long,
        now: Int,
    ): Boolean

    @Throws(SQLException::class)
    fun finish(
        id: Long,
        result: WebCommandResult,
        now: Int,
    )

    @Throws(SQLException::class)
    fun failInterrupted(): Int
}
