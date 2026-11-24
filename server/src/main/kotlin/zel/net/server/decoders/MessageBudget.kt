package zel.net.server.decoders

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.net.WireLimits
import zel.net.clients.ClientConnection
import zel.protocol.InboundFrame
import zel.util.monotonicMillis

/**
 * What one connection may send: a budget for all messages and one for each kind. A budget holds one window's
 * worth and fills steadily, so a burst up to that passes and the pace after it is the limit, with no moment at
 * which a new window doubles it. A message over a budget is dropped. Runs on the event loop, so nothing is locked.
 */
class MessageBudget(
    private val limits: WireLimits = WireLimits(),
    private val millis: () -> Long = ::monotonicMillis,
) : MessageToMessageDecoder<InboundFrame>() {
    private class Allowance(
        var left: Double,
        var at: Long,
    )

    private val total = Allowance(limits.totalPerWindow.toDouble(), millis())
    private val byKind = HashMap<Int, Allowance>()
    private var reportedAt: Long? = null

    override fun decode(
        ctx: ChannelHandlerContext,
        message: InboundFrame,
        out: MutableList<Any>,
    ) {
        val now = millis()
        refill(total, limits.totalPerWindow, now)
        val kind = byKind[message.messageId] ?: newKind(message.messageId, now)
        refill(kind, limits.perMessagePerWindow, now)
        if (total.left < 1 || kind.left < 1) {
            if (total.left < 1) report(ctx, now)
            message.release()
            return
        }
        total.left -= 1
        kind.left -= 1
        out += message
    }

    private fun refill(
        allowance: Allowance,
        perWindow: Int,
        now: Long,
    ) {
        val gained = (now - allowance.at).toDouble() * perWindow / limits.rateWindowMillis
        allowance.left = (allowance.left + gained).coerceAtMost(perWindow.toDouble())
        allowance.at = now
    }

    /** A kind whose budget is full again needs no entry, so the map stays small whatever ids arrive. */
    private fun newKind(
        id: Int,
        now: Long,
    ): Allowance {
        val whole = limits.perMessagePerWindow.toDouble()
        if (byKind.size >= MAX_KINDS) {
            byKind.values.removeIf { kind ->
                refill(kind, limits.perMessagePerWindow, now)
                kind.left >= whole
            }
        }
        return Allowance(whole, now).also { byKind[id] = it }
    }

    private fun report(
        ctx: ChannelHandlerContext,
        now: Long,
    ) {
        if (reportedAt?.let { now - it < limits.rateWindowMillis } == true) return
        reportedAt = now
        val who =
            ctx
                .channel()
                .attr(ClientConnection.KEY)
                .get()
                ?.player
                ?.profile
                ?.name
        log.warn(
            "{} sends more than {} messages in {} ms; the rest is dropped",
            who ?: ctx.channel().remoteAddress(),
            limits.totalPerWindow,
            limits.rateWindowMillis,
        )
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(MessageBudget::class.java)
        const val MAX_KINDS = 128
    }
}
