package zel.game.moderation

/**
 * A word the filter finds. [replacement] stands in its place; without one the whole line is withheld where a line
 * can be, and the word is cut out elsewhere. [muteSeconds] above 0 also mutes whoever wrote it.
 */
data class FilteredWord(
    val key: String,
    val replacement: String?,
    val muteSeconds: Int = 0,
) {
    val withholdsLine: Boolean get() = replacement == null
}
