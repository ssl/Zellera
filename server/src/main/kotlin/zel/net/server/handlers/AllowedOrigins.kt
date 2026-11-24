package zel.net.server.handlers

import java.net.URI
import java.net.URISyntaxException

/**
 * Host names, comma separated, compared without case: `name` is that host, `*.name` every host under it and `*`
 * any host. An empty list admits no one.
 */
class AllowedOrigins(
    setting: String,
) {
    private val entries =
        setting
            .lowercase()
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)
    private val any = ANY in entries
    private val hosts = entries.filterNot { it.startsWith(UNDER) }.toSet()
    private val parents = entries.filter { it.startsWith(UNDER) }.map { it.removePrefix(ANY) }

    fun admits(origin: String?): Boolean {
        val host = hostOf(origin) ?: return false
        return any || host in hosts || parents.any(host::endsWith)
    }

    private fun hostOf(origin: String?): String? =
        try {
            origin?.let { URI(it).host }?.lowercase()
        } catch (_: URISyntaxException) {
            null
        }

    private companion object {
        const val ANY = "*"
        const val UNDER = "*."
    }
}
