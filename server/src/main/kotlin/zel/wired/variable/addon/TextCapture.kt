package zel.wired.variable.addon

/**
 * The part of a said line standing on a `#(name)` marker of a says trigger's text. A whole-line trigger needs the
 * line to be the text with the part in the marker's place; a contains trigger finds the text before the marker
 * anywhere in the line ("I put 7 now" against `put #(n)` gives 7).
 */
object TextCapture {
    private val MARKER = Regex("""#\(([^)]*)\)""")

    private const val BREAKS = "\n\r\u0085  "

    /** The part of [line] the `#(name)` marker of [pattern] stands on, or null when the line does not match. */
    fun part(
        pattern: String,
        name: String,
        line: String,
        contains: Boolean = false,
        inputs: Set<String>? = null,
    ): String? = shape(pattern, name, contains, number = false, inputs)?.find(line)

    /**
     * A number on the marker. In contains mode: a sign and digits right after the text before the marker ("put 7.5"
     * gives 7); no digit there, or a value beyond 64 bits, refuses the line. A whole-line trigger reads the whole
     * marker part as the number (see [part]).
     */
    fun number(
        pattern: String,
        name: String,
        line: String,
        contains: Boolean = false,
        inputs: Set<String>? = null,
    ): Long? {
        if (!contains) return part(pattern, name, line, inputs = inputs)?.toLongOrNull()
        return shape(pattern, name, contains = true, number = true, inputs)?.find(line)?.toLongOrNull()
    }

    private fun shape(
        pattern: String,
        name: String,
        contains: Boolean,
        number: Boolean,
        inputs: Set<String>?,
    ): Shape? {
        val markers =
            MARKER.findAll(pattern).toList().filter { marker ->
                inputs == null || inputs.any { it.equals(marker.groupValues[1], ignoreCase = true) }
            }
        val target = markers.indexOfFirst { it.groupValues[1].equals(name, ignoreCase = true) }
        if (target < 0) return null
        val texts = ArrayList<String>(markers.size + 1)
        var at = 0
        markers.forEach { marker ->
            texts.add(pattern.substring(at, marker.range.first))
            at = marker.range.last + 1
        }
        val tail = pattern.substring(at)
        texts.add(tail)
        return Shape(texts, target, number, fromStart = !contains, toEnd = !contains || (tail.isEmpty() && !number))
    }

    private class Shape(
        val texts: List<String>,
        val target: Int,
        val digits: Boolean,
        val fromStart: Boolean,
        val toEnd: Boolean,
    ) {
        fun find(line: String): String? {
            val n = line.length
            val last = texts.lastIndex
            val free = IntArray(n + 1) { n }
            val digitEnd = IntArray(n + 1) { n }
            for (pos in n - 1 downTo 0) {
                free[pos] = if (line[pos] in BREAKS) pos else free[pos + 1]
                digitEnd[pos] = if (line[pos] in '0'..'9') digitEnd[pos + 1] else pos
            }
            val fits = Array(texts.size) { BooleanArray(n + 1) }
            val reach = IntArray(n + 2) { n + 1 }
            for (i in last downTo 0) {
                val text = texts[i]
                for (pos in 0..n) {
                    val after = pos + text.length
                    val rest =
                        if (i == last) {
                            !toEnd || atEnd(line, after)
                        } else {
                            val ends = partEnds(i, line, after, free, digitEnd)
                            !ends.isEmpty() && reach[ends.first] <= ends.last
                        }
                    fits[i][pos] = after <= n && rest && line.regionMatches(pos, text, 0, text.length, true)
                }
                for (pos in n downTo 0) reach[pos] = if (fits[i][pos]) pos else reach[pos + 1]
            }
            var pos = (if (fromStart) 0..0 else 0..n).firstOrNull { fits[0][it] } ?: return null
            for (i in 0 until last) {
                val start = pos + texts[i].length
                val ends = partEnds(i, line, start, free, digitEnd)
                val number = digits && i == target
                pos = (if (number) ends.reversed() else ends).first { fits[i + 1][it] }
                if (i == target) return line.substring(start, pos)
            }
            return null
        }

        private fun partEnds(
            i: Int,
            line: String,
            start: Int,
            free: IntArray,
            digitEnd: IntArray,
        ): IntRange {
            if (start >= line.length) return IntRange.EMPTY
            if (!digits || i != target) return start + 1..free[start]
            val from = if (line[start] == '+' || line[start] == '-') start + 1 else start
            return if (from >= line.length) IntRange.EMPTY else from + 1..digitEnd[from]
        }

        private fun atEnd(
            line: String,
            at: Int,
        ): Boolean =
            at == line.length ||
                (at == line.length - 1 && line[at] in BREAKS) ||
                (at == line.length - 2 && line[at] == '\r' && line[at + 1] == '\n')
    }
}
