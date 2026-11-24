package zel.wired.score

interface HighscoreStore {
    /** [since] is in unix seconds. */
    fun entries(
        boardId: Int,
        since: Int,
    ): List<ScoreEntry>

    fun dropBefore(
        boardId: Int,
        since: Int,
    )

    fun add(
        boardId: Int,
        entries: List<ScoreEntry>,
    )

    fun clear(boardId: Int)

    fun names(ids: Collection<Int>): Map<Int, String>
}
