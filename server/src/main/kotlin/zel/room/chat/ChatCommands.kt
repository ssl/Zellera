package zel.room.chat

import zel.room.RoomScope
import zel.room.unit.RoomUnit

class ChatCommands(
    private val host: ChatHost,
    private val posture: (RoomScope, RoomUnit, Int) -> Unit,
    private val room: (RoomScope, Int, String) -> Boolean = { _, _, _ -> false },
) {
    /** Runs the command [text] names; true when it ran, so the line is not said. */
    fun run(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
        text: String,
    ): Boolean {
        if (!text.startsWith(PREFIX)) return false
        val name =
            text
                .substring(PREFIX.length)
                .substringBefore(' ')
                .lowercase()
        return when (name) {
            SIT -> true.also { posture(scope, unit, 1) }
            STAND -> true.also { posture(scope, unit, 0) }
            else -> room(scope, playerId, text) || host.command(playerId, text)
        }
    }

    private companion object {
        const val PREFIX = ":"
        const val SIT = "sit"
        const val STAND = "stand"
    }
}
