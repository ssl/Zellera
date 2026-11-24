package zel.protocol

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import zel.util.PacketUtils

/** The receiver releases the body when done. */
open class InboundFrame(
    val messageId: Int,
    bytes: ByteBuf?,
) {
    val body: ByteBuf = if (bytes == null || !bytes.isReadable) Unpooled.EMPTY_BUFFER else bytes

    val bodyText: String get() = PacketUtils.formatPacket(body)

    fun release(): Boolean = body.release()
}
