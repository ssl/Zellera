package zel.game.players

class CustomFilter {
    private val words = LinkedHashSet<String>()
    private var mask: Regex? = null

    @Synchronized
    fun add(word: String?): Boolean = normalize(word)?.let(words::add)?.also { if (it) mask = null } ?: false

    @Synchronized
    fun remove(word: String?): Boolean = normalize(word)?.let(words::remove)?.also { if (it) mask = null } ?: false

    @Synchronized
    fun contains(word: String?): Boolean = normalize(word) in words

    @Synchronized
    fun isEmpty(): Boolean = words.isEmpty()

    @Synchronized
    fun size(): Int = words.size

    @Synchronized
    fun words(): List<String> = words.toList()

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
            ?: words
                .joinToString("|", transform = Regex::escape)
                .toRegex(RegexOption.IGNORE_CASE)
                .also { mask = it }
    }

    companion object {
        const val LONGEST_WORD = 25

        /** Null when nothing is left of the word. */
        fun normalize(word: String?): String? =
            word
                ?.trim()
                ?.lowercase()
                ?.take(LONGEST_WORD)
                ?.takeIf { it.isNotEmpty() }
    }
}
