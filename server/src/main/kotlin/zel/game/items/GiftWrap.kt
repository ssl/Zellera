package zel.game.items

/** A wrapped gift as its box's state keeps it; the furni inside have no owner until it is opened. */
data class GiftWrap(
    val contentIds: List<Int> = emptyList(),
    val paper: Int = 0,
    val ribbon: Int = 0,
    val signed: Boolean = false,
    val note: String = "",
    val senderName: String = "",
    val senderLook: String = "",
) {
    /** The sender as the receiver sees him: nobody for an unsigned gift. */
    val shownSender: String get() = if (signed) senderName else ""
    val shownLook: String get() = if (signed) senderLook else ""

    /** The note gives way first when the whole would not fit the state column. */
    fun state(): String {
        val whole = written(note)
        val over = whole.length - Items.MAX_STATE_LENGTH
        return if (over <= 0) whole else written(note.dropLast(minOf(note.length, over * 2)))
    }

    private fun written(shownNote: String): String =
        StateFields
            .write()
            .numbers(CONTENTS, contentIds)
            .number(PAPER, paper)
            .number(RIBBON, ribbon)
            .flag(SIGNED, signed)
            .text(NOTE, shownNote)
            .text(FROM, senderName)
            .text(LOOK, senderLook)
            .state()

    companion object {
        private const val CONTENTS = "contents"
        private const val PAPER = "paper"
        private const val RIBBON = "ribbon"
        private const val SIGNED = "signed"
        private const val NOTE = "note"
        private const val FROM = "from"
        private const val LOOK = "look"

        /** An empty wrap for a state that is none. */
        fun read(state: String): GiftWrap {
            val fields = StateFields.read(state) ?: return GiftWrap()
            return GiftWrap(
                fields.numbers(CONTENTS),
                fields.number(PAPER),
                fields.number(RIBBON),
                fields.flag(SIGNED),
                fields.text(NOTE),
                fields.text(FROM),
                fields.text(LOOK),
            )
        }
    }
}
