package zel.wired.timing

import zel.wired.box.BoxKind
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.form.DialogSpecs

class TimerBoxes(
    timers: WiredTimers,
) {
    val types: List<BoxType<WiredBox>> =
        listOf(
            repeater("wf_trg_periodically", HALF_SECOND, MAX_HALF_SECONDS, short = false),
            repeater("wf_trg_period_long", FIVE_SECONDS, MAX_FIVE_SECONDS, short = false),
            repeater("wf_trg_period_short", SHORT_UNIT, MAX_SHORT_UNITS, short = true),
            DialogSpecs.boxType("wf_trg_at_given_time", BoxKind.Trigger, ::AtGivenTime),
            DialogSpecs.boxType("wf_act_reset_timers", BoxKind.Effect) { id, settings ->
                ResetTimers(id, settings, timers)
            },
        )

    /** The fresh interval of each repeater is its row in the dialog table. */
    private fun repeater(
        code: String,
        unitMillis: Long,
        maxUnits: Int,
        short: Boolean,
    ): BoxType<WiredBox> =
        DialogSpecs.boxType(code, BoxKind.Trigger) { id, settings ->
            Repeater(id, settings, unitMillis, maxUnits, short)
        }

    companion object {
        const val HALF_SECOND = 500L
        private const val FIVE_SECONDS = 5_000L
        private const val SHORT_UNIT = 50L

        /** Timers run at most a day. */
        const val MAX_HALF_SECONDS = 172_800
        private const val MAX_FIVE_SECONDS = 17_280
        private const val MAX_SHORT_UNITS = 10
    }
}
