package zel.protocol

import io.netty.buffer.ByteBufAllocator
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PacketWriter
import zel.protocol.typed.ClientVersion
import zel.protocol.typed.WireFrame

/** A server message before it meets a client. */
class Outbound<T>(
    val codec: OutgoingCodec<T>,
    val message: T,
) {
    /** Int length of id plus body, short wire id, body; null when the version has no such message. */
    fun frame(
        alloc: ByteBufAllocator,
        version: ClientVersion,
    ): WireFrame? {
        val id = version.outgoing(codec.name) ?: return null
        val body = PacketWriter().also { codec.write(it, message) }.toByteArray()
        val bytes =
            alloc
                .buffer(LENGTH_BYTES + ID_BYTES + body.size)
                .writeInt(ID_BYTES + body.size)
                .writeShort(id)
                .writeBytes(body)
        return WireFrame(bytes, codec.name)
    }

    private companion object {
        const val LENGTH_BYTES = 4
        const val ID_BYTES = 2
    }
}
