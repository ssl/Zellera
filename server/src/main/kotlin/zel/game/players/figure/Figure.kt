package zel.game.players.figure

@JvmInline
value class Figure(
    val text: String,
) {
    /** In hash order, the order a combined figure lists them in. */
    val sets: Map<String, String>
        get() =
            text.split(".").associateTo(HashMap()) { set ->
                val type = set.substringBefore('-')
                type to set.substringAfter('-', "")
            }

    fun dressedIn(
        outfit: Figure,
        ownTypes: Set<String>? = null,
        outfitTypes: Set<String>? = null,
    ): Figure = Figure((setsOf(ownTypes) + outfit.setsOf(outfitTypes)).joinToString("."))

    private fun setsOf(types: Set<String>?): List<String> =
        sets.filterKeys { types == null || it in types }.map { (type, value) -> "$type-$value" }

    companion object {
        /** The width of the stored figure columns. */
        const val MAX_LENGTH = 256
        private const val HEAD = "hd-"

        private val SHAPE = Regex("""[a-z]{2}-\d{1,9}(-\d{0,9})*(\.[a-z]{2}-\d{1,9}(-\d{0,9})*)*""")

        fun isWellFormed(text: String): Boolean = text.length <= MAX_LENGTH && SHAPE.matches(text)

        fun clampToMaxLength(text: String): String {
            if (text.length <= MAX_LENGTH) return text
            var kept = ""
            val (head, rest) = text.split(".").partition { it.startsWith(HEAD) }
            for (set in head + rest) {
                val next = if (kept.isEmpty()) set else "$kept.$set"
                if (next.length > MAX_LENGTH) break
                kept = next
            }
            return kept
        }
    }
}
