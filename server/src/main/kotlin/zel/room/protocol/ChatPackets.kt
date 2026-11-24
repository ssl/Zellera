package zel.room.protocol

import zel.game.permissions.isAnyOwner
import zel.protocol.generated.CancelTypingInCodec
import zel.protocol.generated.ChatInCodec
import zel.protocol.generated.DanceInCodec
import zel.protocol.generated.GetCustomRoomFilterInCodec
import zel.protocol.generated.PlayerExpressionInCodec
import zel.protocol.generated.RoomChatSettingsOut
import zel.protocol.generated.RoomChatSettingsOutCodec
import zel.protocol.generated.RoomFilterSettingsOut
import zel.protocol.generated.RoomFilterSettingsOutCodec
import zel.protocol.generated.RoomInfoUpdatedOut
import zel.protocol.generated.RoomInfoUpdatedOutCodec
import zel.protocol.generated.RoomVisualizationSettingsOutCodec
import zel.protocol.generated.ShoutInCodec
import zel.protocol.generated.SignInCodec
import zel.protocol.generated.StartTypingInCodec
import zel.protocol.generated.UpdateRoomFilterInCodec
import zel.protocol.generated.WhisperInCodec
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.RoomRegistry
import zel.room.RoomScope
import zel.room.chat.ChatKind
import zel.room.chat.Gestures
import zel.room.chat.RoomChat
import zel.room.chat.RoomWords
import zel.room.map.MapMessages
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit

class ChatPackets(
    private val rooms: RoomRegistry,
    chat: RoomChat,
    gestures: Gestures,
    private val words: RoomWords,
) : PacketPackage {
    override val name = "chat"
    override val routes =
        listOf(
            PacketRoute(ChatInCodec) { session, m ->
                withAvatar(session) { scope, unit -> chat.say(scope, unit, ChatKind.Talk, m.string1, m.int1, m.int2) }
            },
            PacketRoute(ShoutInCodec) { session, m ->
                withAvatar(session) { scope, unit -> chat.say(scope, unit, ChatKind.Shout, m.string1, m.int1) }
            },
            PacketRoute(WhisperInCodec) { session, m ->
                withAvatar(session) { scope, unit -> chat.say(scope, unit, ChatKind.Whisper, m.message, m.styleId) }
            },
            PacketRoute(StartTypingInCodec) { session, _ ->
                withAvatar(session) { scope, unit -> gestures.typing(scope, unit, true) }
            },
            PacketRoute(CancelTypingInCodec) { session, _ ->
                withAvatar(session) { scope, unit -> gestures.typing(scope, unit, false) }
            },
            PacketRoute(DanceInCodec) { session, m ->
                withAvatar(session) { scope, unit -> gestures.dance(scope, unit, m.danceId) }
            },
            PacketRoute(PlayerExpressionInCodec) { session, m ->
                withAvatar(session) { scope, unit -> gestures.expression(scope, unit, m.int1) }
            },
            PacketRoute(SignInCodec) { session, m ->
                withAvatar(session) { scope, unit -> gestures.sign(scope, unit, m.signId) }
            },
            PacketRoute(GetCustomRoomFilterInCodec) { session, m ->
                asOwner(session, m.roomId, ::sendWords)
            },
            PacketRoute(UpdateRoomFilterInCodec) { session, m ->
                asOwner(session, m.roomId) { scope, playerId ->
                    words.change(scope.room, m.add, m.word)
                    sendWords(scope, playerId)
                    val out = scope.out
                    val info = scope.room.info
                    out.send(Lane.Now, Audience.Room, RoomInfoUpdatedOutCodec, RoomInfoUpdatedOut(info.id))
                    out.send(
                        Lane.Now,
                        Audience.Room,
                        RoomVisualizationSettingsOutCodec,
                        MapMessages.visualization(info.visuals),
                    )
                    out.send(
                        Lane.Now,
                        Audience.Room,
                        RoomChatSettingsOutCodec,
                        RoomChatSettingsOut(info.moderation.floodFilter),
                    )
                }
            },
        )

    private fun sendWords(
        scope: RoomScope,
        playerId: Int,
    ) {
        val list = RoomFilterSettingsOut(words.words(scope.room))
        scope.out.send(Lane.Now, Audience.Player(playerId), RoomFilterSettingsOutCodec, list)
    }

    private fun withAvatar(
        session: PacketSession,
        block: (RoomScope, RoomUnit) -> Unit,
    ) {
        val playerId = playerId(session) ?: return
        rooms.withPlayer(playerId, block)
    }

    private fun asOwner(
        session: PacketSession,
        roomId: Int,
        block: (RoomScope, Int) -> Unit,
    ) {
        val player = session.client.player ?: return
        val playerId = player.profile.id
        rooms.withRoom(roomId) { scope ->
            if (scope.room.info.ownerId == playerId || player.isAnyOwner) block(scope, playerId)
        }
    }

    private fun playerId(session: PacketSession): Int? =
        session.client.player
            ?.profile
            ?.id
}
