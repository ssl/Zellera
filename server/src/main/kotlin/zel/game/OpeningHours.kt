package zel.game

import zel.core.rules.rules
import zel.util.GameZone
import zel.util.MINUTES_PER_DAY
import zel.util.MINUTES_PER_HOUR
import java.time.LocalTime

/** Opening hours from `opening_hours.*` in minutes of the day in the game's zone; equal times mean all day. */
class OpeningHours(
    private val clock: () -> LocalTime = { LocalTime.now(GameZone.clock) },
) {
    val enabled: Boolean get() = rules.openingHours

    val openHour: Int get() = rules.opensAtMinute / MINUTES_PER_HOUR

    val openMinute: Int get() = rules.opensAtMinute % MINUTES_PER_HOUR

    val isOpen: Boolean get() = isOpenAt(clock())

    fun isOpenAt(time: LocalTime): Boolean {
        if (!enabled) return true
        val opens = rules.opensAtMinute
        val closes = rules.closesAtMinute
        val at = minuteOf(time)
        return when {
            opens == closes -> true
            opens < closes -> at in opens until closes
            else -> at >= opens || at < closes
        }
    }

    /** Whole minutes until the game closes; null while it is closed or has no hours. */
    fun minutesUntilClose(time: LocalTime): Int? {
        if (!enabled || rules.opensAtMinute == rules.closesAtMinute || !isOpenAt(time)) return null
        return Math.floorMod(rules.closesAtMinute - minuteOf(time), MINUTES_PER_DAY)
    }

    private fun minuteOf(time: LocalTime): Int = time.hour * MINUTES_PER_HOUR + time.minute
}
