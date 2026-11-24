package zel.wired.score

data class BoardKind(
    val type: ScoreType,
    val clear: ClearType,
) {
    companion object {
        private const val PREFIX = "highscore_"

        fun of(name: String): BoardKind {
            val type = name.removePrefix(PREFIX).substringBefore('*')
            val period = name.substringAfter('*', "").toIntOrNull() ?: 1
            return BoardKind(
                ScoreType.entries.firstOrNull { type.startsWith(it.nameStart) } ?: ScoreType.CLASSIC,
                ClearType.entries.getOrElse(period - 1) { ClearType.ALL_TIME },
            )
        }
    }
}
