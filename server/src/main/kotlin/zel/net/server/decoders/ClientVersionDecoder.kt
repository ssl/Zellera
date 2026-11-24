package zel.net.server.decoders

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.net.server.PacketDump
import zel.protocol.InboundFrame
import zel.protocol.generated.SSOTicketInCodec
import zel.protocol.typed.ClientPacket
import zel.protocol.typed.ClientVersion
import zel.protocol.typed.ClientVersions
import zel.protocol.typed.PacketRouter
import java.nio.charset.StandardCharsets

/** Names every frame by the ids of the client's version; a first message that is no hello of a known version closes. */
class ClientVersionDecoder(
    private val router: PacketRouter,
) : ChannelInboundHandlerAdapter() {
    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (msg !is InboundFrame) {
            ctx.fireChannelRead(msg)
            return
        }
        val wire = msg.messageId and WIRE_MASK
        val version = ClientVersions.of(ctx.channel()) ?: accept(ctx, msg, wire)
        if (version == null) {
            msg.release()
            ctx.close()
            return
        }
        val name = version.incoming(wire)
        if (PacketDump.enabled && router.routes(name) && name != SSOTicketInCodec.name) {
            PacketDump.line("c2s $wire 0 ${ByteBufUtil.hexDump(msg.body)}")
        }
        ctx.fireChannelRead(ClientPacket(wire, name, msg.body))
    }

    private fun accept(
        ctx: ChannelHandlerContext,
        first: InboundFrame,
        wire: Int,
    ): ClientVersion? {
        val named = peekString(first.body)
        val version = ClientVersions.saidBy(wire, named)
        if (version == null) {
            val from = ctx.channel().remoteAddress()
            log.warn("{} closed: unknown client version (wire {}, '{}')", from, wire, shown(named))
            return null
        }
        ClientVersions.accept(ctx.channel(), version)
        log.info("{} speaks {} ('{}')", ctx.channel().remoteAddress(), version.name, shown(named))
        if (PacketDump.enabled) PacketDump.line("# release ${version.name}")
        return version
    }

    /** A hello as a log line may carry it. */
    private fun shown(hello: String): String = hello.take(SHOWN_LENGTH).filter { it in ' '..'~' }

    private fun peekString(buf: ByteBuf): String {
        val at = buf.readerIndex()
        if (buf.readableBytes() < SHORT_SIZE) return ""
        val length = buf.getUnsignedShort(at)
        if (buf.readableBytes() < SHORT_SIZE + length) return ""
        return buf.toString(at + SHORT_SIZE, length, StandardCharsets.UTF_8)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ClientVersionDecoder::class.java)
        const val WIRE_MASK = 0xFFFF
        const val SHORT_SIZE = 2
        const val SHOWN_LENGTH = 64
    }
}
