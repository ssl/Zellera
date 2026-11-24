package zel.protocol.outgoing.users

import zel.content.ClientStrings
import zel.game.players.CLUB_MONTH_DAYS
import zel.game.players.Player
import zel.game.players.subscriptions.ClubSettings
import zel.game.players.subscriptions.Subscription
import zel.protocol.generated.ScrSendPlayerInfoOut
import zel.protocol.generated.ScrSendPlayerInfoOutCodec
import zel.protocol.outgoing.OutgoingMessage
import zel.util.SECONDS_PER_DAY
import zel.util.SECONDS_PER_MINUTE
import kotlin.math.ceil

class UserClub(
    val player: Player,
    val responseType: Int,
) : OutgoingMessage<ScrSendPlayerInfoOut>(ScrSendPlayerInfoOutCodec) {
    override fun message(): ScrSendPlayerInfoOut {
        val stats = player.stats
        val secondsLeft = maxOf(stats.club.get(Subscription.CLUB)?.secondsLeft ?: 0, 0)
        val time = ClubTime.of(secondsLeft)
        val everMember = stats.club.totalClubDays > 0
        val discount = secondsLeft > 0 && ClubSettings.discount.covers(time.totalDays)
        return ScrSendPlayerInfoOut(
            ClientStrings[ClientStrings.CLUB_PRODUCT_NAME].orEmpty(),
            time.daysToPeriodEnd,
            0,
            time.periodsAhead,
            if (discount) RESPONSE_DISCOUNT_AVAILABLE else responseType,
            everMember,
            true,
            0,
            stats.club.pastClubDays,
            time.minutes,
            if (everMember) (player.services.unixTimestamp() - stats.clubLastChangedAt) / SECONDS_PER_MINUTE else -1,
        )
    }

    data class ClubTime(
        val totalDays: Int,
        val daysToPeriodEnd: Int,
        val periodsAhead: Int,
        val minutes: Int,
    ) {
        companion object {
            fun of(secondsLeft: Int): ClubTime {
                val days = ceil(secondsLeft / SECONDS_PER_DAY.toDouble()).toInt()
                return ClubTime(
                    days,
                    if (days > 0) (days - 1) % CLUB_MONTH_DAYS + 1 else 0,
                    if (days > 0) (days - 1) / CLUB_MONTH_DAYS else 0,
                    ceil(secondsLeft / SECONDS_PER_MINUTE.toDouble()).toInt(),
                )
            }
        }
    }

    companion object {
        const val RESPONSE_REQUESTED = 1

        const val RESPONSE_PUSHED = 2
        private const val RESPONSE_DISCOUNT_AVAILABLE = 3
    }
}
