package zel.room.chat

object ChatText {
    const val MAX_LENGTH = 200

    private val GESTURES: List<Pair<String, Int>> =
        listOf(":)", ":-)", ":]", ":D", ";)").map { it to 1 } +
            listOf(":@", ">:(").map { it to 2 } +
            listOf(":o", ":O", ":0", "O.o", "O.O").map { it to 3 } +
            listOf(":(", ":-(", ":[").map { it to 4 }

    private const val STRIPPED = "\t\r\n"

    private val COLOUR_CODES = listOf("@red@", "@cyan@", "@blue@", "@green@", "@purple@")

    fun gesture(text: String): Int = GESTURES.firstOrNull { text.contains(it.first) }?.second ?: 0

    fun colourCodes(
        text: String,
        member: Boolean,
    ): String {
        if (member) return text
        val code = COLOUR_CODES.firstOrNull { text.startsWith(it) } ?: return text
        return " " + text.substring(code.length)
    }

    /** What a line may hold at all: tabs and breaks gone, at most [MAX_LENGTH] characters, before any step reads it. */
    fun cut(text: String): String = text.filterNot { it in STRIPPED }.take(MAX_LENGTH)

    fun trim(text: String): String = cut(text).trimEnd()

    fun whisper(message: String): WhisperLine {
        val space = message.indexOf(' ')
        if (space <= 0 || space == message.length - 1) return WhisperLine(null, message)
        return WhisperLine(message.substring(0, space), message.substring(space + 1))
    }
}
