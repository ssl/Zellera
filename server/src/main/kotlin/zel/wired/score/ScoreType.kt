package zel.wired.score

enum class ScoreType(
    val nameStart: String,
) {
    PER_TEAM("perteam"),
    MOST_WINS("mostwin"),
    CLASSIC("classic"),
    FASTEST_TIME("fastesttime"),
    LONGEST_TIME("longesttime"),
    ;

    val wire: Int get() = ordinal
}
