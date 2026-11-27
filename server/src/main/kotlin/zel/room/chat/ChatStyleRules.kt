package zel.room.chat

import zel.game.players.ChatStyles

/** Which chat bubble a player may use, for a line in a room and for the saved preference alike. */
object ChatStyleRules {
    const val NORMAL = 0
    const val ALERT = 1
    const val BOT_MESSAGE = 2
    const val STAFF = 23
    const val BOT_GUIDE = 30
    const val BOT = 31
    const val FRANK = 33
    const val WIRED = 34
    const val AMBASSADOR = 37

    private val SYSTEM = 200..999
    private val NOTICES = setOf(ALERT, BOT_MESSAGE, BOT_GUIDE, FRANK, WIRED)
    private const val FIRST_OWNED = 1000

    /** Staff with any colour use every style; the staff, bot and system styles are theirs alone. */
    fun allows(
        requested: Int,
        anyStyle: Boolean,
        ambassador: Boolean,
        owns: (Int) -> Boolean,
    ): Boolean =
        when {
            requested in NOTICES -> false
            anyStyle -> true
            requested < 0 || requested == STAFF || requested == BOT || requested in SYSTEM -> false
            requested == AMBASSADOR -> ambassador
            requested >= FIRST_OWNED -> owned(requested) && owns(requested)
            else -> true
        }

    fun of(
        speaker: ChatSpeaker,
        requested: Int,
        owns: (Int) -> Boolean,
    ): Int = if (allows(requested, speaker.anyStyle, speaker.ambassador, owns)) requested else NORMAL

    private fun owned(style: Int): Boolean = style in ChatStyles.NFT_STYLES || style in ChatStyles.SHOP_STYLES
}
