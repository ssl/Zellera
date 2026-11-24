package zel.game.webcommands

@ConsistentCopyVisibility
data class WebCommandResult private constructor(
    val done: Boolean,
    val result: String,
) {
    fun status(): String = if (done) "done" else "failed"

    companion object {
        const val MAX_RESULT = 255

        fun done(result: String?): WebCommandResult = WebCommandResult(true, normalize(result))

        fun failed(result: String?): WebCommandResult = WebCommandResult(false, normalize(result))

        private fun normalize(result: String?): String = result.orEmpty().trim().take(MAX_RESULT)
    }
}
