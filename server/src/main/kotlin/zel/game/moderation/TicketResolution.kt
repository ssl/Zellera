package zel.game.moderation

/** Why a ticket was closed; the table stores the lower-case name, NULL while it is on the desk. */
enum class TicketResolution {
    /** The reporter took it back before a moderator picked it. */
    WITHDRAWN,
    USELESS,
    ABUSIVE,

    /** A moderator acted on it. */
    HANDLED,

    /** The topic's own action answered it. */
    AUTOMATIC,
    ;

    val stored: String get() = name.lowercase()

    companion object {
        fun fromStored(stored: String?): TicketResolution? = entries.firstOrNull { it.stored == stored }
    }
}
