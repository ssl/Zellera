package zel.net.server

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object PacketDump {
    private val log = LoggerFactory.getLogger(PacketDump::class.java)
    private val file: Path? =
        System.getProperty("zel.packet.dump", "").takeIf { it.isNotEmpty() }?.let {
            log.warn("packet dump enabled: {} (dev only)", it)
            Path.of(it)
        }

    val enabled: Boolean get() = file != null

    @Synchronized
    fun line(text: String) {
        val path = file ?: return
        try {
            Files.writeString(
                path,
                text + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
        } catch (e: IOException) {
            log.warn("packet dump write failed: {}", e.toString())
        }
    }
}
