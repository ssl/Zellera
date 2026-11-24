package zel.net.server.proxy

/** A run of addresses: one address, or `address/bits`. */
class AddressBlock private constructor(
    private val text: String,
    private val network: ByteArray,
    private val bits: Int,
) {
    fun contains(address: ByteArray?): Boolean {
        if (address == null || address.size != network.size) return false
        val whole = bits / BITS_PER_BYTE
        val rest = bits % BITS_PER_BYTE
        if ((0 until whole).any { address[it] != network[it] }) return false
        return rest == 0 || (address[whole].toInt() xor network[whole].toInt()) and tailMask(rest) == 0
    }

    override fun toString(): String = text

    companion object {
        private const val BITS_PER_BYTE = 8
        private const val FULL_BYTE = 0xFF

        /** Null unless the address is a literal and the bits fit it. */
        fun parse(text: String?): AddressBlock? {
            val entry = text?.trim().orEmpty()
            val address = IpLiteral.bytes(entry.substringBefore('/')) ?: return null
            val longest = address.size * BITS_PER_BYTE
            val bits = if ('/' in entry) entry.substringAfter('/').toIntOrNull() ?: return null else longest
            return if (bits in 0..longest) AddressBlock(entry, address, bits) else null
        }

        private fun tailMask(bits: Int): Int = (FULL_BYTE shl (BITS_PER_BYTE - bits)) and FULL_BYTE
    }
}
