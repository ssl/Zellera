package zel.db

import org.slf4j.Logger
import java.sql.SQLException

inline fun <T> Logger.orFallback(
    what: String,
    fallback: T,
    work: () -> T,
): T =
    try {
        work()
    } catch (failure: SQLException) {
        error("{} failed", what, failure)
        fallback
    }
