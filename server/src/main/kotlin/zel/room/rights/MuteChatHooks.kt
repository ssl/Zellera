package zel.room.rights

import zel.room.RoomScope
import zel.room.chat.ChatRoomHooks

class MuteChatHooks(
    private val moderation: Moderation,
    private val next: ChatRoomHooks = ChatRoomHooks.None,
) : ChatRoomHooks by next {
    override fun mutedAll(scope: RoomScope): Boolean = moderation.allMuted(scope)

    override fun roomMuteSeconds(
        scope: RoomScope,
        playerId: Int,
    ): Int = moderation.muteLeft(scope, playerId)
}
