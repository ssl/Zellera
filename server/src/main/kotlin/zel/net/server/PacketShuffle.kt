package zel.net.server

import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PacketShuffle private constructor(
    private val fromClient: IntArray,
    private val toClient: IntArray,
) {
    fun opened(wire: Int): Int = open(fromClient, wire and MASK)

    fun sealed(id: Int): Int = seal(toClient, id and MASK)

    companion object {
        private const val MASK = 0xFFFF
        private const val BYTE = 0xFF
        private const val ROUNDS = 4
        private const val SIDE = 256
        private const val HALF_BITS = 8
        private const val HMAC = "HmacSHA256"
        private const val LABEL = "zel.shuffle"

        fun of(
            sharedKey: ByteArray,
            release: String,
            modulusHex: String,
        ): PacketShuffle {
            val shared = BigInteger(1, sharedKey).toString(16)
            val mac = Mac.getInstance(HMAC).apply { init(SecretKeySpec(shared.toByteArray(Charsets.UTF_8), HMAC)) }
            val seed = mac.doFinal("$LABEL\n$release\n${modulusHex.lowercase()}".toByteArray(Charsets.UTF_8))
            return PacketShuffle(table(seed, "c2s"), table(seed, "s2c"))
        }

        private fun table(
            seed: ByteArray,
            direction: String,
        ): IntArray {
            val out = IntArray(ROUNDS * SIDE)
            var filled = 0
            var block = 0
            while (filled < out.size) {
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(seed)
                digest.update(direction.toByteArray(Charsets.UTF_8))
                digest.update(block.toByte())
                for (b in digest.digest()) if (filled < out.size) out[filled++] = b.toInt() and BYTE
                block++
            }
            return out
        }

        private fun seal(
            table: IntArray,
            id: Int,
        ): Int {
            var left = id ushr HALF_BITS
            var right = id and BYTE
            for (round in 0 until ROUNDS) {
                val next = left xor table[round * SIDE + right]
                left = right
                right = next
            }
            return (left shl HALF_BITS) or right
        }

        private fun open(
            table: IntArray,
            wire: Int,
        ): Int {
            var left = wire ushr HALF_BITS
            var right = wire and BYTE
            for (round in ROUNDS - 1 downTo 0) {
                val previous = right xor table[round * SIDE + left]
                right = left
                left = previous
            }
            return (left shl HALF_BITS) or right
        }
    }
}
