package zel.room.tick

import zel.protocol.generated.OutgoingCodec

interface PlayerChannels {
    fun <T> write(
        playerIds: List<Int>,
        codec: OutgoingCodec<T>,
        message: T,
    )

    fun flush(playerIds: Collection<Int>)
}
