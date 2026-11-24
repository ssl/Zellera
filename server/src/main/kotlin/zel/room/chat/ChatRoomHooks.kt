package zel.room.chat

import zel.room.RoomScope
import zel.room.unit.RoomUnit

interface ChatRoomHooks {
    fun hasRights(
        scope: RoomScope,
        playerId: Int,
    ): Boolean = playerId == scope.room.info.ownerId

    fun mutedAll(scope: RoomScope): Boolean = false

    fun roomMuteSeconds(
        scope: RoomScope,
        playerId: Int,
    ): Int = 0

    fun inMuteArea(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean = false

    fun hears(
        scope: RoomScope,
        speaker: RoomUnit,
        receiver: RoomUnit,
    ): Boolean = true

    companion object {
        val None: ChatRoomHooks = object : ChatRoomHooks {}
    }
}
