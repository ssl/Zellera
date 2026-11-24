package zel.room.chat

interface ChatHost {
    fun rules(): ChatRules = ChatRules()

    fun speaker(playerId: Int): ChatSpeaker? = null

    fun ignores(
        receiverId: Int,
        speakerId: Int,
    ): Boolean = false

    fun ownsStyle(
        playerId: Int,
        styleId: Int,
    ): Boolean = false

    fun filter(
        playerId: Int,
        text: String,
    ): ChatFilterResult = ChatFilterResult.Say(text)

    fun heardText(
        receiverId: Int,
        text: String,
    ): String = text

    fun command(
        playerId: Int,
        text: String,
    ): Boolean = false

    fun talked(playerId: Int) {}

    fun log(line: ChatLogLine) {}

    companion object {
        val None: ChatHost = object : ChatHost {}
    }
}
