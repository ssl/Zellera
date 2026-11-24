package zel.core.config

import java.nio.file.Files
import java.nio.file.Path

/** This installation's key of the key exchange: two lines of upper-case hex in the secrets folder. */
object KeyFile {
    const val NAME = "rsa.env"
    const val MODULUS = "ZEL_RSA_N"
    const val EXPONENT = "ZEL_RSA_D"
    private val HEX = Regex("[0-9A-Fa-f]+")

    class Key(
        val modulus: String,
        val exponent: String,
    )

    /** `ZEL_KEY_EXCHANGE=0` runs without the key exchange, and no key is made or read. */
    fun isOff(environment: Map<String, String>): Boolean = environment["ZEL_KEY_EXCHANGE"] == "0"

    /** `ZEL_KEY_FILE` names a key of the operator's own; else the one in `ZEL_SECRETS_DIR`. */
    fun path(environment: Map<String, String>): Path? =
        environment["ZEL_KEY_FILE"]?.takeIf(String::isNotBlank)?.let(Path::of)
            ?: environment["ZEL_SECRETS_DIR"]?.takeIf(String::isNotBlank)?.let { Path.of(it).resolve(NAME) }

    /** Null when there is no file; a file that is not a whole key is an error. */
    fun read(file: Path): Key? {
        if (!Files.exists(file)) return null
        val values =
            Files
                .readAllLines(file)
                .filter { '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
        val modulus = values[MODULUS]
        val exponent = values[EXPONENT]
        check(modulus != null && exponent != null && HEX.matches(modulus) && HEX.matches(exponent)) {
            "$file must hold the lines $MODULUS= and $EXPONENT= in hex; remove it and start again for a new key"
        }
        return Key(modulus, exponent)
    }
}
