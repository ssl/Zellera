package zel.protocol.outgoing.users

import zel.content.ClientStrings
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.protocol.generated.PerkAllowancesOut
import zel.protocol.generated.PerkAllowancesOutCodec
import zel.protocol.outgoing.OutgoingMessage
import zel.protocol.outgoing.users.UserPerks.Verdict.Denied
import zel.protocol.outgoing.users.UserPerks.Verdict.Granted
import zel.protocol.outgoing.users.UserPerks.Verdict.Hidden

class UserPerks(
    val player: Player,
) : OutgoingMessage<PerkAllowancesOut>(PerkAllowancesOutCodec) {
    class PerkContext(
        val tradePass: Boolean,
        val mayTrade: Boolean,
        private val keyHeld: (String) -> Boolean,
    ) {
        fun holds(key: String): Boolean = keyHeld(key)
    }

    sealed interface Verdict {
        data object Granted : Verdict

        data class Denied(
            val requirement: String,
        ) : Verdict

        data object Hidden : Verdict
    }

    class Feature(
        val code: String,
        val judge: (PerkContext) -> Verdict,
    )

    override fun message(): PerkAllowancesOut {
        val permissions = checkNotNull(player.services.game.permissions)
        val trading = player.stats.standing
        val context = PerkContext(trading.tradePass, trading.mayTrade()) { permissions.allows(player, it) }
        return PerkAllowancesOut(
            features().mapNotNull { feature ->
                when (val verdict = feature.judge(context)) {
                    Granted -> PerkAllowancesOut.List1Item(feature.code, "", true)
                    is Denied -> PerkAllowancesOut.List1Item(feature.code, verdict.requirement, false)
                    Hidden -> null
                }
            },
        )
    }

    companion object {
        private const val UNFULFILLED = "requirement.unfulfilled."

        private fun open(
            code: String,
            right: Privilege,
        ) = Feature(code) { if (it.holds(right.key)) Granted else Denied("") }

        private fun earned(
            code: String,
            requirement: String,
            right: Privilege,
        ) = Feature(code) { if (it.holds(right.key)) Granted else Denied(UNFULFILLED + requirement) }

        val FEATURES: List<Feature> =
            listOf(
                Feature("USE_GUIDE_TOOL") { Denied(UNFULFILLED + "helper_level_4") },
                Feature("JUDGE_CHAT_REVIEWS") { Denied(UNFULFILLED + "helper_level_6") },
                earned("VOTE_IN_COMPETITIONS", "helper_level_2", Privilege.PERK_VOTE_IN_COMPETITIONS),
                Feature("CALL_ON_HELPERS") { Denied("") },
                open("CITIZEN", Privilege.PERK_CITIZEN),
                Feature("UNITY_TRADE") {
                    when {
                        !it.tradePass -> Hidden
                        it.mayTrade -> Granted
                        else -> Denied("")
                    }
                },
                Feature("TRADE") { Denied("${UNFULFILLED}citizenship_level_3") },
                earned("BUILDER_AT_WORK", "group_membership", Privilege.PERK_BUILDER_AT_WORK),
                open("CAMERA", Privilege.PERK_CAMERA),
                open("NAVIGATOR_PHASE_TWO_2014", Privilege.PERK_NEW_NAVIGATOR),
                open("MOUSE_ZOOM", Privilege.PERK_MOUSE_ZOOM),
                open("NAVIGATOR_ROOM_THUMBNAIL_CAMERA", Privilege.PERK_ROOM_THUMBNAIL_CAMERA),
            )

        fun features(): List<Feature> {
            val clubOffer = ClientStrings[ClientStrings.CLUB_OFFER_PERK]?.takeIf { it.isNotBlank() } ?: return FEATURES
            return FEATURES + open(clubOffer, Privilege.PERK_CLUB_OFFER)
        }
    }
}
