package zel.protocol.typed

import zel.net.clients.ClientConnection
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class AllowedLookups(
    private val byName: Map<String, AllowedLookup>,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val answers = ConcurrentHashMap<Pair<String, Any>, Answer>()

    val names: Set<String> get() = byName.keys

    fun allows(
        name: String,
        client: ClientConnection,
        value: Any,
    ): Boolean? {
        val lookup = byName[name] ?: return true
        return guarded(name) { lookup.now(client, value) ?: if (lookup.later == null) true else null }
    }

    fun laterAllows(
        name: String,
        value: Any,
    ): Boolean {
        val later = byName[name]?.later ?: return true
        val key = name to value
        val at = now()
        val known = answers[key]
        if (known != null && at - known.at < CACHE_MS) return known.allowed
        val allowed = guarded(name) { later(value) } ?: true
        if (answers.size >= CACHE_SIZE) answers.clear()
        answers[key] = Answer(allowed, at)
        return allowed
    }

    @Suppress("TooGenericExceptionCaught") // must not reach the packet's handler
    private fun guarded(
        name: String,
        lookup: () -> Boolean?,
    ): Boolean? =
        try {
            lookup()
        } catch (e: RuntimeException) {
            LOG.warn("allowed value lookup {} failed", name, e)
            true
        }

    private class Answer(
        val allowed: Boolean,
        val at: Long,
    )

    companion object {
        private val LOG = LoggerFactory.getLogger(AllowedLookups::class.java)
        private const val CACHE_SIZE = 4096
        private const val CACHE_MS = 60_000L
        val NONE = AllowedLookups(emptyMap())
    }
}
