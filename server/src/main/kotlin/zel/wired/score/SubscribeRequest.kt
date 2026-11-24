package zel.wired.score

data class SubscribeRequest(
    val boardId: Int,
    val revision: Int,
    val variable: String,
    val chunks: List<Int>,
) {
    /** A board's rows are asked for as one chunk or two different ones. */
    val namesChunks: Boolean
        get() = chunks.size in 1..MAX_CHUNKS && chunks.all { it >= 0 } && chunks.distinct().size == chunks.size

    private companion object {
        const val MAX_CHUNKS = 2
    }
}
