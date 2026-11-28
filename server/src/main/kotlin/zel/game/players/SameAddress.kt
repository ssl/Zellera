package zel.game.players

import java.net.InetAddress

internal object SameAddress {
    private const val V6_BYTES = 16
    private const val V6_PREFIX_BYTES = 8

    fun of(
        made: String?,
        used: String?,
    ): Boolean {
        val first = parse(made) ?: return true
        val second = parse(used) ?: return true
        if (first.size != second.size) return true
        val compared = if (first.size == V6_BYTES) V6_PREFIX_BYTES else first.size
        return (0 until compared).all { first[it] == second[it] }
    }

    private fun parse(address: String?): ByteArray? {
        val text = address?.trim()?.removePrefix("::ffff:")?.takeIf { it.isNotEmpty() } ?: return null
        if (!text.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == '.' || it == ':' }) return null
        return runCatching { InetAddress.getByName(text).address }.getOrNull()
    }
}
