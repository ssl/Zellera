package zel.protocol.friends

internal object FriendInput {
    /** The longest name a player can have. */
    const val MAX_NAME_LENGTH = 15

    private const val MAX_CHAT_LENGTH = 255

    /** What the invitation box of the client lets through. */
    private const val MAX_INVITE_LENGTH = 120
    private val RELATIONS = 0..3

    fun name(raw: String): String? = raw.trim().takeIf(::canStartAName)

    /** No name holds white space or runs longer than [MAX_NAME_LENGTH], so such a text matches nobody. */
    fun canStartAName(text: String): Boolean =
        text.isNotEmpty() && text.length <= MAX_NAME_LENGTH && text.none { it.isWhitespace() }

    /** What a search looks for: the typed text without its white space, no longer than a name. */
    fun searchText(raw: String): String = raw.filterNot { it.isWhitespace() }.lowercase().take(MAX_NAME_LENGTH)

    fun chatText(raw: String): String = raw.trim { it <= ' ' }.take(MAX_CHAT_LENGTH)

    fun inviteText(raw: String): String = raw.trim().take(MAX_INVITE_LENGTH)

    fun isRelation(value: Int): Boolean = value in RELATIONS

    /** The player ids of a request, each once; nobody can name more players than a friend list holds. */
    fun batch(
        ids: List<Int>,
        listSize: Int,
    ): List<Int> =
        ids
            .asSequence()
            .filter { it > 0 }
            .distinct()
            .take(listSize)
            .toList()
}
