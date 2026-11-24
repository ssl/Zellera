package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.util.MILLIS_PER_MINUTE
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing

class ClockCounter(
    override val furniId: Int,
    override val settings: BoxSettings,
) : FurniTrigger {
    private val targetMillis: Long =
        (settings.ints.getOrNull(MINUTES) ?: 0) * MILLIS_PER_MINUTE +
            (settings.ints.getOrNull(SECONDS) ?: 0) * SECOND_MILLIS +
            (settings.ints.getOrNull(HALF) ?: 0) * HALF_MILLIS

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = false

    fun reaches(
        clockId: Int,
        millis: Int,
    ): Boolean = millis.toLong() == targetMillis && listens(clockId)

    private companion object {
        const val SECONDS = 0
        const val MINUTES = 1
        const val HALF = 2
        const val SECOND_MILLIS = 1_000L
        const val HALF_MILLIS = 500L
    }
}
