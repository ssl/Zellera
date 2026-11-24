package zel.room.furni.behaviour.state

import zel.room.Room

/** Call under the room lock. */
object RoomControllers {
    const val HAND_ITEM_BLOCK = "control.handitems"
    const val CHOOSER_DISABLE = "control.chooser"
    const val INVISIBLE_FURNI = "control.invisible"
    const val WIRED_DISABLE = "control.wired"

    fun handItemsBlocked(room: Room): Boolean = on(room, HAND_ITEM_BLOCK)

    fun chooserDisabled(room: Room): Boolean = on(room, CHOOSER_DISABLE)

    fun invisibleFurni(room: Room): Boolean = on(room, INVISIBLE_FURNI)

    fun configurationStates(
        room: Room,
        chooserDisabled: Boolean = chooserDisabled(room),
    ): List<Boolean> = listOf(handItemsBlocked(room), chooserDisabled, false, invisibleFurni(room), false)

    private fun on(
        room: Room,
        type: String,
    ): Boolean = room.furni.withInteraction(type).any { it.state.trim() == ON }

    const val ON = "1"
    const val OFF = "0"
}
