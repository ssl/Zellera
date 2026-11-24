package zel.game.moderation

/** Filtered words matched case-insensitively anywhere, longest first; withholding words on normalised text. */
class WordList(
    words: List<FilteredWord>,
    fold: (String) -> String = ChatNormaliser::lowercase,
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

    /** Finds [words] in a text read through [fold], which keeps one character for one so ranges map back. */
    private class Matcher(
        words: List<FilteredWord>,
        private val fold: (String) -> String,
    ) {
        private val keys = words.map { fold(it.key) to it }.sortedByDescending { it.first.length }

        fun matches(text: String): List<Pair<IntRange, FilteredWord>> {
            if (keys.isEmpty()) return emptyList()
            val folded = fold(text)
            val hits = ArrayList<Pair<IntRange, FilteredWord>>()
            var at = 0
            while (at < folded.length) {
                val hit = keys.firstOrNull { (key, _) -> folded.startsWith(key, at) }
                if (hit == null) {
                    at++
                } else {
                    hits += (at until at + hit.first.length) to hit.second
                    at += hit.first.length
                }
            }
            return hits
        }
    }
}
