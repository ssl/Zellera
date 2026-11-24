package zel.wired.trigger

import zel.room.Room
import zel.room.event.PlayerAction
import zel.room.furni.place.Interactions
import zel.wired.box.BoxSettings
import zel.wired.firing.FurniSource
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest
import kotlin.math.absoluteValue

object TriggerRules {
    const val SAVE_FAILED = "This trigger cannot be saved with these settings."
    const val REQUIRE_CLICK_TILES = "wiredfurni.error.require_click_tiles"
    const val REQUIRE_CONTRACTS = "wiredfurni.error.require_contracts_or_trade_wired"

    private const val CLICK_TILE = "tile.click"
    private val CONTRACTS =
        setOf("wf_contract_payment", "wf_contract_reward", "wf_contract_trade", "wf_xtra_custom_contract")

    private val keepStates = BoxValidation { room, s -> SaveOutcome.Saved(s.copy(furniStates = states(room, s))) }

    private val needsInts =
        BoxValidation.fromRequest { s, request ->
            if (request.ints.isEmpty()) SaveOutcome.Invalid(SAVE_FAILED) else SaveOutcome.Saved(s)
        }

    val validations: Map<String, BoxValidation> =
        mapOf(
            "wf_trg_says_something" to
                BoxValidation.fromRequest { s, request -> SaveOutcome.Saved(says(zeroWithoutInts(s, request))) },
            ClickUser.CODE to
                BoxValidation.fromRequest { s, request -> SaveOutcome.Saved(zeroWithoutInts(s, request)) },
            "wf_trg_user_performs_action" to BoxValidation { _, s -> SaveOutcome.Saved(performsAction(s)) },
            "wf_trg_room_initializes" to BoxValidation { _, _ -> SaveOutcome.Saved(BoxSettings.EMPTY) },
            "wf_trg_click_tile" to picksOnly(REQUIRE_CLICK_TILES, setOf(CLICK_TILE)),
            "wf_trg_transaction_complete" to picksOnly(REQUIRE_CONTRACTS, CONTRACTS),
            "wf_trg_transaction_fail" to picksOnly(REQUIRE_CONTRACTS, CONTRACTS),
            "wf_trg_state_changed" to keepStates,
            "wf_trg_stuff_state" to keepStates,
            "wf_trg_score_achieved" to needsInts,
            "wf_trg_at_given_time" to needsInts,
            "wf_trg_at_time_long" to needsInts,
            "wf_trg_periodically" to needsInts,
            "wf_trg_period_long" to needsInts,
            "wf_trg_period_short" to needsInts,
        )

    private fun zeroWithoutInts(
        s: BoxSettings,
        request: SaveRequest,
    ): BoxSettings = if (request.ints.isEmpty()) s.copy(ints = s.ints.map { 0 }) else s

    private fun says(s: BoxSettings): BoxSettings =
        if (s.ints.getOrNull(SaysSomething.MODE) == SaysSomething.MODE_ANY) s.copy(text = "") else s

    private fun performsAction(s: BoxSettings): BoxSettings {
        val action = s.ints.firstOrNull() ?: 0
        val known = action in PlayerAction.WAVE..PlayerAction.DANCE || action == PlayerAction.SIXTY_SEVEN
        return if (known) s else s.copy(ints = listOf(0))
    }

    private fun picksOnly(
        key: String,
        types: Set<String>,
    ) = BoxValidation { room, s ->
        val wrong =
            s.furniSources.firstOrNull() == FurniSource.PICKED &&
                s.picks.any { pick ->
                    room.furni[pick.absoluteValue]?.let { Interactions.of(it.definition) } !in types
                }
        if (wrong) SaveOutcome.Invalid(key) else SaveOutcome.Saved(s)
    }

    private fun states(
        room: Room,
        settings: BoxSettings,
    ): Map<Int, String> =
        settings.picks
            .mapNotNull { pick -> room.furni[pick.absoluteValue]?.let { pick to it.state } }
            .toMap()
}
