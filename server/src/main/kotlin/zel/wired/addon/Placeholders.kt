package zel.wired.addon

import zel.wired.firing.Firing

object Placeholders {
    /**
     * Most characters a placeholder value or a filled text keeps; building stops there. 20 times a show message's
     * 400 characters, and at 3 UTF-8 bytes a character still well inside one wire string.
     */
    const val MAX_TEXT = 8_000

    fun values(firing: Firing): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        firing.addons.filterIsInstance<PlaceholderBox>().forEach { box ->
            val key = box.name.lowercase()
            if (key.isNotEmpty() && key !in values) box.text(firing)?.let { values[key] = cap(it) }
        }
        return values
    }

    fun fill(
        firing: Firing,
        text: String,
    ): String {
        if (!text.contains("$(")) return text
        return replace(text, PATTERN, values(firing))
    }

    /** [text] with each match of [pattern] whose first group names one of [values] replaced, cut at [MAX_TEXT]. */
    fun replace(
        text: String,
        pattern: Regex,
        values: Map<String, String>,
    ): String {
        val out = StringBuilder()
        var last = 0
        for (match in pattern.findAll(text)) {
            if (!out.appendCapped(text, last, match.range.first)) return out.cut()
            val value = values[match.groupValues[1].lowercase()] ?: match.value
            if (!out.appendCapped(value, 0, value.length)) return out.cut()
            last = match.range.last + 1
        }
        out.appendCapped(text, last, text.length)
        return out.cut()
    }

    /** [values] joined by [delimiter], null when there are none; stops taking values at [MAX_TEXT]. */
    fun join(
        values: Sequence<String>,
        delimiter: String,
    ): String? {
        val out = StringBuilder()
        var any = false
        for (value in values) {
            if (any && !out.appendCapped(delimiter, 0, delimiter.length)) break
            any = true
            if (!out.appendCapped(value, 0, value.length)) break
        }
        return if (any) out.cut() else null
    }

    fun cap(text: String): String =
        if (text.length <= MAX_TEXT) text else StringBuilder().append(text, 0, MAX_TEXT).cut()

    /** Appends up to the cap; false once the cap is reached. */
    private fun StringBuilder.appendCapped(
        text: CharSequence,
        start: Int,
        end: Int,
    ): Boolean {
        val room = MAX_TEXT - length
        append(text, start, minOf(end, start + room))
        return end - start < room
    }

    /** A cut never leaves half a surrogate pair at the end. */
    private fun StringBuilder.cut(): String {
        if (length == MAX_TEXT && this[length - 1].isHighSurrogate()) setLength(length - 1)
        return toString()
    }

    private val PATTERN = Regex("""\$\(([^()]*)\)""")
}
