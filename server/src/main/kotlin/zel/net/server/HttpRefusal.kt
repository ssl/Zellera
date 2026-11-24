package zel.net.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion

internal enum class HttpRefusal(
    private val status: HttpResponseStatus,
    private val retryAfterSeconds: Int = 0,
) {
    FORBIDDEN(HttpResponseStatus.FORBIDDEN),
    NOT_FOUND(HttpResponseStatus.NOT_FOUND),

    TOO_MANY(HttpResponseStatus.TOO_MANY_REQUESTS),

    BUSY(HttpResponseStatus.SERVICE_UNAVAILABLE, retryAfterSeconds = 1),
    ;

    fun close(ctx: ChannelHandlerContext) {
        val answer = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.EMPTY_BUFFER)
        HttpUtil.setContentLength(answer, 0)
        HttpUtil.setKeepAlive(answer, false)
        if (retryAfterSeconds > 0) answer.headers().set(HttpHeaderNames.RETRY_AFTER, retryAfterSeconds)
        ctx.writeAndFlush(answer).addListener(ChannelFutureListener.CLOSE)
    }
}
