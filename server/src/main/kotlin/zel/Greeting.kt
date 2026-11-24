package zel

import zel.core.BuildInfo
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Greeting {
    private val BUILT_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

    fun banner(
        build: BuildStamp,
        runtime: String,
        styled: Boolean,
    ): String {
        val pen = if (styled) Pen.ANSI else Pen.PLAIN
        val label = if (build.hash.isEmpty()) "" else " (build ${build.hash})"
        val heading = "${BuildInfo.PRODUCT} ${build.version}$label"
        return pen.strong(heading) + "\non " + runtime + "\n" + pen.faint("-".repeat(heading.length)) + "\n"
    }

    fun buildLine(
        build: BuildStamp,
        gameZone: ZoneId,
    ): String {
        val moment =
            if (build.builtAtMillis > 0) {
                BUILT_AT.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(build.builtAtMillis), gameZone))
            } else {
                "unknown"
            }
        val label = if (build.hash.isEmpty()) "" else "build ${build.hash}, "
        return "${label}built $moment [${gameZone.id}]"
    }

    private enum class Pen(
        private val bold: String,
        private val dim: String,
        private val end: String,
    ) {
        PLAIN("", "", ""),
        ANSI("\u001B[1m", "\u001B[2m", "\u001B[0m"),
        ;

        fun strong(text: String) = bold + text + end

        fun faint(text: String) = dim + text + end
    }
}
