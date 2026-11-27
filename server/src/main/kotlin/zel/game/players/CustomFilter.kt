package zel.game.players

import java.text.Normalizer

class CustomFilter {
    private val words = LinkedHashMap<String, String>()
    private var mask: Regex? = null

    @Synchronized
    fun add(word: String?): Boolean {
        val normalized = normalize(word) ?: return false
        if (words.putIfAbsent(collationKey(normalized), normalized) != null) return false
        mask = null
        return true
    }

    @Synchronized
    fun remove(word: String?): Boolean {
        val normalized = normalize(word) ?: return false
        if (words.remove(collationKey(normalized)) == null) return false
        mask = null
        return true
    }

    @Synchronized
    fun contains(word: String?): Boolean = normalize(word)?.let { collationKey(it) in words } ?: false

    @Synchronized
    fun isEmpty(): Boolean = words.isEmpty()

    @Synchronized
    fun size(): Int = words.size

    @Synchronized
    fun words(): List<String> = words.values.toList()

    /** [replacement] is taken literally. */
    fun apply(
        message: String?,
        replacement: String,
    ): String? {
        if (message.isNullOrEmpty()) return message
        val mask = mask() ?: return message
        return mask.replace(message) { replacement }
    }

    @Synchronized
    private fun mask(): Regex? {
        if (words.isEmpty()) return null
        return mask
            ?: words.values
                .joinToString("|", transform = Regex::escape)
                .toRegex(RegexOption.IGNORE_CASE)
                .also { mask = it }
    }

    companion object {
        const val LONGEST_WORD = 25

        private val COMBINING = Regex("\\p{Mn}+")

        /** Null when nothing is left of the word. */
        fun normalize(word: String?): String? =
            word
                ?.trim()
                ?.lowercase()
                ?.take(LONGEST_WORD)
                ?.takeIf { it.isNotEmpty() }

        fun collationKey(word: String): String =
            COMBINING.replace(Normalizer.normalize(word.lowercase(), Normalizer.Form.NFD), "").replace("ß", "ss")
    }
}
