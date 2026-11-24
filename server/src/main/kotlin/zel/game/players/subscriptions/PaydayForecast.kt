package zel.game.players.subscriptions

import zel.protocol.generated.ScrSendKickbackInfoOut

data class PaydayForecast(
    val streakDays: Int,
    val firstSubscribed: String,
    val kickbackShare: Double,
    val spentSincePayday: Int,
    val streakReward: Int,
    val spendingReward: Int,
    val minutesUntilPayday: Int,
) {
    /** What the round pays, capped rather than wrapped. */
    val total: Int get() = (streakReward.toLong() + spendingReward).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

    fun message() =
        ScrSendKickbackInfoOut(
            streakDays,
            firstSubscribed,
            kickbackShare,
            0,
            0,
            spentSincePayday,
            streakReward,
            spendingReward,
            minutesUntilPayday,
        )
}
