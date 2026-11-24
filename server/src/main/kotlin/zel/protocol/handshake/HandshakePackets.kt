package zel.protocol.handshake

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.game.players.DeviceReading
import zel.net.clients.ClientConnections
import zel.net.crypto.DiffieHellman
import zel.net.server.GameChannelPipeline
import zel.net.server.Rc4Stream
import zel.protocol.Outbound
import zel.protocol.generated.ClientHelloInCodec
import zel.protocol.generated.CompleteDiffieHandshakeInCodec
import zel.protocol.generated.CompleteDiffieHandshakeOut
import zel.protocol.generated.CompleteDiffieHandshakeOutCodec
import zel.protocol.generated.DisconnectInCodec
import zel.protocol.generated.InitDiffieHandshakeInCodec
import zel.protocol.generated.InitDiffieHandshakeOut
import zel.protocol.generated.InitDiffieHandshakeOutCodec
import zel.protocol.generated.UniqueIDInCodec
import zel.protocol.generated.UniqueMachineIDOut
import zel.protocol.generated.UniqueMachineIDOutCodec
import zel.protocol.typed.ClientVersions
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.util.monotonicMillis
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicLong

class HandshakePackets(
    private val clients: ClientConnections,
    login: Login,
    machineIds: MachineIdBinding,
) : PacketPackage {
    override val name = "handshake"

    private val missingKeyWarnedAt = AtomicLong(NEVER)

    override val routes =
        listOf(
            PacketRoute(ClientHelloInCodec, requiresPlayer = false) { session, m ->
                session.client.releaseName = m.release.trim()
            },
            PacketRoute(InitDiffieHandshakeInCodec, requiresPlayer = false) { session, _ ->
                initDiffie(session)
            },
            PacketRoute(CompleteDiffieHandshakeInCodec, requiresPlayer = false) { session, m ->
                completeDiffie(session, m.sharedKey)
            },
            PacketRoute(SsoTicketReader, requiresPlayer = false) { session, m ->
                login.admit(session.client, m.string1)
            },
            PacketRoute(UniqueIDInCodec, requiresPlayer = false) { session, m ->
                // the device the client measured; its id piece is the machine id the bans and the mod tool know
                val device = DeviceReading.parse(m.storedMachineId)
                session.client.device = device
                val machineId = machineIds.bind(session.client, device)
                session.send(UniqueMachineIDOutCodec, UniqueMachineIDOut(machineId))
            },
            PacketRoute(DisconnectInCodec) { session, _ ->
                clients.close(session.client)
            },
        )

    /** Once per connection; a second ask, or one with encryption off, ends the connection. */
    private fun initDiffie(session: PacketSession) {
        val diffie = session.client.startHandshake()
        if (diffie == null) {
            if (session.client.handshake == null) warnAboutMissingKey()
            clients.close(session.client)
        } else {
            val answer = InitDiffieHandshakeOut(diffie.signedPrime, diffie.signedGenerator)
            session.send(InitDiffieHandshakeOutCodec, answer)
        }
    }

    /** The client and the server disagree about the key exchange; said at most once a minute. */
    private fun warnAboutMissingKey() {
        val now = monotonicMillis()
        val last = missingKeyWarnedAt.get()
        if (last != NEVER && now - last < MISSING_KEY_WARNING_GAP_MILLIS) return
        if (!missingKeyWarnedAt.compareAndSet(last, now)) return
        log.warn(
            "A client asked for the key exchange, but this server has no key, so its connection was closed. " +
                "Take ZEL_KEY_EXCHANGE=0 out of config/zellera.local.env and restart. (Said at most once a minute.)",
        )
    }

    private fun completeDiffie(
        session: PacketSession,
        clientKey: String,
    ) {
        val diffie = session.client.handshake
        when {
            diffie == null -> clients.close(session.client)
            session.client.streamEncrypted -> log.debug("Ignoring a repeated CompleteDiffieHandshake")
            else -> startRc4(session, diffie, clientKey)
        }
    }

    private fun startRc4(
        session: PacketSession,
        diffie: DiffieHellman,
        clientKey: String,
    ) {
        val sharedKey =
            try {
                diffie.sharedKey(clientKey)
            } catch (refused: GeneralSecurityException) {
                log.debug("Key exchange refused: {}", refused.message)
                clients.close(session.client)
                return
            }
        val channel = session.client.channel
        val answer = CompleteDiffieHandshakeOut(diffie.signedPublicKey, true)
        val version = ClientVersions.of(channel) ?: return
        val frame = Outbound(CompleteDiffieHandshakeOutCodec, answer).frame(channel.alloc(), version) ?: return
        session.client.streamEncrypted = true
        channel.eventLoop().execute {
            channel.writeAndFlush(frame)
            channel
                .pipeline()
                .addAfter(GameChannelPipeline.Stage.WEBSOCKET_BYTES.handlerName, "rc4", Rc4Stream(sharedKey))
        }
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE
        const val MISSING_KEY_WARNING_GAP_MILLIS = 60_000L
        val log: Logger = LoggerFactory.getLogger(HandshakePackets::class.java)
    }
}
