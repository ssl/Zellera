package zel

import zel.core.config.KeyFile
import java.io.PrintStream
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.spec.RSAKeyGenParameterSpec

/** `keys ensure`: this installation's own key of the key exchange, made once and never written again. */
object KeysCommand {
    private const val NAME = "keys"
    private const val USAGE = "usage: zellera keys ensure"
    private const val BAD_ARGUMENTS = 2
    private const val FAILED = 1
    private const val BITS = 2048
    private const val HEX = 16
    private val OWNER_FOLDER = PosixFilePermissions.fromString("rwx------")
    private val OWNER_FILE = PosixFilePermissions.fromString("rw-------")

    fun handles(args: Array<String>): Boolean = args.firstOrNull() == NAME

    fun run(
        args: Array<String>,
        environment: Map<String, String>,
        err: PrintStream,
    ): Int {
        if (args.drop(1) != listOf("ensure")) {
            err.println(USAGE)
            return BAD_ARGUMENTS
        }
        if (KeyFile.isOff(environment)) return 0
        val file = KeyFile.path(environment)
        if (file == null) {
            err.println("ZEL_SECRETS_DIR is not set, so the key has nowhere to go")
            return FAILED
        }
        ensure(file)
        return 0
    }

    /** True when the key was made now. The file appears under its name whole, through a temporary one. */
    fun ensure(file: Path): Boolean {
        if (Files.exists(file)) return false
        val folder = file.toAbsolutePath().parent
        if (!Files.isDirectory(folder)) {
            Files.createDirectories(folder, PosixFilePermissions.asFileAttribute(OWNER_FOLDER))
        }
        Files.setPosixFilePermissions(folder, OWNER_FOLDER)
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(RSAKeyGenParameterSpec(BITS, RSAKeyGenParameterSpec.F4))
        val key = generator.generateKeyPair().private as RSAPrivateKey
        val part = Files.createTempFile(folder, KeyFile.NAME, ".new", PosixFilePermissions.asFileAttribute(OWNER_FILE))
        val lines = listOf("${KeyFile.MODULUS}=${hex(key.modulus)}", "${KeyFile.EXPONENT}=${hex(key.privateExponent)}")
        Files.writeString(part, lines.joinToString("\n", postfix = "\n"))
        Files.move(part, file, StandardCopyOption.ATOMIC_MOVE)
        return true
    }

    private fun hex(number: BigInteger): String = number.toString(HEX).uppercase()
}
