package zel.net.crypto

import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.RSAPrivateKeySpec
import javax.crypto.Cipher

class RsaCrypto(
    modulusHex: String,
    privateExponentHex: String,
) {
    private val modulus = BigInteger(modulusHex, RADIX_HEX)
    private val key: PrivateKey =
        KeyFactory
            .getInstance("RSA")
            .generatePrivate(RSAPrivateKeySpec(modulus, BigInteger(privateExponentHex, RADIX_HEX)))
    private val blockBytes = (modulus.bitLength() + Byte.SIZE_BITS - 1) / Byte.SIZE_BITS

    /** The payload of a block `00 02 <at least 8 random bytes> 00 payload`. */
    fun decrypt(block: ByteArray): ByteArray {
        refuseUnless(block.size == blockBytes) { "block of ${block.size} bytes, key takes $blockBytes" }
        val opened = applyKey(block)
        refuseUnless(opened[0] == ZERO && opened[1] == TYPE_ENCRYPT) { "block not sealed for this key" }
        val separator =
            (HEADER_BYTES until opened.size).firstOrNull { opened[it] == ZERO }
                ?: throw GeneralSecurityException("block has no payload")
        refuseUnless(separator - HEADER_BYTES >= MIN_PAD_BYTES) { "block padding shorter than $MIN_PAD_BYTES bytes" }
        return opened.copyOfRange(separator + 1, opened.size)
    }

    /** The block `00 01 FF.. 00 payload` under the private exponent. */
    fun sign(payload: ByteArray): ByteArray {
        val padBytes = blockBytes - HEADER_BYTES - 1 - payload.size
        refuseUnless(padBytes >= MIN_PAD_BYTES) { "${payload.size} bytes do not fit a $blockBytes byte block" }
        val block = ByteArray(blockBytes)
        block[1] = TYPE_SIGN
        block.fill(PAD, HEADER_BYTES, HEADER_BYTES + padBytes)
        payload.copyInto(block, HEADER_BYTES + padBytes + 1)
        return applyKey(block)
    }

    /** `block ^ d mod n`; a block not below the modulus is refused. */
    private fun applyKey(block: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val digits = cipher.doFinal(block)
        return ByteArray(blockBytes - digits.size) + digits
    }

    private companion object {
        const val RADIX_HEX = 16
        const val HEADER_BYTES = 2
        const val MIN_PAD_BYTES = 8
        const val ZERO: Byte = 0
        const val TYPE_SIGN: Byte = 1
        const val TYPE_ENCRYPT: Byte = 2
        const val PAD: Byte = -1

        private inline fun refuseUnless(
            holds: Boolean,
            problem: () -> String,
        ) {
            if (!holds) throw GeneralSecurityException(problem())
        }
    }
}
