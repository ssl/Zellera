package zel.room.games

import zel.protocol.generated.OutgoingCodec
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.RoomOutput

class StepScope(
    val direct: RoomScope,
) : RoomScope by direct {
    override val out: RoomOutput =
        object : RoomOutput {
            override fun <T> send(
                lane: Lane,
                to: Audience,
                codec: OutgoingCodec<T>,
                message: T,
            ) = direct.out.send(if (lane == Lane.Now) Lane.AfterUnitUpdate else lane, to, codec, message)
        }
}
