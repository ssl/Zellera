package zel.net.server.handlers

/** A pong only counts while a ping waits for one. */
internal class QuietWatch(
    openedAt: Long,
    private val gapMillis: Long,
    private val quietMillis: Long,
    loginMillis: Long,
) {
    enum class Call {
        NOTHING,
        PING,

        DROP_UNANSWERED,
        DROP_QUIET,

        DROP_NOT_LOGGED_IN,
    }

    private var dropDue = openedAt + quietMillis - TOLERANCE_MILLIS
    private var pingDue = Long.MIN_VALUE
    private val loginDue = openedAt + loginMillis - TOLERANCE_MILLIS

    var awaitingPong = false
        private set

    fun heard(
        now: Long,
        pong: Boolean,
    ): Boolean {
        dropDue = now + quietMillis - TOLERANCE_MILLIS
        val answer = pong && awaitingPong
        if (answer) awaitingPong = false
        return answer
    }

    /** Only a socket [withPlayer] gets pings. */
    fun due(
        now: Long,
        withPlayer: Boolean,
    ): Call =
        when {
            now >= dropDue -> {
                if (awaitingPong) Call.DROP_UNANSWERED else Call.DROP_QUIET
            }

            !withPlayer && now >= loginDue -> {
                Call.DROP_NOT_LOGGED_IN
            }

            withPlayer && now >= pingDue -> {
                pingDue = now + gapMillis
                awaitingPong = true
                Call.PING
            }

            else -> {
                Call.NOTHING
            }
        }

    companion object {
        /** A check that runs this much before the deadline counts as reaching it. */
        const val TOLERANCE_MILLIS = 1_000L
    }
}
