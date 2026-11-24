package zel.room.chat

import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.entry.RoomLeaving

class DoorChatHooks(
    private val leaving: RoomLeaving,
    private val next: ChatRoomHooks = ChatRoomHooks.None,
) : ChatRoomHooks by next {
    override fun hasRights(
        scope: RoomScope,
        playerId: Int,
    ): Boolean = leaving.doorOf(scope.room).levelOf(playerId) >= DoorCheck.RIGHTS
}
