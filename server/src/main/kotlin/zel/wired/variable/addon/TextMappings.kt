package zel.wired.variable.addon

import zel.wired.variable.ValueTexts

/** A text connector's lines `<number>=<text>`: only numeric keys stay, a text may be empty. */
object TextMappings {
    private const val SEPARATOR = '='

    fun parse(text: String): List<Pair<Int, String>> =
        text.split('\n').mapNotNull { line ->
            val at = line.indexOf(SEPARATOR)
            if (at < 0) return@mapNotNull null
            line.substring(0, at).toIntOrNull()?.let { it to line.substring(at + 1).trimEnd('\r') }
        }

    fun table(texts: List<String>): ValueTexts? {
        val entries = LinkedHashMap<Int, String>()
        // Of several connectors the upper one names a value.
        texts.flatMap(::parse).forEach { (value, text) -> entries[value] = text }
        return if (entries.isEmpty()) null else ValueTexts(entries.map { it.key to it.value })
    }
}
