package zel.room.unit.bot

import zel.room.RoomScope
import zel.room.chat.ChatBubble
import zel.room.chat.ChatKind
import zel.room.chat.ChatOut
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

object UnitSpeech {
    @Suppress("LongParameterList") // one per bubble field
    fun toRoom(
        scope: RoomScope,
        speaker: RoomUnit,
        kind: ChatKind,
        text: String,
        style: Int,
        gesture: Int = 0,
        width: Int = ChatBubble.USER_WIDTH,
        hideName: Boolean? = null,
        lane: Lane = Lane.Now,
    ) {
        scope.room.units.all().forEach { receiver ->
            val playerId = (receiver.kind as? UnitKind.Player)?.playerId ?: return@forEach
            val bubble =
                ChatBubble(speaker.index, text, gesture, style, ChatBubble.NO_TRACKING, receiver.index, width, hideName)
            ChatOut.send(scope, kind, playerId, bubble, lane)
        }
    }

    @Suppress("LongParameterList") // the bubble's fields a bot line sets
    fun whisper(
        scope: RoomScope,
        speaker: RoomUnit,
        target: RoomUnit,
        text: String,
        style: Int,
        width: Int = ChatBubble.USER_WIDTH,
        hideName: Boolean? = null,
    ) {
        val playerId = (target.kind as? UnitKind.Player)?.playerId ?: return
        val bubble = ChatBubble(speaker.index, text, 0, style, ChatBubble.NO_TRACKING, target.index, width, hideName)
        ChatOut.send(scope, ChatKind.Whisper, playerId, bubble, Lane.Now)
    }
}
