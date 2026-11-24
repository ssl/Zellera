package zel.wired.firing

/** [created] and [updated] are epoch milliseconds. */
data class HeldValue(
    val value: Long,
    val created: Long,
    val updated: Long,
)
