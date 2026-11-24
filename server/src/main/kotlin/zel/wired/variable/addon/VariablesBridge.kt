package zel.wired.variable.addon

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import zel.room.RoomRegistry
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.util.ReferenceCountUtil
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class VariablesBridge(
    private val secret: String,
    private val registry: RoomRegistry,
    private val ops: BridgeOps,
) : ChannelInboundHandlerAdapter() {
    override fun channelRead(
        ctx: ChannelHandlerContext,
        msg: Any,
    ) {
        if (msg !is FullHttpRequest || !msg.uri().substringBefore('?').equals(PATH)) {
            ctx.fireChannelRead(msg)
            return
        }
        try {
            answer(ctx, msg)
        } finally {
            ReferenceCountUtil.release(msg)
        }
    }

    private fun answer(
        ctx: ChannelHandlerContext,
        request: FullHttpRequest,
    ) {
        when {
            secret.isEmpty() -> {
                reply(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE, "")
            }

            !allowed(ctx, request) -> {
                reply(ctx, HttpResponseStatus.FORBIDDEN, "")
            }

            request.method() != HttpMethod.POST -> {
                reply(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, "")
            }

            request.content().readableBytes() > MAX_BODY -> {
                reply(ctx, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, "")
            }

            else -> {
                val body = parse(request)
                if (body == null) {
                    reply(ctx, HttpResponseStatus.BAD_REQUEST, "")
                } else {
                    reply(ctx, HttpResponseStatus.OK, call(body).toString())
                }
            }
        }
    }

    private fun allowed(
        ctx: ChannelHandlerContext,
        request: FullHttpRequest,
    ): Boolean {
        val peer = (ctx.channel().remoteAddress() as? InetSocketAddress)?.address
        val headers = request.headers()
        val direct = PROXY_HEADERS.none { headers.contains(it) }
        val sent = headers.get(SECRET_HEADER).orEmpty().toByteArray(StandardCharsets.UTF_8)
        val matches = MessageDigest.isEqual(sent, secret.toByteArray(StandardCharsets.UTF_8))
        return peer?.isLoopbackAddress == true && direct && matches
    }

    private fun parse(request: FullHttpRequest): JsonObject? =
        try {
            JsonParser.parseString(request.content().toString(StandardCharsets.UTF_8)) as? JsonObject
        } catch (_: JsonSyntaxException) {
            null
        }

    @Suppress("TooGenericExceptionCaught") // whatever a call breaks on, the web gets its well-formed "maintenance"
    private fun call(body: JsonObject): JsonObject =
        try {
            val op = body.get("op")?.asString.orEmpty()
            val roomId = body.get("roomId")?.asInt ?: throw BridgeRefusal(BAD_REQUEST, NOT_FOUND)
            registry.load(roomId)
            val data = registry.withRoom(roomId) { scope -> ops.run(scope, op, body) } ?: JsonNull.INSTANCE
            JsonObject().apply {
                addProperty("ok", true)
                add("data", data)
            }
        } catch (refused: BridgeRefusal) {
            refusal(refused.status, refused.code)
        } catch (e: RuntimeException) {
            log.warn("wired variables bridge call failed", e)
            refusal(UNAVAILABLE, "maintenance")
        }

    private fun refusal(
        status: Int,
        code: String,
    ) = JsonObject().apply {
        addProperty("ok", false)
        addProperty("status", status)
        addProperty("error", code)
    }

    private fun reply(
        ctx: ChannelHandlerContext,
        status: HttpResponseStatus,
        body: String,
    ) {
        val content = Unpooled.copiedBuffer(body, StandardCharsets.UTF_8)
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content)
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
        response.headers().set(HttpHeaderNames.CONNECTION, "close")
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

    companion object {
        const val PATH = "/internal/wired-variables/v1"
        private const val SECRET_HEADER = "X-Bridge-Secret"
        private const val MAX_BODY = 64 * 1024
        private const val BAD_REQUEST = 400
        private const val UNAVAILABLE = 503
        private const val NOT_FOUND = "wired.variables.not_found"
        private val PROXY_HEADERS = listOf("X-Forwarded-For", "Forwarded", "X-Real-IP")
        private val log = LoggerFactory.getLogger(VariablesBridge::class.java)
    }
}
