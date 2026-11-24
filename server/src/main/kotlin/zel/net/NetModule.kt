package zel.net

import zel.core.Configuration
import zel.net.clients.ClientConnections
import zel.net.clients.FailedTickets
import zel.net.crypto.DiffieHellman
import zel.net.crypto.RsaCrypto
import zel.net.crypto.RsaKey
import zel.net.server.GameChannelPipeline
import zel.net.server.GamePort
import zel.net.server.WorkerSettings
import zel.net.server.handlers.AddressSlots
import zel.net.server.proxy.CloudflareEdge
import zel.net.server.proxy.ProxyTrust
import zel.protocol.typed.PacketDispatcher
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.slf4j.LoggerFactory
import java.io.File

class NetModule(
    private val configuration: Configuration,
    private val rsaKey: RsaKey?,
    private val packets: (ClientConnections) -> PacketDispatcher,
) {
    fun gamePort(): GamePort {
        val connections =
            ClientConnections(
                ::newHandshake,
                configuration.getInt("network.connections.max", DEFAULT_MAX_CONNECTIONS),
                FailedTickets(configuration),
            )
        val workers = WorkerSettings.from(configuration)
        val transport = TransportSettings.read(configuration)
        val packetPool = workers.packetPool()
        val bridgePool = workers.bridgePool()
        val pipeline =
            GameChannelPipeline(
                configuration,
                connections,
                packets(connections),
                packetPool,
                bridgePool,
                workers.perConnection,
                ProxyTrust(configuration, CloudflareEdge.configured(configuration)),
                transport,
                loadTls(),
                WireLimits.from(configuration),
                slots = AddressSlots(configuration.getInt("network.connections.per_address", DEFAULT_PER_ADDRESS)),
                perConnectionBytes = workers.perConnectionBytes,
            )
        return GamePort(
            configuration.getValue("network.listen.address", "127.0.0.1"),
            configuration.getInt("network.listen.port", DEFAULT_PORT),
            Listener.EventLoops(
                configuration.getInt("network.accept.threads"),
                configuration.getInt("network.io.threads"),
            ),
            transport,
            pipeline,
            connections,
            packetPool,
            bridgePool,
        )
    }

    private fun newHandshake(): DiffieHellman? =
        rsaKey?.let { DiffieHellman(RsaCrypto(it.modulusHex, it.privateExponentHex)) }

    private fun loadTls(): SslContext? {
        val certificate = File("ssl", "cert.pem")
        val privateKey = File("ssl", "privkey.pem")
        if (!certificate.exists() || !privateKey.exists()) return null
        return runCatching { SslContextBuilder.forServer(certificate, privateKey).build() }
            .onSuccess { log.info("WSS on: certificate loaded from ssl/") }
            .onFailure { log.warn("WSS off: the certificate in ssl/ does not load: {}", it.message) }
            .getOrNull()
    }

    companion object {
        private val log = LoggerFactory.getLogger(NetModule::class.java)
        private const val DEFAULT_PORT = 2096
        const val DEFAULT_MAX_CONNECTIONS = 20_000

        /** Room for a school or a shared connection behind one address. */
        const val DEFAULT_PER_ADDRESS = 20
    }
}
