package zel.ext

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.protocol.generated.IncomingCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.generated.PacketReader

object ExtensionBus {
    private val log: Logger = LoggerFactory.getLogger(ExtensionBus::class.java)

    private class Hooked<T>(
        val owner: String,
        val codec: IncomingCodec<T>?,
        val hook: PacketHook<T>,
    )

    @Volatile
    private var incoming: Map<String, List<Hooked<*>>> = emptyMap()

    @Volatile
    private var outgoing: Map<String, List<Hooked<*>>> = emptyMap()

    @Volatile
    private var events: List<Pair<String, (ServerEvent) -> Unit>> = emptyList()

    fun hooksOutgoing(message: String): Boolean = outgoing.containsKey(message)

    /** Null when an extension took it. */
    fun <T> incoming(
        playerId: Int?,
        codec: IncomingCodec<T>,
        message: T,
    ): T? = run(incoming[codec.name], playerId, message)

    fun unrouted(
        playerId: Int?,
        message: String?,
        body: ByteArray,
    ): Boolean {
        val hooks = incoming[message] ?: return false
        hooks.forEach { hooked -> guarded(hooked.owner) { readAndRun(hooked, playerId, body) } }
        return true
    }

    /** Null when an extension dropped it. */
    fun <T> outgoing(
        playerId: Int?,
        codec: OutgoingCodec<T>,
        message: T,
    ): T? = run(outgoing[codec.name], playerId, message)

    fun event(event: ServerEvent) {
        events.forEach { (owner, listener) -> guarded(owner) { listener(event) } }
    }

    @Synchronized
    internal fun <T> hookIncoming(
        owner: String,
        codec: IncomingCodec<T>,
        hook: PacketHook<T>,
    ) {
        incoming = incoming + (codec.name to incoming[codec.name].orEmpty() + Hooked(owner, codec, hook))
    }

    @Synchronized
    internal fun <T> hookOutgoing(
        owner: String,
        codec: OutgoingCodec<T>,
        hook: PacketHook<T>,
    ) {
        outgoing = outgoing + (codec.name to outgoing[codec.name].orEmpty() + Hooked(owner, null, hook))
    }

    @Synchronized
    internal fun listen(
        owner: String,
        listener: (ServerEvent) -> Unit,
    ) {
        events = events + (owner to listener)
    }

    @Synchronized
    internal fun drop(owner: String) {
        incoming = incoming.mapValues { (_, l) -> l.filter { it.owner != owner } }.filterValues { it.isNotEmpty() }
        outgoing = outgoing.mapValues { (_, l) -> l.filter { it.owner != owner } }.filterValues { it.isNotEmpty() }
        events = events.filter { it.first != owner }
    }

    @Suppress("UNCHECKED_CAST") // hooks are filed under their own codec's name
    private fun <T> run(
        hooks: List<Hooked<*>>?,
        playerId: Int?,
        message: T,
    ): T? {
        if (hooks == null) return message
        var current: T = message
        for (hooked in hooks) {
            val hook = hooked.hook as PacketHook<T>
            val next = guardedOr(hooked.owner, current) { hook.on(playerId, current) } ?: return null
            current = next
        }
        return current
    }

    private fun <T> readAndRun(
        hooked: Hooked<T>,
        playerId: Int?,
        body: ByteArray,
    ) {
        val codec = hooked.codec ?: return
        hooked.hook.on(playerId, codec.read(PacketReader(body)))
    }

    private fun guarded(
        owner: String,
        block: () -> Unit,
    ) {
        guardedOr(owner, Unit) { block() }
    }

    /** A hook that throws passes the message on unchanged. */
    private fun <T> guardedOr(
        owner: String,
        fallback: T,
        block: () -> T?,
    ): T? =
        try {
            block()
        } catch (
            @Suppress("TooGenericExceptionCaught") e: RuntimeException,
        ) {
            log.error("game extension {}: a packet or event hook failed", owner, e)
            fallback
        }
}

/** [playerId] is null before login. */
fun interface PacketHook<T> {
    fun on(
        playerId: Int?,
        message: T,
    ): T?
}
