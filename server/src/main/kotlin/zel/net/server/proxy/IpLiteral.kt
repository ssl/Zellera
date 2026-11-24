package zel.net.server.proxy

import java.net.InetAddress

object IpLiteral {
    private const val IPV4_DOTS = 3
    private const val IPV6_BYTES = 16
    private const val SUBSCRIBER_BYTES = 8
    private const val LOOPBACK_V4: Byte = 127

    /** Null for anything but a full IP literal; a name is never looked up. */
    fun bytes(text: String?): ByteArray? {
        if (text.isNullOrEmpty() || (':' !in text && text.count { it == '.' } != IPV4_DOTS)) return null
        return try {
            InetAddress.ofLiteral(text).address
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun isLoopback(text: String?): Boolean {
        val address = bytes(text) ?: return false
        return if (address.size == IPV6_BYTES) {
            address.last() == 1.toByte() && address.dropLast(1).all { it == 0.toByte() }
        } else {
            address[0] == LOOPBACK_V4
        }
    }

    /**
     * What one subscriber line holds: an IPv4 address, or the /64 around an IPv6 address, which a provider hands
     * out whole. Text that is no address stands for itself.
     */
    fun subscriber(text: String): String {
        val address = bytes(text) ?: return text
        if (address.size != IPV6_BYTES) return address.joinToString(".") { (it.toInt() and BYTE).toString() }
        val prefix = address.take(SUBSCRIBER_BYTES).joinToString("") { hex(it) }
        return "$prefix/64"
    }

    private fun hex(byte: Byte): String = (byte.toInt() and BYTE).toString(HEX).padStart(2, '0')

    private const val BYTE = 0xFF
    private const val HEX = 16
}
