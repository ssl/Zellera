package zel.game.moderation

import java.text.Normalizer

/**
 * Reads a text the way the word filter compares it. Characters commonly written for one another form a group and
 * read as its first one; filtered words are read the same way, so nothing has to guess which letter a look-alike
 * was meant as.
 */
object ChatNormaliser {
    class Folded(
        val text: String,
        val origin: IntArray,
    )

    private val groups = listOf("il1|!", "o0", "e3", "a4@", "s5$", "z2", "t7+", "b8", "g9")

    private val groupOf: Map<Char, Char> = groups.flatMap { group -> group.map { it to group.first() } }.toMap()

    private val homoglyphs =
        mapOf(
            'а' to 'a',
            'в' to 'b',
            'с' to 'c',
            'е' to 'e',
            'н' to 'h',
            'і' to 'i',
            'ј' to 'j',
            'к' to 'k',
            'м' to 'm',
            'о' to 'o',
            'р' to 'p',
            'ԛ' to 'q',
            'т' to 't',
            'у' to 'y',
            'х' to 'x',
            'ѕ' to 's',
        )

    private val ignored = setOf(0x200B, 0x200C, 0x200D, 0x2060, 0xFEFF, 0x00AD)

    fun normalise(text: String): Folded {
        val out = StringBuilder(text.length)
        val origin = ArrayList<Int>(text.length + 1)
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val width = Character.charCount(codePoint)
            if (codePoint !in ignored) {
                val plain = read(codePoint, width)
                if (plain != null) {
                    out.append(plain)
                    origin.add(i)
                } else {
                    for (j in 0 until width) {
                        out.append(text[i + j])
                        origin.add(i + j)
                    }
                }
            }
            i += width
        }
        origin.add(text.length)
        return Folded(out.toString(), origin.toIntArray())
    }

    fun lowercase(text: String): Folded {
        val out = StringBuilder(text.length)
        val origin = IntArray(text.length + 1) { it }
        text.forEach { out.append(it.lowercaseChar()) }
        return Folded(out.toString(), origin)
    }

    private fun read(
        codePoint: Int,
        width: Int,
    ): Char? {
        val base = Normalizer.normalize(Character.toString(codePoint), Normalizer.Form.NFKD).first()
        val plain =
            when {
                base.isLetterOrDigit() -> base.lowercaseChar()
                width == 1 -> codePoint.toChar().lowercaseChar()
                else -> return null
            }
        val mapped = homoglyphs[plain] ?: plain
        return groupOf[mapped] ?: mapped
    }
}
