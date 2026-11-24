package zel.room.entry

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** A door password as the database holds it: `k1:<rounds>:<salt>:<key>`, salt and key in URL-safe base64. */
object DoorSecret {
    const val MAX_LENGTH = 64

    private const val VERSION = "k1"
    private const val ROUNDS = 100_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val PARTS = 4
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun seal(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val key = derive(password, salt, ROUNDS)
        return listOf(VERSION, ROUNDS, encoder.encodeToString(salt), encoder.encodeToString(key)).joinToString(":")
    }

    fun opens(
        sealed: String,
        given: String,
    ): Boolean {
        if (given.isEmpty() || given.length > MAX_LENGTH) return false
        val parts = sealed.split(':')
        val rounds = parts.getOrNull(1)?.toIntOrNull()
        if (parts.size != PARTS || parts[0] != VERSION || rounds == null || rounds !in 1..ROUNDS) return false
        val salt = decode(parts[2]) ?: return false
        val key = decode(parts[3]) ?: return false
        return MessageDigest.isEqual(key, derive(given, salt, rounds))
    }

    private fun derive(
        password: String,
        salt: ByteArray,
        rounds: Int,
    ): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, rounds, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun decode(text: String): ByteArray? =
        try {
            decoder.decode(text)
        } catch (_: IllegalArgumentException) {
            null
        }
}
