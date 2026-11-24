package zel.protocol.landing

import zel.core.Configuration
import zel.game.landing.Landing
import zel.protocol.generated.CommunityGoalHallOfFameOut
import zel.protocol.generated.CommunityGoalHallOfFameOutCodec
import zel.protocol.generated.CurrentTimingCodeOut
import zel.protocol.generated.CurrentTimingCodeOutCodec
import zel.protocol.generated.GetCommunityGoalHallOfFameInCodec
import zel.protocol.generated.GetCurrentTimingCodeInCodec
import zel.protocol.generated.GetFurnitureAliasesInCodec
import zel.protocol.generated.GetIsBadgeRequestFulfilledInCodec
import zel.protocol.generated.GetLimitedOfferAppearingNextInCodec
import zel.protocol.generated.GetPromoArticlesInCodec
import zel.protocol.generated.GetSecondsUntilInCodec
import zel.protocol.generated.IsBadgeRequestFulfilledOut
import zel.protocol.generated.IsBadgeRequestFulfilledOutCodec
import zel.protocol.generated.LimitedOfferAppearingNextOut
import zel.protocol.generated.LimitedOfferAppearingNextOutCodec
import zel.protocol.generated.PromoArticlesOut
import zel.protocol.generated.PromoArticlesOutCodec
import zel.protocol.generated.RequestABadgeInCodec
import zel.protocol.generated.SecondsUntilOut
import zel.protocol.generated.SecondsUntilOutCodec
import zel.protocol.outgoing.rooms.FurnitureAliases
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.util.GameZone
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class LandingPackets(
    private val manager: Landing,
    private val config: Configuration,
    private val now: () -> Int,
) : PacketPackage {
    override val name = "landing"

    private val hallOfFameCooldown = ClientCooldown("landing.halloffame", HALL_OF_FAME_GAP_MS)
    private val badgeQueryCooldown = ClientCooldown("landing.badgequery", BADGE_QUERY_GAP_MS)
    private val badgeClaimCooldown = ClientCooldown("landing.badgeclaim", BADGE_CLAIM_GAP_MS)

    override val routes =
        listOf(
            PacketRoute(GetFurnitureAliasesInCodec) { session, _ ->
                session.client.send(FurnitureAliases())
            },
            PacketRoute(GetPromoArticlesInCodec) { session, _ -> sendNews(session) },
            PacketRoute(GetCommunityGoalHallOfFameInCodec) { session, m ->
                if (hallOfFameCooldown.acquire(session.client)) {
                    session.send(CommunityGoalHallOfFameOutCodec, hallOfFame(m.code))
                }
            },
            PacketRoute(GetCurrentTimingCodeInCodec) { session, m ->
                session.send(
                    CurrentTimingCodeOutCodec,
                    CurrentTimingCodeOut(m.schedulingStr, TimingSchedule.dueCode(m.schedulingStr)),
                )
            },
            PacketRoute(GetIsBadgeRequestFulfilledInCodec) { session, m ->
                if (badgeQueryCooldown.acquire(session.client)) isBadgeFulfilled(session, m.requestCode)
            },
            PacketRoute(RequestABadgeInCodec) { session, m ->
                if (badgeClaimCooldown.acquire(session.client)) claimBadge(session, m.requestCode)
            },
            PacketRoute(GetLimitedOfferAppearingNextInCodec) { session, _ -> sendLimitedOffer(session) },
            PacketRoute(GetSecondsUntilInCodec) { session, m -> sendSecondsUntil(session, m.date) },
        )

    private fun sendNews(session: PacketSession) {
        session.send(
            PromoArticlesOutCodec,
            PromoArticlesOut(
                manager.news.all.map {
                    PromoArticlesOut.ArticlesItem(
                        it.id,
                        it.title,
                        it.body,
                        it.buttonText,
                        it.link.type.code,
                        it.link.target,
                        it.image,
                    )
                },
            ),
        )
    }

    private fun hallOfFame(code: String) =
        CommunityGoalHallOfFameOut(
            code,
            manager.hallOfFame.standings(code).map {
                CommunityGoalHallOfFameOut.WinnersItem(it.playerId, it.name, it.figure, it.rank, it.points)
            },
        )

    private fun isBadgeFulfilled(
        session: PacketSession,
        requestCode: String,
    ) {
        val player = session.client.player ?: return
        val key = badgeRewardKey(requestCode) ?: return
        val badge = config.getValue("$key.badge_code", "")
        val claimed = badge.isNotEmpty() && player.inventory.badges.owns(badge)
        session.send(IsBadgeRequestFulfilledOutCodec, IsBadgeRequestFulfilledOut(requestCode, claimed))
    }

    private fun claimBadge(
        session: PacketSession,
        requestCode: String,
    ) {
        val player = session.client.player ?: return
        val key = badgeRewardKey(requestCode) ?: return
        val badgeCode = config.getValue("$key.badge_code", "")
        if (badgeCode.isEmpty() || !config.getBoolean("$key.is_claimable")) {
            session.send(IsBadgeRequestFulfilledOutCodec, IsBadgeRequestFulfilledOut(requestCode, false))
            return
        }
        player.inventory.badges.award(badgeCode, asUnseen = false)
        session.send(IsBadgeRequestFulfilledOutCodec, IsBadgeRequestFulfilledOut(requestCode, true))
    }

    private fun sendLimitedOffer(session: PacketSession) {
        if (!config.getBoolean(LIMITED_OFFER_ENABLED)) return
        val remaining = maxOf(config.getInt(LIMITED_OFFER_TIMESTAMP) - now(), -1)
        val pending = remaining > 0
        session.send(
            LimitedOfferAppearingNextOutCodec,
            LimitedOfferAppearingNextOut(
                remaining,
                if (pending) -1 else config.getInt(LIMITED_OFFER_ITEM_ID),
                if (pending) -1 else config.getInt(LIMITED_OFFER_PAGE_ID),
                if (pending) "" else config.getValue(LIMITED_OFFER_ITEM_NAME, ""),
            ),
        )
    }

    private fun sendSecondsUntil(
        session: PacketSession,
        date: String,
    ) {
        val moment =
            try {
                LocalDateTime.parse(date, TimingSchedule.STAMP)
            } catch (_: DateTimeParseException) {
                return
            }
        val seconds = maxOf(0, moment.atZone(GameZone.of(config)).toEpochSecond().toInt() - now())
        session.send(SecondsUntilOutCodec, SecondsUntilOut(date, seconds))
    }

    internal companion object {
        private const val MAX_BADGE_REQUEST_CODE = 32

        fun badgeRewardKey(code: String): String? =
            code
                .takeIf { it.isNotEmpty() && it.length <= MAX_BADGE_REQUEST_CODE }
                ?.takeIf { it.all { c -> c.isLetterOrDigit() || c == '_' || c == '-' } }
                ?.let { "landing.badge_reward.$it" }

        private const val HALL_OF_FAME_GAP_MS = 500L
        private const val BADGE_QUERY_GAP_MS = 500L
        private const val BADGE_CLAIM_GAP_MS = 1000L
        private const val LIMITED_OFFER_ENABLED = "landing.limited_countdown.is_shown"
        private const val LIMITED_OFFER_TIMESTAMP = "landing.limited_countdown.ends_at"
        private const val LIMITED_OFFER_ITEM_ID = "landing.limited_countdown.furni_id"
        private const val LIMITED_OFFER_PAGE_ID = "landing.limited_countdown.page_id"
        private const val LIMITED_OFFER_ITEM_NAME = "landing.limited_countdown.furni_name"
    }
}
