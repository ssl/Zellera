package zel.wired.variable.addon

/**
 * The part of a said line standing on a `#(name)` marker of a says trigger's text. A whole-line trigger needs the
 * line to be the text with the part in the marker's place; a contains trigger finds the text before the marker
 * anywhere in the line ("I put 7 now" against `put #(n)` gives 7).
 */
object TextCapture {
    private val MARKER = Regex("""#\(([^)]*)\)""")

    /** The part of [line] the `#(name)` marker of [pattern] stands on, or null when the line does not match. */
    fun part(
        pattern: String,
        name: String,
        line: String,
        contains: Boolean = false,
        inputs: Set<String>? = null,
    ): String? {
        val (regex, group) = regex(pattern, name, contains, number = false, inputs) ?: return null
        return regex.find(line)?.groupValues?.get(group)
    }

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
        val (regex, group) = regex(pattern, name, contains = true, number = true, inputs) ?: return null
        return regex
            .find(line)
            ?.groupValues
            ?.get(group)
            ?.toLongOrNull()
    }

    /**
     * The pattern as one regex and the group of the first marker named [name]. Whole-line mode anchors both ends;
     * contains mode anchors neither, except that a part with no text after it runs to the end of the line. With
     * [number] the named marker takes a sign and digits only. A marker no input of [inputs] names is plain text.
     */
    private fun regex(
        pattern: String,
        name: String,
        contains: Boolean,
        number: Boolean,
        inputs: Set<String>?,
    ): Pair<Regex, Int>? {
        val markers =
            MARKER.findAll(pattern).toList().filter { marker ->
                inputs == null || inputs.any { it.equals(marker.groupValues[1], ignoreCase = true) }
            }
        val target = markers.indexOfFirst { it.groupValues[1].equals(name, ignoreCase = true) }
        if (target < 0) return null
        val regex = StringBuilder(if (contains) "" else "^")
        var at = 0
        markers.forEachIndexed { index, marker ->
            regex.append(Regex.escape(pattern.substring(at, marker.range.first)))
            regex.append(if (number && index == target) "([+-]?[0-9]+)" else "(.+?)")
            at = marker.range.last + 1
        }
        val tail = pattern.substring(at)
        regex.append(Regex.escape(tail))
        if (!contains || (tail.isEmpty() && !number)) regex.append('$')
        return Regex(regex.toString(), RegexOption.IGNORE_CASE) to target + 1
    }
}
