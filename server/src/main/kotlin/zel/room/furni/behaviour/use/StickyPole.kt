package zel.room.furni.behaviour.use

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable

/**
 * A sticky pole does nothing when used: no state change and no whisper. It is still [Usable] so a use does not fall
 * through to the default toggle.
 */
object StickyPole : Usable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) = Unit
}
