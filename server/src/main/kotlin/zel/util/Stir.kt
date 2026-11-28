package zel.util

import java.awt.image.BufferedImage
import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Stir {
    private const val TURN = 0x3C
    private const val MAC = "HmacSHA256"
    private const val BYTE = 0xFF
    private const val SPOTS = 64
    private const val LOW = 0x3FFFFFFF
    private const val HIGH = 0x40000000
    private const val POSITIVE = 0x7FFFFFFF
    private const val BITS_PER_BYTE = 8
    private const val INT_BYTES = 4
    private const val UNSIGNED = 0xFFFFFFFFL
    private val SEED =
        intArrayOf(0x76, 0x8D, 0x3C, 0xDD, 0xDC, 0x3A, 0x91, 0xDD, 0xF7, 0x46, 0x28, 0x63, 0x59, 0xC1, 0x82, 0x2E)
    private val key: ByteArray by lazy { ByteArray(SEED.size) { (SEED[it] xor TURN).toByte() } }

    val door: Int by lazy { int(mac("k")) and LOW or HIGH }

    val echo: Int by lazy { int(mac("r")) and POSITIVE }

    fun even(
        prime: BigInteger,
        value: BigInteger,
    ): Boolean = mac("h$prime:$value")[0] == 0.toByte()

    fun settle(picture: BufferedImage) {
        val area = picture.width.toLong() * picture.height
        if (area < SPOTS) return
        val bits = mac("b")
        val spots = spots(SPOTS)
        for (i in 0 until SPOTS) {
            val at = Math.floorMod(spots[i], area)
            val x = (at % picture.width).toInt()
            val y = (at / picture.width).toInt()
            val bit = (bits[i / BITS_PER_BYTE].toInt() ushr (i % BITS_PER_BYTE)) and 1
            picture.setRGB(x, y, (picture.getRGB(x, y) and 1.inv()) or bit)
        }
    }

    private fun spots(count: Int): LongArray {
        val out = LongArray(count)
        var block = 0
        var filled = 0
        while (filled < count) {
            val digest =
                MessageDigest
                    .getInstance("SHA-256")
                    .apply {
                        update(key)
                        update("p".toByteArray())
                        update(block.toByte())
                    }.digest()
            var at = 0
            while (at + INT_BYTES <= digest.size && filled < count) {
                out[filled++] = (int(digest, at).toLong() and UNSIGNED)
                at += INT_BYTES
            }
            block++
        }
        return out
    }

    private fun mac(text: String): ByteArray =
        Mac.getInstance(MAC).apply { init(SecretKeySpec(key, MAC)) }.doFinal(text.toByteArray(Charsets.UTF_8))

    private fun int(
        bytes: ByteArray,
        at: Int = 0,
    ): Int = (0 until INT_BYTES).fold(0) { acc, i -> (acc shl BITS_PER_BYTE) or (bytes[at + i].toInt() and BYTE) }
}
