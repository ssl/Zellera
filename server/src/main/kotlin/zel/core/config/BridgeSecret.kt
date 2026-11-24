package zel.core.config

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.HexFormat
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readText

/** The secret the server's wired bridge shares with the web, made once by whichever of the two starts first. */
internal object BridgeSecret {
    private const val FILE = "wired-bridge-secret"
    private const val BYTES = 32
    private val HEX = Regex("[0-9a-f]{64}")
    private val OWNER_ONLY = PosixFilePermissions.fromString("rw-------")
    private val random = SecureRandom()

    fun of(secrets: Path): String {
        val file = secrets.resolve(FILE)
        if (!file.exists()) create(file)
        val text = if (file.fileSize() == 0L) "" else file.readText().filterNot(Char::isWhitespace)
        check(HEX.matches(text)) { "$file must hold 64 lower-case hex characters" }
        return text
    }

    /** Written beside the file and linked into place, so a reader never sees it half written. */
    private fun create(file: Path) {
        Files.createDirectories(file.parent)
        val draft =
            Files.createTempFile(
                file.parent,
                ".$FILE",
                ".new",
                PosixFilePermissions.asFileAttribute(OWNER_ONLY),
            )
        try {
            Files.writeString(draft, HexFormat.of().formatHex(ByteArray(BYTES).also(random::nextBytes)))
            Files.createLink(file, draft)
        } catch (_: FileAlreadyExistsException) {
            // the web made it at the same moment
        } finally {
            Files.deleteIfExists(draft)
        }
    }
}
