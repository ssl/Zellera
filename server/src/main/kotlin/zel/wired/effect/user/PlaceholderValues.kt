package zel.wired.effect.user

import zel.room.unit.RoomUnit
import zel.wired.firing.Firing

fun interface PlaceholderValues {
    fun of(
        firing: Firing,
        receiver: RoomUnit?,
    ): Map<String, String>

    companion object {
        val None: PlaceholderValues = PlaceholderValues { _, _ -> emptyMap() }
    }
}
