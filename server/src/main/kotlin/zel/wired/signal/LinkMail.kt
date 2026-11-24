package zel.wired.signal

sealed interface LinkMail {
    val hops: Int

    /** What a Link carries at most, whatever a box or a trigger handed it. */
    fun bounded(): LinkMail

    data class Message(
        val text: String,
        override val hops: Int,
    ) : LinkMail {
        override fun bounded(): LinkMail = if (text.length <= MAX_MESSAGE) this else copy(text = text.take(MAX_MESSAGE))
    }

    data class Frame(
        val rows: List<FrameRow>,
        override val hops: Int,
    ) : LinkMail {
        override fun bounded(): LinkMail =
            copy(
                rows =
                    rows.take(SendDataframe.MAX_ROWS).map { row ->
                        if (row.text.length <= MAX_TEXT) row else row.copy(text = row.text.take(MAX_TEXT))
                    },
            )
    }

    companion object {
        const val MAX_TEXT = 400
        const val MAX_MESSAGE = 1000
    }
}
