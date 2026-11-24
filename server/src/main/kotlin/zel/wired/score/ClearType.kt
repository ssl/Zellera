package zel.wired.score

enum class ClearType {
    ALL_TIME,
    DAILY,
    WEEKLY,
    MONTHLY,
    ;

    val wire: Int get() = ordinal
}
