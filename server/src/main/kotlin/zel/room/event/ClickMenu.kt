package zel.room.event

/** Whether a click on a unit keeps its menu shut: a block-menu click-user stack that held for the click shuts it. */
class ClickMenu {
    var blocked: Boolean = false
        private set

    fun block() {
        blocked = true
    }
}
