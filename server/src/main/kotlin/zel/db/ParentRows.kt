package zel.db

import java.sql.SQLException

/** For writes whose parent row may be deleted while they wait on the writer. */
object ParentRows {
    /** The database's error for a foreign key whose parent row is missing. */
    const val NO_PARENT = 1452

    inline fun unlessGone(write: () -> Unit) {
        try {
            write()
        } catch (e: SQLException) {
            if (e.errorCode != NO_PARENT) throw e
        }
    }
}
