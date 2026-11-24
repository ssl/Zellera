package zel.room.furni.transit

/** Who a member gate lets through, and when it closes behind them. */
enum class GateRule(
    /** Sent as a plural update after the tick's unit update. */
    val plural: Boolean = false,
    /** Closes this long after a step-off; null: as the last unit steps off, [OPEN_MILLIS] after opening at least. */
    val closeAfterMillis: Long? = null,
) {
    Club(plural = true),
    Group(closeAfterMillis = 500L),
    Effect(closeAfterMillis = 1_000L),
    ;

    companion object {
        const val OPEN_MILLIS = 1_000L
    }
}
