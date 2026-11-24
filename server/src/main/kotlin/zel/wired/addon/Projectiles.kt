package zel.wired.addon

import zel.room.RoomScope
import zel.wired.effect.furni.FloorMove

fun interface Projectiles {
    fun fly(
        scope: RoomScope,
        move: FloorMove,
        distance: Double,
        steps: List<Double>,
    )

    companion object {
        val None = Projectiles { _, _, _, _ -> }
    }
}
