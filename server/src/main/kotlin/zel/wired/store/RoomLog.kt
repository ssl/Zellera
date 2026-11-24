package zel.wired.store

import java.time.Instant
import java.util.Locale
import java.util.Objects

class RoomLog(
    private val roomId: Int,
) {
    data class Line(
        val level: Int,
        val source: Int,
        val text: String,
        val atMillis: Long,
    )

    data class Page(
        val total: Int,
        val lines: List<Pair<Long, Line>>,
    )

    private class Entry(
        val ordinal: Int,
        val line: Line,
    )

    private val lines = ArrayDeque<Entry>()
    private var written = 0

    fun add(line: Line) {
        lines.addFirst(Entry(++written, line))
        if (lines.size > LIMIT) lines.removeLast()
    }

    fun page(
        page: Int,
        size: Int,
        level: Int,
        source: Int,
        query: String,
    ): Page {
        val needle = query.lowercase(Locale.ROOT)
        val found =
            lines.filter {
                (level < 0 || it.line.level == level) &&
                    (source < 0 || it.line.source == source) &&
                    needle in it.line.text.lowercase(Locale.ROOT)
            }
        val first = (page - 1).toLong() * size
        val shown =
            found
                .asSequence()
                .withIndex()
                .filter { it.index >= first && it.index < first + size }
                .map { id(it.value) to it.value.line }
                .toList()
        return Page(found.size, shown)
    }

    private fun id(entry: Entry): Long =
        Objects.hash(roomId, entry.ordinal, Instant.ofEpochMilli(entry.line.atMillis)).toLong()

    private companion object {
        const val LIMIT = 200
    }
}
