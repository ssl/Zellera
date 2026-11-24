package zel.room.chat

import zel.protocol.generated.ChatOut
import zel.protocol.generated.ChatOutCodec
import zel.protocol.generated.ShoutOut
import zel.protocol.generated.ShoutOutCodec
import zel.protocol.generated.WhisperOut
import zel.protocol.generated.WhisperOutCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane

object ChatOut {
    fun send(
        scope: RoomScope,
        kind: ChatKind,
        playerId: Int,
        bubble: ChatBubble,
        lane: Lane = Lane.Now,
    ) {
        val to = Audience.Player(playerId)
        val b = bubble
        when (kind) {
            ChatKind.Talk -> {
                val m =
                    ChatOut(
                        b.unitIndex,
                        b.text,
                        b.gesture,
                        b.style,
                        emptyList(),
                        b.trackingId,
                        b.receiverIndex,
                        b.width,
                        b.hideName,
                        null,
                    )
                scope.out.send(lane, to, ChatOutCodec, m)
            }

            ChatKind.Shout -> {
                val m =
                    ShoutOut(
                        b.unitIndex,
                        b.text,
                        b.gesture,
                        b.style,
                        emptyList(),
                        b.trackingId,
                        b.receiverIndex,
                        b.width,
                        b.hideName,
                        null,
                    )
                scope.out.send(lane, to, ShoutOutCodec, m)
            }

            ChatKind.Whisper -> {
                val m =
                    WhisperOut(
                        b.unitIndex,
                        b.text,
                        b.gesture,
                        b.style,
                        emptyList(),
                        b.trackingId,
                        b.receiverIndex,
                        b.width,
                        b.hideName,
                        null,
                    )
                scope.out.send(lane, to, WhisperOutCodec, m)
            }
        }
    }
}
