package zel.wired.trigger

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing

class ClickUser(
    override val furniId: Int,
    override val settings: BoxSettings,
) : TriggerBox {
    override val needsUser = true

    private val blocksMenu = (settings.ints.firstOrNull() ?: BLOCKS_MENU) == BLOCKS_MENU

    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = event is RoomEvent.ClickUser

    /** A block-menu box keeps the menu shut only when its stack held for this click, for a user, bot or pet alike. */
    override fun passed(firing: Firing) {
        if (blocksMenu) (firing.event as? RoomEvent.ClickUser)?.menu?.block()
    }

    companion object {
        const val CODE = "wf_trg_click_user"
        const val BLOCKS_MENU = 1
        const val DOES_NOT_TURN = 1
    }
}
