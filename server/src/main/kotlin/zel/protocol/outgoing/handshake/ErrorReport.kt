package zel.protocol.outgoing.handshake

import zel.protocol.generated.ErrorReportOut
import zel.protocol.generated.ErrorReportOutCodec
import zel.protocol.outgoing.OutgoingMessage
import java.time.Clock
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class ErrorReport(
    private val requestId: Int,
    private val code: Int,
    private val time: String = serverTime(),
) : OutgoingMessage<ErrorReportOut>(ErrorReportOutCodec) {
    override fun message() = ErrorReportOut(requestId, code, time)

    companion object {
        private const val NO_REQUEST = 0
        private const val REQUEST_FAILED = 1
        private val CLOCK_FORMAT = DateTimeFormatter.ofPattern("M/d/yy, h:mm a", Locale.US).withZone(ZoneOffset.UTC)

        fun of(code: Int) = ErrorReport(NO_REQUEST, code)

        fun forRequest(requestId: Int) = ErrorReport(requestId, REQUEST_FAILED)

        fun serverTime(clock: Clock = Clock.systemUTC()): String = CLOCK_FORMAT.format(clock.instant())
    }
}
