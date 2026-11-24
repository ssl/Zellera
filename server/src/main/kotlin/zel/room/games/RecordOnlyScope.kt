package zel.room.games

import zel.protocol.generated.OutgoingCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.RoomOutput

class RecordOnlyScope(
    inner: RoomScope,
) : RoomScope by inner {
    override val out: RoomOutput =
        object : RoomOutput {
            override fun <T> send(
                lane: Lane,
                to: Audience,
                codec: OutgoingCodec<T>,
                message: T,
            ) = Unit
        }
}
