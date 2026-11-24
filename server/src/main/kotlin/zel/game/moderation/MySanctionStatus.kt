package zel.game.moderation

import zel.util.HOURS_PER_DAY
import zel.util.SECONDS_PER_HOUR
import kotlin.math.ceil

object MySanctionStatus {
    const val PERMANENT_BAN_HOURS = 100 * 365 * 24

    fun interface Texts {
        fun get(
            key: String,
            fallback: String,
        ): String
    }

    data class Type(
        val name: String,
        val hours: Int,
        val probationDays: Int,
    ) {
        companion object {
            val NONE = Type("", 0, 0)
        }
    }

    data class Entry(
        val current: Type,
        val text: String,
        val gradual: Boolean,
        val probationHoursLeft: Int,
        val next: Type,
    )

    fun entries(
        last: SanctionRecord?,
        steps: (Int) -> LadderStep?,
        now: Int,
        muteSecondsLeft: Int,
        texts: Texts,
    ): List<Entry> {
        val entries = ArrayList<Entry>(2)
        val step = last?.let { steps(it.step) }
        var ladderMuteRunning = false

        if (last != null && step != null && last.onProbation(now)) {
            val current = type(step)
            val reason = last.reason.trim().takeIf { it.isNotEmpty() }
            val text = currentText(current, texts) + reason?.let { "\n$it" }.orEmpty()
            val hoursLeft = ceil((last.probationEndsAt - now) / SECONDS_PER_HOUR.toDouble()).toInt()
            entries.add(Entry(current, text, true, hoursLeft, type(steps(last.step + 1))))
            ladderMuteRunning = last.kind == SanctionKind.MUTE && last.endsAt > now
        }

        if (muteSecondsLeft > 0 && !ladderMuteRunning) {
            val mute = Type("MUTE", ceil(muteSecondsLeft / SECONDS_PER_HOUR.toDouble()).toInt(), 0)
            val text = texts.get("moderation.status.muted_now", "You are muted for a while.")
            entries.add(Entry(mute, text, false, 0, Type.NONE))
        }

        return entries
    }

    internal fun type(step: LadderStep?): Type {
        if (step == null) return Type.NONE
        val permanent = step.kind == SanctionKind.BAN && step.hours >= PERMANENT_BAN_HOURS
        return Type(if (permanent) "BAN_PERMANENT" else step.kind.name, step.hours, step.probationDays)
    }

    internal fun currentText(
        type: Type,
        texts: Texts,
    ): String =
        when (type.name) {
            "ALERT" -> {
                texts.get("moderation.status.alert", "A moderator gave you a warning.")
            }

            "MUTE" -> {
                texts
                    .get("moderation.status.mute", "You were muted. Duration in hours: %hours%.")
                    .replace("%hours%", type.hours.toString())
            }

            "BAN", "BAN_PERMANENT" -> {
                if (type.hours > HOURS_PER_DAY) {
                    texts
                        .get("moderation.status.ban_days", "You were banned. Duration in days: %days%.")
                        .replace("%days%", (type.hours / HOURS_PER_DAY).toString())
                } else {
                    texts
                        .get("moderation.status.ban", "You were banned. Duration in hours: %hours%.")
                        .replace("%hours%", type.hours.toString())
                }
            }

            else -> {
                ""
            }
        }
}
