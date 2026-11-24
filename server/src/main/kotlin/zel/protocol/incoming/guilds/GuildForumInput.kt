package zel.protocol.incoming.guilds

import zel.game.guilds.forums.ForumState

internal object GuildForumInput {
    /** The client reads threads, posts and forums twenty at a time. */
    const val PAGE_SIZE = 20
    private val SUBJECT_LENGTH = 11..255
    private val MESSAGE_LENGTH = 11..16_000

    /** Any place in a list may be asked for; past its end the page is empty. */
    fun isListOffset(offset: Int): Boolean = offset >= 0

    fun isPage(
        index: Int,
        limit: Int,
    ): Boolean = isListOffset(index) && limit in 1..PAGE_SIZE

    /** A client marks the forums it showed, at most a page of them. */
    fun isReadBatch(size: Int): Boolean = size in 1..PAGE_SIZE

    fun isModerationState(value: Int): Boolean = ForumState.entries.any { it.code == value }

    fun normalize(text: String): String = text.trim()

    /** The compose window sends a subject of more than ten characters; the thread row holds up to 255. */
    fun isSubject(text: String): Boolean = text.length in SUBJECT_LENGTH

    /** The compose window sends a message of more than ten characters. */
    fun isMessage(text: String): Boolean = text.length in MESSAGE_LENGTH
}
