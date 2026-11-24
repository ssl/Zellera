package zel.wired.effect.furni

import zel.room.unit.UnitDirection
import zel.wired.box.BoxSettings
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome

object FurniEffectRules {
    const val INVALID_START = "Choose a start direction."
    const val INVALID_BLOCKED = "Choose what happens when the way is blocked."

    val validations: Map<String, BoxValidation> =
        mapOf(
            "wf_act_move_to_dir" to
                BoxValidation { _, settings ->
                    val start = settings.ints.getOrNull(0) ?: 0
                    val blocked = settings.ints.getOrNull(1) ?: 0
                    when {
                        start !in 0 until UnitDirection.COUNT -> SaveOutcome.Invalid(INVALID_START)
                        blocked !in 0..MoveToDirection.MAX_RULE -> SaveOutcome.Invalid(INVALID_BLOCKED)
                        else -> SaveOutcome.Saved(settings)
                    }
                },
            "wf_act_move_furni_as_group" to
                BoxValidation.fromRequest { s, request ->
                    SaveOutcome.Saved(if (request.ints.isEmpty()) s.copy(ints = s.ints.map { 0 }) else s)
                },
            "wf_act_set_altitude" to
                BoxValidation { _, settings ->
                    if ((settings.ints.getOrNull(0) ?: 0) < 0) {
                        SaveOutcome.Invalid(SetAltitude.INVALID_ALTITUDE)
                    } else {
                        SaveOutcome.Saved(settings)
                    }
                },
            "wf_act_match_to_sshot" to
                BoxValidation { room, settings ->
                    SaveOutcome.Saved(
                        saved(settings).copy(snapshots = MatchToSnapshot.snapshots(room, settings.picks)),
                    )
                },
            "wf_act_move_rotate" to BoxValidation { _, settings -> SaveOutcome.Saved(saved(settings)) },
            "wf_act_move_furni_to" to
                BoxValidation { room, settings ->
                    val span = MoveFurniTo.span(room.floor.width, room.floor.height)
                    val ints =
                        settings.ints.mapIndexed { i, value ->
                            if (i == MoveFurniTo.SPACING) value.coerceIn(1, span) else value
                        }
                    SaveOutcome.Saved(settings.copy(ints = ints))
                },
        )

    /** The last int of these two dialogs is 1 only on a box never saved. */
    private fun saved(settings: BoxSettings): BoxSettings =
        settings.copy(
            ints =
                settings.ints.mapIndexed { i, value ->
                    if (i == settings.ints.lastIndex && value == 1) 0 else value
                },
        )
}
