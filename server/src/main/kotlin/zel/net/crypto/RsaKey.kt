package zel.net.crypto

import zel.core.Configuration
import java.math.BigInteger

/** The key of the optional key exchange; a client that asks for it must carry the same modulus. */
class RsaKey(
    val modulusHex: String,
    val privateExponentHex: String,
) {
    companion object {
        private const val HEX = 16
        private val PUBLIC_EXPONENT = BigInteger.valueOf(0x10001)
        private val PROBE = BigInteger.valueOf(0x2A5E)

        /** Null when `login.key_exchange.is_on` is off; a missing or broken key stops the start. */
        fun fromSettings(settings: Configuration): RsaKey? {
            if (!settings.getBoolean("login.key_exchange.is_on", false)) return null
            val n = settings.hex("login.key_exchange.modulus")
            val d = settings.hex("login.key_exchange.private_exponent")
            check(n != null && d != null && n > PROBE && PROBE.modPow(PUBLIC_EXPONENT, n).modPow(d, n) == PROBE) {
                "login.key_exchange.modulus and login.key_exchange.private_exponent are not one RSA key " +
                    "with public exponent 10001"
            }
            return RsaKey(n.toString(HEX).uppercase(), d.toString(HEX).uppercase())
        }

        private fun Configuration.hex(key: String): BigInteger? = getValue(key, "").trim().toBigIntegerOrNull(HEX)
    }
}
