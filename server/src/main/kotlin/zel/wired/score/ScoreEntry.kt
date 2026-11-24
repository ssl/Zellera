package zel.wired.score

data class ScoreEntry(
    val userIds: List<Int>,
    val score: Int,
    val win: Boolean,
    val timestamp: Int,
    val seconds: Int = 0,
)
