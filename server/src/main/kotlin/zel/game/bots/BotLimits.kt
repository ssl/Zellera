package zel.game.bots

// Settings are @JvmField: an object property's setter would write a static field (SpotBugs ST).
object BotLimits {
    /** In seconds. */
    @JvmField
    @Volatile
    var minChatDelay = 5

    /** In seconds. */
    @JvmField
    @Volatile
    var maxChatDelay = 604_800

    @JvmField
    @Volatile
    var maxNameLength = 15

    @JvmField
    @Volatile
    var maxInventory = 25

    /** At the shortest gap these fill two minutes without a repeat. */
    const val MAX_CHAT_LINES = 25

    /** What a stored line holds at most. */
    const val STORED_LINE_LENGTH = 255

    const val STORED_MOTTO_LENGTH = 100
}
