package zel.wired.usage

class ErrorTable {
    private val counts = IntArray(WiredError.entries.size)
    private val latest = LongArray(WiredError.entries.size) { NEVER }

    fun record(
        error: WiredError,
        atMillis: Long,
    ) {
        counts[error.ordinal]++
        latest[error.ordinal] = atMillis
    }

    fun count(error: WiredError): Int = counts[error.ordinal]

    fun clear() {
        counts.fill(0)
        latest.fill(NEVER)
    }

    /** Wall-clock milliseconds; [NEVER] when it never happened. */
    fun latest(error: WiredError): Long = latest[error.ordinal]

    companion object {
        const val NEVER = -1L
    }
}
