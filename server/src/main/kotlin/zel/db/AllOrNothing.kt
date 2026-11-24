package zel.db

import java.sql.Connection

/** Null or a throw rolls back; call it outside any transaction, as it commits what came before. */
inline fun <T : Any> Connection.allOrNothing(crossinline work: () -> T?): T? {
    val restoreAutoCommit = autoCommit
    autoCommit = false
    try {
        val result = work()
        if (result == null) rollback() else commit()
        return result
    } catch (
        // whatever fails, the transaction is rolled back before the failure goes on
        @Suppress("TooGenericExceptionCaught") failure: Throwable,
    ) {
        runCatching { rollback() }.exceptionOrNull()?.let(failure::addSuppressed)
        throw failure
    } finally {
        autoCommit = restoreAutoCommit
    }
}
