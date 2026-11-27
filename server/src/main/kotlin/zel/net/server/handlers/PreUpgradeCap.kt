package zel.net.server.handlers

import java.util.concurrent.ConcurrentHashMap

class PreUpgradeCap(
    private val perPeer: Int,
) {
    private val held = ConcurrentHashMap<String, Int>()

    fun enter(peer: String): Boolean {
        if (perPeer <= 0) return true
        var taken = false
        held.compute(peer) { _, count ->
            val now = count ?: 0
            taken = now < perPeer
            if (taken) now + 1 else now
        }
        return taken
    }

    fun leave(peer: String) {
        if (perPeer <= 0) return
        held.computeIfPresent(peer) { _, count -> (count - 1).takeIf { it > 0 } }
    }
}
