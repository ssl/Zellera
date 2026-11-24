package zel.db

import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.sql.SQLTransactionRollbackException

object DeadlockRetry {
    private val log = LoggerFactory.getLogger(DeadlockRetry::class.java)
    private const val ATTEMPTS = 5
    private const val PAUSE_MILLIS = 25L
    private const val DEADLOCK_ERROR = 1213
    private const val ROW_CHANGED_ERROR = 1020

    /** The last deadlock, or any other failure, reaches the caller. */
    fun <T> run(
        attempts: Int = ATTEMPTS,
        pause: (Long) -> Unit = Thread::sleep,
        write: () -> T,
    ): T {
        var attempt = 1
        while (true) {
            try {
                return write()
            } catch (failure: SQLException) {
                if (attempt >= attempts || !isDeadlock(failure)) throw failure
                log.warn("write retried after a deadlock, attempt {}", attempt)
                pause(PAUSE_MILLIS * attempt)
                attempt++
            }
        }
    }

    private fun isDeadlock(failure: SQLException): Boolean =
        failure is SQLTransactionRollbackException ||
            failure.errorCode == DEADLOCK_ERROR ||
            failure.errorCode == ROW_CHANGED_ERROR
}
