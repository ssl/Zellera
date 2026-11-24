package zel.wired.variable.addon

/** Without the key itself. */
class StoredApiKey(
    val hash: ByteArray,
    val hint: String,
    /** Null without a bridge secret. */
    val sealed: ByteArray?,
)
