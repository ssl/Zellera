package zel.wired.condition

import zel.room.Room
import zel.room.RoomScope
import zel.room.games.GamesPart
import zel.wired.box.BoxForm
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.effect.furni.MatchToSnapshot
import zel.wired.form.BoxValidation
import zel.wired.form.DialogSpecs
import zel.wired.form.SaveOutcome
import kotlin.math.abs

@Suppress("LongParameterList") // one collaborator per fact source a condition reads
class ConditionBoxes(
    private val games: GamesPart,
    private val elapsed: (RoomScope) -> Long,
    private val clock: WiredClock,
    private val units: UnitFacts,
    private val recent: RecentActions,
    private val chests: ChestStock = ChestStock.NONE,
    private val moves: MoveProbe = MoveProbe.NONE,
    private val amounts: ChestAmounts = ChestAmounts.TYPED,
) {
    val types: List<BoxType<WiredBox>> =
        furni() + users() + time() + teams() +
            BoxType("wf_xtra_or_eval", BoxKind.Addon, tableForm("wf_xtra_or_eval"), ::OrEval)

    val validations: Map<String, BoxValidation> =
        mapOf(
            "wf_cnd_match_snapshot" to BoxValidation(::snapshot),
            "wf_cnd_not_match_snap" to BoxValidation(::snapshot),
            "wf_cnd_counter_time_matches" to BoxValidation(::counters),
            "wf_cnd_chest_has_items" to BoxValidation { _, s -> SaveOutcome.Saved(s) },
            "wf_cnd_user_count_in" to BoxValidation { _, s -> userCount(s) },
            "wf_cnd_not_user_count" to BoxValidation { _, s -> userCount(s) },
            "wf_cnd_chest_has_item_type" to BoxValidation { _, s -> chestAmount(s) },
            "wf_cnd_user_performs_action" to BoxValidation { _, s -> action(s) },
            "wf_cnd_not_user_performs_action" to BoxValidation { _, s -> action(s) },
            "wf_cnd_time_more_than" to needsInts(::timeLimit),
            "wf_cnd_time_less_than" to needsInts(::timeLimit),
            "wf_cnd_date_rng_active" to needsInts { SaveOutcome.Saved(it) },
            "wf_xtra_or_eval" to BoxValidation { _, s -> orEval(s) },
            "wf_cnd_slc_quantity" to
                BoxValidation.fromRequest { s, request ->
                    SaveOutcome.Saved(if (request.ints.isEmpty()) s.copy(ints = s.ints.map { 0 }) else amountHeld(s))
                },
        )

    private fun furni(): List<BoxType<WiredBox>> =
        listOf(
            type("wf_cnd_has_furni_on") { id, s -> FurniHasFurniOn(id, s, false) },
            type("wf_cnd_not_furni_on") { id, s -> FurniHasFurniOn(id, s, true) },
            type("wf_cnd_furnis_hv_avtrs") { id, s -> FurniHasAvatar(id, s, false, units) },
            type("wf_cnd_not_hv_avtrs") { id, s -> FurniHasAvatar(id, s, true, units) },
            type("wf_cnd_stuff_is") { id, s -> StuffIs(id, s, false) },
            type("wf_cnd_not_stuff_is") { id, s -> StuffIs(id, s, true) },
            type("wf_cnd_has_altitude", ::HasAltitude),
            type("wf_cnd_match_snapshot") { id, s -> MatchSnapshot(id, s, false) },
            type("wf_cnd_not_match_snap") { id, s -> MatchSnapshot(id, s, true) },
            type("wf_cnd_counter_time_matches") { id, s -> CounterTimeMatches(id, s, games) },
            type("wf_cnd_valid_moves") { id, s -> ValidMoves(id, s, moves) },
            type("wf_cnd_slc_quantity", ::SelectionQuantity),
            type("wf_cnd_chest_has_items") { id, s -> ChestHasItems(id, s, chests, amounts) },
            type("wf_cnd_chest_has_item_type") { id, s -> ChestHasItemType(id, s, chests, amounts) },
        )

    private fun users(): List<BoxType<WiredBox>> =
        listOf(
            type("wf_cnd_trggrer_on_frn") { id, s -> TriggererOnFurni(id, s, false) },
            type("wf_cnd_not_trggrer_on") { id, s -> TriggererOnFurni(id, s, true) },
            type("wf_cnd_not_user_count", ::NotUserCount),
            type("wf_cnd_actor_in_group") { id, s -> ActorInGroup(id, s, units, false) },
            type("wf_cnd_not_in_group") { id, s -> ActorInGroup(id, s, units, true) },
            type("wf_cnd_has_handitem") { id, s -> HasHandItem(id, s, units, false) },
            type("wf_cnd_not_has_handitem") { id, s -> HasHandItem(id, s, units, true) },
            type("wf_cnd_actor_dir", ::ActorDirection),
            type("wf_cnd_triggerer_match") { id, s -> TriggererMatch(id, s, units, false) },
            type("wf_cnd_not_triggerer_match") { id, s -> TriggererMatch(id, s, units, true) },
            type("wf_cnd_user_performs_action") { id, s -> UserPerformsAction(id, s, units, recent, false) },
            type("wf_cnd_not_user_performs_action") { id, s -> UserPerformsAction(id, s, units, recent, true) },
            type("wf_cnd_wearing_effect") { id, s -> WearingEffect(id, s, units, false) },
            type("wf_cnd_not_wearing_fx") { id, s -> WearingEffect(id, s, units, true) },
            type("wf_cnd_wearing_badge") { id, s -> WearingBadge(id, s, units, false) },
            type("wf_cnd_not_wearing_b") { id, s -> WearingBadge(id, s, units, true) },
        )

    private fun time(): List<BoxType<WiredBox>> =
        listOf(
            type("wf_cnd_time_more_than") { id, s -> TimeCompare(id, s, elapsed, Comparison.MORE) },
            type("wf_cnd_time_less_than") { id, s -> TimeCompare(id, s, elapsed, Comparison.LESS) },
            type("wf_cnd_match_time") { id, s -> MatchTime(id, s, clock) },
            type("wf_cnd_match_date") { id, s -> MatchDate(id, s, clock) },
            type("wf_cnd_date_rng_active") { id, s -> DateRangeActive(id, s, clock) },
        )

    private fun teams(): List<BoxType<WiredBox>> =
        listOf(
            type("wf_cnd_actor_in_team") { id, s -> ActorInTeam(id, s, games, false) },
            type("wf_cnd_not_in_team") { id, s -> ActorInTeam(id, s, games, true) },
            type("wf_cnd_team_has_score") { id, s -> TeamHasScore(id, s, games) },
            type("wf_cnd_team_has_rank") { id, s -> TeamHasRank(id, s, games) },
        )

    private fun type(
        code: String,
        create: (Int, BoxSettings) -> WiredBox,
    ): BoxType<WiredBox> = BoxType(code, BoxKind.Condition, tableForm(code), create)

    private fun snapshot(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome = SaveOutcome.Saved(settings.copy(snapshots = MatchToSnapshot.snapshots(room, settings.picks)))

    private fun counters(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome {
        val present = settings.picks.mapNotNull { room.furni[abs(it)] }
        if (present.any { !CounterTimeMatches.isCounter(games, it) }) return SaveOutcome.Invalid(REQUIRE_COUNTER)
        return SaveOutcome.Saved(settings.copy(picks = present.map { it.id }))
    }

    private fun chestAmount(settings: BoxSettings): SaveOutcome {
        val named = settings.variableIds.any { it.isNotBlank() && it != NO_VARIABLE }
        return if (settings.intAt(FROM_VARIABLE) != 0 &&
            !named
        ) {
            SaveOutcome.Invalid(SAVE_REFUSED)
        } else {
            SaveOutcome.Saved(settings)
        }
    }

    private fun action(settings: BoxSettings): SaveOutcome {
        val saved = settings.intAt(0).takeIf { it in UserPerformsAction.ACTIONS } ?: 0
        return SaveOutcome.Saved(settings.copy(ints = listOf(saved) + settings.ints.drop(1)))
    }

    private fun timeLimit(settings: BoxSettings): SaveOutcome =
        SaveOutcome.Saved(
            settings.copy(
                ints =
                    listOf(settings.intAt(0).coerceIn(0, TimeCompare.MAX_HALF_SECONDS)) + settings.ints.drop(1),
            ),
        )

    /** A save without any number is refused; the fresh ints would hide that from [rule]. */
    private fun needsInts(rule: (BoxSettings) -> SaveOutcome): BoxValidation =
        BoxValidation.fromRequest { settings, request ->
            if (request.ints.isEmpty()) SaveOutcome.Invalid(SAVE_REFUSED) else rule(settings)
        }

    private fun orEval(settings: BoxSettings): SaveOutcome {
        val mode = settings.intAt(0).coerceIn(OrEval.COUNT, OrEval.NONE)
        return SaveOutcome.Saved(settings.copy(ints = listOf(mode, settings.intAt(1), settings.intAt(2))))
    }

    /** The highest count is held to 125, and never below the lowest. */
    private fun userCount(settings: BoxSettings): SaveOutcome {
        val low = settings.intAt(0).coerceIn(0, MAX_PEOPLE)
        val high = settings.intAt(1).coerceIn(low, MAX_PEOPLE)
        return SaveOutcome.Saved(settings.copy(ints = listOf(low, high)))
    }

    companion object {
        private const val SAVE_REFUSED = "This condition cannot be saved with these settings."
        private const val MAX_PEOPLE = 125
        private const val REQUIRE_COUNTER = "wiredfurni.error.require_counter_furni"
        private const val NO_VARIABLE = "0"
        private const val FROM_VARIABLE = 1

        private fun tableForm(code: String): BoxForm =
            requireNotNull(DialogSpecs.TABLE[code]) { "no dialog row for $code" }.form
    }

    /** An amount above 100 is saved as 100. */
    private fun amountHeld(s: BoxSettings): BoxSettings =
        s.copy(
            ints =
                s.ints.mapIndexed { i, v ->
                    if (i == SelectionQuantity.AMOUNT) v.coerceIn(0, SelectionQuantity.MAX_AMOUNT) else v
                },
        )
}
