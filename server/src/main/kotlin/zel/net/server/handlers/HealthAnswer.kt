package zel.net.server.handlers

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion
import zel.core.BuildInfo

/** Answers `GET /health` with the running version; the listener only opens once the game is loaded. */
class HealthAnswer : ChannelInboundHandlerAdapter() {
    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (msg !is FullHttpRequest || msg.method() != HttpMethod.GET || msg.uri().substringBefore('?') != PATH) {
            ctx.fireChannelRead(msg)
            return
        }
        msg.release()
        val body = Unpooled.copiedBuffer(BODY, Charsets.UTF_8)
        val answer = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, body)
        answer.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        answer.headers().set(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE)
        HttpUtil.setContentLength(answer, body.readableBytes().toLong())
        HttpUtil.setKeepAlive(answer, false)
        ctx.writeAndFlush(answer).addListener(ChannelFutureListener.CLOSE)
    }

    companion object {
        const val PATH = "/health"
        val BODY = """{"status":"ok","version":"${BuildInfo.VERSION}"}"""
    }
}
