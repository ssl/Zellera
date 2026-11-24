package zel.room.rights

import zel.protocol.generated.OutgoingCodec

interface Reply {
    fun <T> send(
        codec: OutgoingCodec<T>,
        message: T,
    )
}
