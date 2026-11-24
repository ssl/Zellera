package zel.net.server

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.protocol.InboundFrame
import zel.protocol.generated.CompleteDiffieHandshakeOutCodec
import zel.protocol.generated.InitDiffieHandshakeOutCodec
import zel.protocol.generated.UniqueMachineIDOutCodec
import zel.protocol.generated.WiredWebApiKeyResultOutCodec
import zel.protocol.typed.ClientPacket
import zel.protocol.typed.IncomingPacket
import zel.protocol.typed.WireFrame
import zel.util.PacketUtils

/** Logs every message both ways at debug level; bodies that carry a secret or an address show as `(hidden)`. */
class PacketLog : ChannelDuplexHandler() {
    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (msg is InboundFrame) {
            val name = (msg as? ClientPacket)?.name
            val body = if (name in HIDDEN) HIDDEN_BODY else msg.bodyText
            line(RECEIVED, msg.messageId, name, body)
        }
        ctx.fireChannelRead(msg)
    }

    override fun write(
        ctx: ChannelHandlerContext,
        msg: Any,
        promise: ChannelPromise,
    ) {
        if (msg is WireFrame) {
            val frame = msg.content()
            val at = frame.readerIndex()
            val body =
                if (msg.name in HIDDEN) {
                    HIDDEN_BODY
                } else {
                    PacketUtils.formatPacket(frame.slice(at + HEADER_BYTES, frame.readableBytes() - HEADER_BYTES))
                }
            line(SENT, frame.getUnsignedShort(at + LENGTH_BYTES), msg.name, body)
        }
        ctx.write(msg, promise)
    }

    /** Plain text, so the line reads the same on a console and in a file. */
    private fun line(
        direction: String,
        id: Int,
        name: String?,
        body: String,
    ) = log.debug("{} {} {}  {}", direction, id, name ?: UNNAMED, body)

    private companion object {
        val log: Logger = LoggerFactory.getLogger(PacketLog::class.java)
        const val RECEIVED = "recv"
        const val SENT = "sent"
        const val UNNAMED = "?"
        const val LENGTH_BYTES = 4
        const val HEADER_BYTES = 6
        const val HIDDEN_BODY = "(hidden)"

        /** The secret incoming messages, plus the handshake's keys, the machine id and wired API keys going out. */
        val HIDDEN: Set<String> =
            IncomingPacket.SECRET_PACKETS +
                setOf(
                    InitDiffieHandshakeOutCodec.name,
                    CompleteDiffieHandshakeOutCodec.name,
                    UniqueMachineIDOutCodec.name,
                    WiredWebApiKeyResultOutCodec.name,
                )
    }
}
