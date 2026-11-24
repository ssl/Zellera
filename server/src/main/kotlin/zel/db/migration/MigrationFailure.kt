package zel.db.migration

class MigrationFailure(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
