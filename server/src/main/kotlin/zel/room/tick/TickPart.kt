package zel.room.tick

import zel.room.RoomScope

fun interface TickPart {
    fun run(scope: RoomScope)

    companion object {
        val None: TickPart = TickPart { }

        fun inOrder(vararg parts: TickPart): TickPart = TickPart { scope -> parts.forEach { it.run(scope) } }
    }
}
