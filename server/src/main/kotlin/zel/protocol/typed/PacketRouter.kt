package zel.protocol.typed

import zel.ext.ExtensionBus
import zel.net.clients.ClientConnection
import zel.protocol.generated.MalformedPacketException
import zel.protocol.generated.OutgoingCodec
import zel.wired.variable.addon.VariableAddonPackets
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInboundHandlerAdapter
import org.slf4j.LoggerFactory

class PacketRouter(
    packages: List<PacketPackage>,
    private val allowed: AllowedValues = AllowedValues(),
) {
    private val log = LoggerFactory.getLogger(PacketRouter::class.java)
    private val byName: Map<String, PacketRoute<*>>
    private val variablesBridge = packages.filterIsInstance<VariableAddonPackets>().firstOrNull()

    init {
        val routes = packages.flatMap { it.routes }
        val dup = routes.groupBy { it.codec.name }.filterValues { it.size > 1 }.keys
        require(dup.isEmpty()) { "messages routed twice: $dup" }
        byName = routes.associateBy { it.codec.name }
    }

    fun variablesBridge(): ChannelHandler = variablesBridge?.bridgeHandler() ?: ChannelInboundHandlerAdapter()

    fun routes(message: String?): Boolean = message in byName

    fun dispatch(
        client: ClientConnection,
        message: ClientPacket,
    ) {
        val route = byName[message.name]
        when {
            route == null -> {
                val playerId = client.player?.profile?.id
                if (!ExtensionBus.unrouted(playerId, message.name, ByteBufUtil.getBytes(message.body))) {
                    unrouted(client, message)
                }
            }

            route.requiresPlayer && client.player == null -> {
                log.debug(
                    "{} needs an authenticated session",
                    route.codec.name,
                )
            }

            else -> {
                run(route, client, message)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught") // must not reach the connection's handler
    private fun run(
        route: PacketRoute<*>,
        client: ClientConnection,
        message: ClientPacket,
    ) {
        val name = route.codec.name
        val body = ByteBufUtil.getBytes(message.body)
        client.warnings.current = IncomingPacket(name)
        try {
            val left = route.dispatch(ClientSession(client), body, allowed)
            if (left > 0) client.warnings.invalid(name, "$left bytes left over")
        } catch (_: java.nio.BufferUnderflowException) {
            client.warnings.invalid(name, "truncated, ${body.size} bytes")
        } catch (e: MalformedPacketException) {
            client.warnings.invalid(name, "${e.message}")
        } catch (e: Exception) {
            client.warnings.failed(e)
        }
    }

    private fun unrouted(
        client: ClientConnection,
        message: ClientPacket,
    ) {
        val name = message.name
        if (name == null) {
            val label = "id ${message.messageId}"
            client.warnings.current = IncomingPacket(label)
            client.warnings.invalid(label, "unknown id")
        } else {
            log.debug("no handler for {} ({})", name, message.messageId)
        }
    }

    private class ClientSession(
        override val client: ClientConnection,
    ) : PacketSession {
        override fun <T> send(
            codec: OutgoingCodec<T>,
            message: T,
        ) = client.send(codec, message)
    }
}
