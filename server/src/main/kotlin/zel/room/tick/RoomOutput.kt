package zel.room.tick

import zel.protocol.generated.OutgoingCodec

interface RoomOutput {
    fun <T> send(
        lane: Lane,
        to: Audience,
        codec: OutgoingCodec<T>,
        message: T,
    )
}
