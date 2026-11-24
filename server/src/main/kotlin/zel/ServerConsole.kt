package zel

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.console.ConsoleMenu
import java.io.BufferedReader
import java.io.IOException

/** The end of input ends reading, not the server. */
class ServerConsole(
    private val menu: ConsoleMenu,
    private val runs: () -> Boolean,
) {
    fun start() {
        Thread
            .ofPlatform()
            .name("console")
            .daemon(true)
            .start { readFrom(System.`in`.bufferedReader()) }
    }

    fun readFrom(reader: BufferedReader) {
        try {
            while (runs()) {
                val line = reader.readLine()?.trim() ?: return
                if (line.isEmpty()) continue
                if (!menu.dispatch(line)) log.info("unknown command '{}'; commands: {}", line, menu.help)
            }
        } catch (closed: IOException) {
            log.debug("console input closed", closed)
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ServerConsole::class.java)
    }
}
