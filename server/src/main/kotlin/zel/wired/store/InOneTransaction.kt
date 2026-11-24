package zel.wired.store

import zel.db.DeadlockRetry
import zel.db.allOrNothing
import java.sql.Connection
import javax.sql.DataSource

/** Read-committed, on its own connection, retried after a deadlock. */
internal fun <T : Any> DataSource.inOneTransaction(work: (Connection) -> T): T =
    DeadlockRetry.run {
        connection.use { c ->
            val isolation = c.transactionIsolation
            c.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
            try {
                checkNotNull(c.allOrNothing { work(c) })
            } finally {
                c.transactionIsolation = isolation
            }
        }
    }
