package zel.net

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.WriteBufferWaterMark
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Listener(
    private val name: String,
    private val address: String,
    private val port: Int,
    loops: EventLoops,
    transport: TransportSettings,
    pipeline: ChannelHandler,
    private val binder: (ServerBootstrap) -> ChannelFuture = { it.bind(address, port) },
) {
    /** A count of 0 lets the library choose. */
    data class EventLoops(
        val acceptThreads: Int,
        val ioThreads: Int,
    )

    class BindFailed(
        address: String,
        port: Int,
        cause: Throwable?,
    ) : RuntimeException("Failed to listen on $address:$port", cause)

    private val acceptLoops = loops(loops.acceptThreads, "$name accept")
    private val ioLoops = loops(loops.ioThreads, "$name io")

    val bootstrap: ServerBootstrap =
        ServerBootstrap()
            .group(acceptLoops, ioLoops)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(
                ChannelOption.WRITE_BUFFER_WATER_MARK,
                WriteBufferWaterMark(transport.resumeBytes, transport.pauseBytes),
            ).childHandler(pipeline)

    private var socket: Channel? = null

    val isListening: Boolean get() = socket != null

    fun start() {
        val attempt = binder(bootstrap).awaitUninterruptibly()
        attempt.cause()?.let { throw BindFailed(address, port, it) }
        socket = attempt.channel()
        log.info("{} listening on {}:{}", name, address, port)
    }

    fun stop() {
        socket?.close()?.syncUninterruptibly()
        socket = null
        acceptLoops.shutdownGracefully()
        ioLoops.shutdownGracefully()
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Listener::class.java)

        fun loops(
            count: Int,
            label: String,
        ): EventLoopGroup = MultiThreadIoEventLoopGroup(count, DefaultThreadFactory(label), NioIoHandler.newFactory())
    }
}
