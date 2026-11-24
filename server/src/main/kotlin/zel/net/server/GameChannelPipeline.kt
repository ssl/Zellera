// The bridge runs on a worker group of its own, a pipeline form Netty 4.2 deprecates.
@file:Suppress("DEPRECATION")

package zel.net.server

import zel.core.Configuration
import zel.net.TransportSettings
import zel.net.WireLimits
import zel.net.clients.ClientConnections
import zel.net.server.codec.WebSocketPayloads
import zel.net.server.decoders.ClientVersionDecoder
import zel.net.server.decoders.GameFrameDecoder
import zel.net.server.decoders.MessageBudget
import zel.net.server.encoders.GameFrameEncoder
import zel.net.server.handlers.AddressSlots
import zel.net.server.handlers.HealthAnswer
import zel.net.server.handlers.LivenessGuard
import zel.net.server.handlers.UpgradeGate
import zel.net.server.handlers.WriteStallGuard
import zel.net.server.proxy.ClientAddress
import zel.net.server.proxy.ProxyTrust
import zel.protocol.typed.PacketDispatcher
import zel.wired.variable.addon.VariablesBridge
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.HttpMessage
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.ssl.SslContext

class GameChannelPipeline(
    private val configuration: Configuration,
    private val connections: ClientConnections,
    private val dispatcher: PacketDispatcher,
    private val packets: WorkerPool,
    private val bridge: WorkerPool,
    private val perConnection: Int,
    private val trust: ProxyTrust,
    private val transport: TransportSettings,
    private val tls: SslContext?,
    private val limits: WireLimits = WireLimits(),
    private val slots: AddressSlots = AddressSlots(0),
    private val perConnectionBytes: Long = WorkerSettings.defaultPerConnectionBytes(),
) : ChannelInitializer<Channel>() {
    enum class Stage(
        val handlerName: String,
    ) {
        TLS("tls"),
        HEALTH("health"),
        UPGRADE_GATE("upgrade-gate"),
        BRIDGE_INTAKE("bridge-intake"),
        BRIDGE("bridge"),
        WEBSOCKET("websocket"),
        WEBSOCKET_BYTES("websocket-bytes"),
        FRAME_DECODER("frame-decoder"),
        FRAME_ENCODER("frame-encoder"),
        MESSAGE_BUDGET("message-budget"),
        VERSION("version"),
        LIVENESS("liveness"),
        WRITE_GUARD("write-guard"),
        PACKET_LOG("packet-log"),
        PACKETS("packets"),
    }

    public override fun initChannel(channel: Channel) {
        if (!connections.open(channel) || !holdsSlot(channel)) {
            channel.close()
            return
        }
        val pipeline = channel.pipeline()
        tls?.let { pipeline.addLast(Stage.TLS.handlerName, it.newHandler(channel.alloc())) }
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(RequestAggregator(limits.upgradeBytes, limits.bridgeBytes))
        pipeline.addLast(Stage.HEALTH.handlerName, HealthAnswer())
        pipeline.addLast(Stage.UPGRADE_GATE.handlerName, UpgradeGate(configuration, trust, slots))
        pipeline.addLast(Stage.BRIDGE_INTAKE.handlerName, BridgeAdmission(bridge, VariablesBridge.PATH))
        pipeline.addLast(bridge.executors, Stage.BRIDGE.handlerName, dispatcher.router.variablesBridge())
        pipeline.addLast(Stage.WEBSOCKET.handlerName, WebSocketServerProtocolHandler(webSocket()))
        pipeline.addLast(WebSocketFrameAggregator(limits.maxSocketMessageBytes))
        pipeline.addLast(Stage.WEBSOCKET_BYTES.handlerName, WebSocketPayloads(HTTP_ONLY.map { it.handlerName }))
        pipeline.addLast(Stage.FRAME_DECODER.handlerName, GameFrameDecoder(limits))
        pipeline.addLast(Stage.FRAME_ENCODER.handlerName, GameFrameEncoder())
        pipeline.addLast(Stage.MESSAGE_BUDGET.handlerName, MessageBudget(limits))
        pipeline.addLast(Stage.VERSION.handlerName, ClientVersionDecoder(dispatcher.router))
        pipeline.addLast(
            Stage.LIVENESS.handlerName,
            LivenessGuard(limits.pingGapSeconds, limits.pingTimeoutSeconds, limits.loginSeconds),
        )
        pipeline.addLast(
            Stage.WRITE_GUARD.handlerName,
            WriteStallGuard(transport.stalledSeconds, transport.backlogBytes.toLong()),
        )
        if (PacketDispatcher.showPackets) pipeline.addLast(Stage.PACKET_LOG.handlerName, PacketLog())
        pipeline.addLast(
            Stage.PACKETS.handlerName,
            PacketIntake(dispatcher, packets, perConnection, perConnectionBytes, limits.ordinaryFrameBytes),
        )
    }

    /** A proxy's sockets are its players': each is counted at the upgrade, under the address the proxy names. */
    private fun holdsSlot(channel: Channel): Boolean {
        val peer = ClientAddress.peer(channel)
        return trust.forwards(peer) || slots.hold(channel, peer)
    }

    private fun webSocket(): WebSocketServerProtocolConfig =
        WebSocketServerProtocolConfig
            .newBuilder()
            .websocketPath(PATH)
            .subprotocols(SUBPROTOCOL)
            .checkStartsWith(false)
            .allowExtensions(false)
            .maxFramePayloadLength(limits.maxSocketMessageBytes)
            .handleCloseFrames(true)
            .dropPongFrames(true)
            .build()

    /** An upgrade request has no body, so only the bridge's path may send one. */
    internal class RequestAggregator(
        private val upgradeBytes: Int = WireLimits.DEFAULT_UPGRADE_BYTES,
        bridgeBytes: Int = WireLimits.DEFAULT_BRIDGE_BYTES,
    ) : HttpObjectAggregator(bridgeBytes) {
        override fun isContentLengthInvalid(
            start: HttpMessage,
            maxContentLength: Int,
        ): Boolean {
            val bridge = (start as? HttpRequest)?.uri()?.substringBefore('?') == VariablesBridge.PATH
            return super.isContentLengthInvalid(start, if (bridge) maxContentLength else upgradeBytes)
        }
    }

    private companion object {
        const val PATH = "/websocket"
        const val SUBPROTOCOL = "zellera"
        val HTTP_ONLY = listOf(Stage.HEALTH, Stage.UPGRADE_GATE, Stage.BRIDGE_INTAKE, Stage.BRIDGE)
    }
}
