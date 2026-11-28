package zel.net.crypto

import zel.util.Stir
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.HexFormat

class DiffieHellman(
    private val rsa: RsaCrypto,
    random: SecureRandom = SecureRandom(),
) {
    private val prime: BigInteger = BigInteger.probablePrime(PRIME_BITS, random)
    private val generator: BigInteger = BigInteger.probablePrime(PRIME_BITS - 1, random)
    private val secret: BigInteger
    private val publicValue: BigInteger

    init {
        var chosen: BigInteger
        var value: BigInteger
        do {
            chosen = BigInteger(SECRET_BITS, random).setBit(0)
            value = generator.modPow(chosen, prime)
        } while (!Stir.even(prime, value))
        secret = chosen
        publicValue = value
    }

    val signedPrime: String by lazy { signed(prime) }

    val signedGenerator: String by lazy { signed(generator) }

    val signedPublicKey: String by lazy { signed(publicValue) }

    @Throws(GeneralSecurityException::class)
    fun sharedKey(encryptedClientKeyHex: String): ByteArray {
        val clientValue = unseal(encryptedClientKeyHex)
        if (clientValue <= BigInteger.ONE || clientValue >= prime - BigInteger.ONE) {
            throw GeneralSecurityException("client public value outside the group")
        }
        return clientValue
            .modPow(secret, prime)
            .toByteArray()
            .dropWhile { it == ZERO }
            .toByteArray()
    }

    private fun unseal(hex: String): BigInteger {
        val sealed =
            runCatching { HEX.parseHex(hex) }
                .getOrElse { throw GeneralSecurityException("client key is no hex", it) }
        val digits = String(rsa.decrypt(sealed), Charsets.US_ASCII)
        return digits.toBigIntegerOrNull() ?: throw GeneralSecurityException("client key is no number")
    }

    private fun signed(value: BigInteger): String =
        HEX.formatHex(rsa.sign(value.toString().toByteArray(Charsets.US_ASCII)))

    private companion object {
        const val PRIME_BITS = 128
        const val SECRET_BITS = 128
        const val ZERO: Byte = 0

        /** Lowercase out, as the client expects; either case is read. */
        val HEX: HexFormat = HexFormat.of()
    }
}
