package zel.wired.score

import zel.protocol.generated.PacketWriter
import zel.protocol.generated.VariableLeaderboardChunkUpdateOut

object LeaderboardRows {
    private const val CHUNK = 50
    const val SORT_VALUE_HIGH = 0
    const val SORT_VALUE_LOW = 1
    const val SORT_OLDEST = 2
    const val SORT_NEWEST = 3
    const val SORT_OLDEST_UPDATE = 4
    const val SORT_NEWEST_UPDATE = 5

    /** Ties go to the earlier update, then the lower user id. */
    fun sorted(
        holders: List<LeaderHolder>,
        sort: Int,
    ): List<LeaderHolder> {
        val ties = compareBy<LeaderHolder> { it.updated }.thenBy { it.userId }
        val highFirst = compareByDescending<LeaderHolder> { it.value ?: Long.MIN_VALUE }
        val order: Comparator<LeaderHolder> =
            when (sort) {
                SORT_VALUE_HIGH -> highFirst
                SORT_VALUE_LOW -> compareBy { it.value ?: Long.MAX_VALUE }
                SORT_OLDEST -> compareBy { it.created }
                SORT_NEWEST -> compareByDescending { it.created }
                SORT_OLDEST_UPDATE -> compareBy { it.updated }
                SORT_NEWEST_UPDATE -> compareByDescending { it.updated }
                else -> highFirst
            }
        return holders.sortedWith(order.then(ties))
    }

    /** A chunk number past the end gets an empty chunk only when it is the first. */
    fun chunks(
        boardId: Int,
        revision: Int,
        variable: String,
        rows: List<LeaderRow>,
        asked: List<Int>,
        shows: (Int) -> Boolean,
    ): List<VariableLeaderboardChunkUpdateOut> {
        val pages = rows.chunked(CHUNK)
        return asked.map { chunk ->
            val page = pages.getOrElse(chunk) { emptyList() }
            val w = PacketWriter().int(page.size)
            page.forEach { row(w, it, shows) }
            VariableLeaderboardChunkUpdateOut(boardId, revision, variable, chunk, rows.size, w.toByteArray())
        }
    }

    private fun row(
        w: PacketWriter,
        row: LeaderRow,
        shows: (Int) -> Boolean,
    ) {
        val holder = row.holder
        w
            .int(row.rank)
            .int(holder.userId)
            .string(holder.name)
            .string(holder.figure)
        w.string(holder.value?.toString().orEmpty())
        w.long(if (shows(LeaderboardForm.CREATION)) holder.created else 0)
        w.long(if (shows(LeaderboardForm.LAST_UPDATE)) holder.updated else 0)
        w.boolean(row.level != null)
        row.level?.let(w::string)
        val text = row.text
        w.boolean(text != null)
        text?.let(w::string)
    }

    fun displayName(name: String): String =
        name.split('_').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}
