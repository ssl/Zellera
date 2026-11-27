package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.chat.ChatRoomHooks
import zel.room.unit.RoomUnit

class AreaChat(
    private val next: ChatRoomHooks = ChatRoomHooks.None,
) : ChatRoomHooks by next {
    override fun inMuteArea(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean =
        scope.room.furni.withBehaviour(MuteArea::class.java).any { furni ->
            (furni.behaviour as MuteArea).mutes(furni, unit.tile)
        } || next.inMuteArea(scope, unit)

    override fun hears(
        scope: RoomScope,
        speaker: RoomUnit,
        receiver: RoomUnit,
    ): Boolean {
        val tent = tentAt(scope, speaker)
        if (tent != null && receiver.tile !in tent.tiles()) return false
        return next.hears(scope, speaker, receiver)
    }

    override fun confinesToArea(
        scope: RoomScope,
        speaker: RoomUnit,
    ): Boolean = tentAt(scope, speaker) != null || next.confinesToArea(scope, speaker)

    private fun tentAt(
        scope: RoomScope,
        speaker: RoomUnit,
    ) = scope.room.furni
        .at(speaker.tile)
        .furni
        .firstOrNull { it.behaviour is Tent }
}
