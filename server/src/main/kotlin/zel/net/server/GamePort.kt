package zel.net.server

import zel.core.load.ServerGauges
import zel.net.Listener
import zel.net.TransportSettings
import zel.net.clients.ClientConnections
import io.netty.channel.ChannelHandler
import java.util.concurrent.atomic.AtomicBoolean

class GamePort(
    address: String,
    port: Int,
    loops: Listener.EventLoops,
    transport: TransportSettings,
    pipeline: ChannelHandler,
    val connections: ClientConnections,
    private val packets: WorkerPool,
    private val bridge: WorkerPool,
) {
    private val listener = Listener("Game server", address, port, loops, transport, pipeline)
    private val running = AtomicBoolean(true)

    fun start() = listener.start()

    fun stop() {
        if (!running.getAndSet(false)) return
        listener.stop()
        connections.connections.toList().forEach(connections::close)
        packets.close()
        bridge.close()
    }

    fun packetOccupancy(): ServerGauges.Occupancy? = packets.occupancy().takeIf { running.get() }
}
