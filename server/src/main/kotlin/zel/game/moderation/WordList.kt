package zel.game.moderation

/** Filtered words matched case-insensitively anywhere, longest first; withholding words on normalised text. */
class WordList(
    words: List<FilteredWord>,
    fold: (String) -> ChatNormaliser.Folded = ChatNormaliser::lowercase,
) {
    data class Censored(
        val text: String,
        val found: List<FilteredWord>,
    )

    private val matched = words.filter { it.key.isNotEmpty() }
    private val censoring = Matcher(matched, fold)
    private val hiding = Matcher(matched.filter { it.withholdsLine }, ChatNormaliser::normalise)

    fun hides(text: String): Boolean = hiding.matches(text).isNotEmpty()

    fun censor(text: String): Censored {
        val hits = censoring.matches(text)
        if (hits.isEmpty()) return Censored(text, emptyList())
        val out = StringBuilder(text.length)
        var at = 0
        for ((range, word) in hits) {
            out.append(text, at, range.first).append(word.replacement.orEmpty())
            at = range.last + 1
        }
        out.append(text, at, text.length)
        return Censored(out.toString(), hits.map { it.second }.distinct())
    }

    /** Finds [words] in a text read through [fold]. */
    private class Matcher(
        words: List<FilteredWord>,
        private val fold: (String) -> ChatNormaliser.Folded,
    ) {
        private val keys = words.map { fold(it.key).text to it }.sortedByDescending { it.first.length }

        fun matches(text: String): List<Pair<IntRange, FilteredWord>> {
            if (keys.isEmpty()) return emptyList()
            val folded = fold(text)
            val chars = folded.text
            val hits = ArrayList<Pair<IntRange, FilteredWord>>()
            var at = 0
            while (at < chars.length) {
                val hit = keys.firstOrNull { (key, _) -> chars.startsWith(key, at) }
                if (hit == null) {
                    at++
                } else {
                    val end = at + hit.first.length
                    hits += (folded.origin[at] until folded.origin[end]) to hit.second
                    at = end
                }
            }
            return hits
        }
    }
}
