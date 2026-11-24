package zel.game.moderation

import java.text.Normalizer

/**
 * Reads a text the way the word filter compares it. Characters commonly written for one another form a group and
 * read as its first one; filtered words are read the same way, so nothing has to guess which letter a look-alike
 * was meant as. One character stays one character, so match ranges map back to the text.
 */
object ChatNormaliser {
    private val groups = listOf("il1|!", "o0", "e3", "a4@", "s5$", "z2", "t7+", "b8", "g9")

    private val groupOf: Map<Char, Char> = groups.flatMap { group -> group.map { it to group.first() } }.toMap()

    fun normalise(text: String): String = buildString(text.length) { text.forEach { append(read(it)) } }

    fun lowercase(text: String): String = buildString(text.length) { text.forEach { append(it.lowercaseChar()) } }

    /** Accents, full-width and other styled forms read as their plain letter or digit first. */
    private fun read(char: Char): Char {
        val base = Normalizer.normalize(char.toString(), Normalizer.Form.NFKD).first()
        val plain = (if (base.isLetterOrDigit()) base else char).lowercaseChar()
        return groupOf[plain] ?: plain
    }
}
