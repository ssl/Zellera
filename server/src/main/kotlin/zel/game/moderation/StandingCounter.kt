package zel.game.moderation

enum class StandingCounter(
    val column: String,
) {
    WARNINGS_RECEIVED("warnings_received"),
    BANS_RECEIVED("bans_received"),
    REPORTS_ABUSIVE("reports_abusive"),
    TRADE_LOCKS("trade_lock_count"),
}
