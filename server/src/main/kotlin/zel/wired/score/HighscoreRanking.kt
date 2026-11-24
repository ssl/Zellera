package zel.wired.score

object HighscoreRanking {
    private const val MAX_ROWS = 50

    fun rows(
        type: ScoreType,
        entries: List<ScoreEntry>,
    ): List<ScoreRow> {
        val teams = entries.groupBy { it.userIds.toSet() }.values
        val timed = entries.filter { it.seconds > 0 }.groupBy { it.userIds.toSet() }.values
        val rows =
            when (type) {
                ScoreType.CLASSIC -> {
                    entries.map { ScoreRow(it.score, it.userIds) }.sortedByDescending { it.score }
                }

                ScoreType.PER_TEAM -> {
                    best(teams) { e -> e.maxOf { it.score } }.sortedByDescending { it.score }
                }

                ScoreType.MOST_WINS -> {
                    best(teams) { e -> e.count { it.win } }.filter { it.score > 0 }.sortedByDescending { it.score }
                }

                ScoreType.FASTEST_TIME -> {
                    best(timed) { e -> e.minOf { it.seconds } }.sortedBy { it.score }
                }

                ScoreType.LONGEST_TIME -> {
                    best(timed) { e -> e.maxOf { it.seconds } }.sortedByDescending { it.score }
                }
            }
        return rows.take(MAX_ROWS)
    }

    private fun best(
        teams: Collection<List<ScoreEntry>>,
        value: (List<ScoreEntry>) -> Int,
    ): List<ScoreRow> = teams.map { entries -> ScoreRow(value(entries), entries.first().userIds) }
}
