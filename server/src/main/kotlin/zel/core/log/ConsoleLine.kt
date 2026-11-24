package zel.core.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.LayoutBase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One console line: time, the class that speaks, the message. Most lines are information and carry no level; a line
 * that is anything else says so in a word before its message, which reads the same in a file without colour.
 */
class ConsoleLine : LayoutBase<ILoggingEvent>() {
    /** 0 keeps every line. */
    var traceLines: Int = 0

    override fun doLayout(event: ILoggingEvent): String {
        val head = line(event.timeStamp, event.level, event.loggerName, event.formattedMessage, ConsoleColour.on)
        val trace =
            event.throwableProxy
                ?.let(ThrowableProxyUtil::asString)
                ?.let(::cut)
                .orEmpty()
        return head + CoreConstants.LINE_SEPARATOR + trace
    }

    private fun cut(trace: String): String {
        val lines = trace.lines().filter { it.isNotEmpty() }
        val kept = if (traceLines > 0) lines.take(traceLines) else lines
        return kept.joinToString("") { it + CoreConstants.LINE_SEPARATOR }
    }

    /** How loud a line is: the word before its message and what colour does to it. */
    private enum class Voice(
        val word: String,
        private val paint: String,
        private val paintsMessage: Boolean,
    ) {
        PROBLEM("error: ", "\u001B[1;31m", true),
        CAUTION("warn: ", "\u001B[33m", false),
        NORMAL("", "", false),
        DETAIL("debug: ", FAINT, true),
        ;

        fun speak(
            message: String,
            coloured: Boolean,
        ): String =
            when {
                !coloured || paint.isEmpty() -> word + message
                paintsMessage -> paint + word + message + RESET
                else -> paint + word + RESET + message
            }

        companion object {
            fun of(level: Level): Voice =
                when {
                    level.isGreaterOrEqual(Level.ERROR) -> PROBLEM
                    level.isGreaterOrEqual(Level.WARN) -> CAUTION
                    level.isGreaterOrEqual(Level.INFO) -> NORMAL
                    else -> DETAIL
                }
        }
    }

    companion object {
        const val SOURCE_WIDTH = 18
        private const val RESET = "\u001B[0m"
        private const val FAINT = "\u001B[2m"
        private val TIME_OF_DAY = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.ROOT)

        fun line(
            timeMillis: Long,
            level: Level,
            logger: String,
            message: String,
            coloured: Boolean,
        ): String {
            val time = TIME_OF_DAY.format(Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()))
            val clock = if (coloured) FAINT + time + RESET else time
            return "$clock ${source(logger)} ${Voice.of(level).speak(message, coloured)}"
        }

        /** The class without its package, in a column; a longer name ends in `~`. */
        fun source(logger: String): String {
            val simple = logger.substringAfterLast('.')
            val fitted = if (simple.length > SOURCE_WIDTH) simple.take(SOURCE_WIDTH - 1) + "~" else simple
            return fitted.padEnd(SOURCE_WIDTH)
        }
    }
}
