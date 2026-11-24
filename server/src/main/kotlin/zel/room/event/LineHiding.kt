package zel.room.event

import zel.room.RoomScope

class LineHiding(
    holdsAfterLine: Boolean = false,
) {
    var hidden: Boolean = false
        private set

    private val held: MutableList<(RoomScope) -> Unit>? = if (holdsAfterLine) ArrayList() else null

    private var sent = false

    /** What runs once the line is out; null when this line holds nothing back or has already gone out. */
    val afterLine: MutableList<(RoomScope) -> Unit>? get() = held.takeUnless { sent }

    fun hide() {
        hidden = true
    }

    fun lineSent(scope: RoomScope) {
        val waiting = afterLine.orEmpty().toList()
        sent = true
        waiting.forEach { it(scope) }
    }
}
