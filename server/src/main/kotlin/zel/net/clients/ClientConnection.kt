package zel.net.clients

import zel.ext.ExtensionBus
import zel.game.LatencyTracker
import zel.game.players.DeviceReading
import zel.game.players.Player
import zel.net.crypto.DiffieHellman
import zel.protocol.Outbound
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.UnseenItemsOut
import zel.protocol.outgoing.OutgoingMessage
import zel.protocol.typed.ClientVersions
import zel.protocol.typed.PacketWarnings
import zel.util.monotonicMillis
import io.netty.channel.Channel
import io.netty.util.AttributeKey
import io.netty.util.concurrent.EventExecutor
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ClientConnection(
    val channel: Channel,
    private val newHandshake: () -> DiffieHellman? = { null },
) {
    @Volatile
    var player: Player? = null

    @Volatile
    var machineId: String = ""

    /** The reading the client sent with its machine id; null when it sent none or a malformed one. */
    @Volatile
    var device: DeviceReading? = null

    @Volatile
    var releaseName: String = ""

    @Volatile
    var streamEncrypted: Boolean = false

    /** Null before the first InitDiffie or with encryption off. */
    @Volatile
    var handshake: DiffieHellman? = null
        private set

    /** A logout queued on this lane runs after the packets read before the close. */
    @Volatile
    private var packetLane: Executor = Executor(Runnable::run)

    val isClosed: Boolean get() = closed.get()

    val warnings = PacketWarnings({ player?.profile?.name }, { channel.remoteAddress()?.toString() ?: "closed" })

    val latency = LatencyTracker()

    private val cooldowns = Cooldowns()
    private val messengerStarted = AtomicBoolean()
    private val closed = AtomicBoolean()
    private val loggedOut = CountDownLatch(1)

    /** Null with encryption off or when already started, since each start costs RSA work. */
    @Synchronized
    fun startHandshake(): DiffieHellman? {
        if (handshake != null) return null
        return newHandshake()?.also { handshake = it }
    }

    fun runLogoutOn(lane: Executor) {
        packetLane = lane
    }

    fun startMessengerOnce(): Boolean = !messengerStarted.getAndSet(true)

    /** A refused message does not move the gap. */
    fun allow(
        group: String,
        gapMillis: Long,
        now: Long = monotonicMillis(),
    ): Boolean = cooldowns.pass(group, gapMillis, now)

    fun send(message: OutgoingMessage<*>) = send(message.compose())

    fun send(message: Outbound<*>?) = write(listOfNotNull(message))

    fun <T> send(
        codec: OutgoingCodec<T>,
        message: T,
    ) = send(Outbound(codec, message))

    fun sendAll(messages: List<Outbound<*>?>) = write(messages.filterNotNull())

    /** The logout runs behind any of this connection's packets still running. */
    fun close() {
        if (closed.getAndSet(true)) return
        channel.close()
        try {
            packetLane.execute(::logOut)
        } catch (
            @Suppress("SwallowedException") stopping: RejectedExecutionException, // the lane is stopping
        ) {
            logOut()
        }
    }

    /** On its own lane it logs out inline, since a queued logout would wait on the caller. */
    fun closeAndAwaitLogout(timeoutMillis: Long): Boolean {
        val lane = packetLane
        if (lane is EventExecutor && lane.inEventLoop()) {
            closed.set(true)
            channel.close()
            logOut()
            return true
        }
        close()
        return loggedOut.await(timeoutMillis, TimeUnit.MILLISECONDS)
    }

    @Suppress("TooGenericExceptionCaught") // channel already closed; a failed logout is logged
    private fun logOut() {
        try {
            val player = player?.takeIf { it.client === this && it.isOnline } ?: return
            try {
                player.disconnect()
            } catch (failure: Exception) {
                log.error("logout of player {} failed", player.profile.id, failure)
            }
        } finally {
            loggedOut.countDown()
        }
    }

    /** Nothing is sent to a client whose hello has not been accepted. */
    private fun write(sent: List<Outbound<*>>) {
        val messages = sent.mapNotNull { hooked(it) }
        if (messages.isEmpty()) return
        keepUnseenMarks(messages)
        if (!channel.isActive) return
        val version = ClientVersions.of(channel)
        if (version == null) {
            log.warn("{} not sent: the client has not said hello yet", messages.joinToString { it.codec.name })
            return
        }
        for (message in messages) {
            val frame = message.frame(channel.alloc(), version) ?: continue
            channel.write(frame, channel.voidPromise())
        }
        channel.flush()
    }

    private fun <T> hooked(outbound: Outbound<T>): Outbound<T>? {
        if (!ExtensionBus.hooksOutgoing(outbound.codec.name)) return outbound
        val message = ExtensionBus.outgoing(player?.profile?.id, outbound.codec, outbound.message) ?: return null
        return if (message === outbound.message) outbound else Outbound(outbound.codec, message)
    }

    /** What the client is told is new stays new for its next session too. */
    private fun keepUnseenMarks(messages: List<Outbound<*>>) {
        val unseen = player?.inventory?.unseen ?: return
        messages
            .mapNotNull { it.message as? UnseenItemsOut }
            .forEach { new -> unseen.markAll(new.categories.associate { it.category to it.itemIds }) }
    }

    private class Cooldowns {
        private val openAt = ConcurrentHashMap<String, Long>()

        fun pass(
            group: String,
            gapMillis: Long,
            now: Long,
        ): Boolean {
            var passed = false
            openAt.compute(group) { _, deadline ->
                if (deadline == null || now >= deadline) {
                    passed = true
                    now + gapMillis
                } else {
                    deadline
                }
            }
            return passed
        }
    }

    companion object {
        val KEY: AttributeKey<ClientConnection> = AttributeKey.valueOf("connection")

        private val log = LoggerFactory.getLogger(ClientConnection::class.java)
    }
}
