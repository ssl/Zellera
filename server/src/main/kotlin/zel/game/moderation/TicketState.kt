package zel.game.moderation

private const val WIRE_CLOSED = 3

/** [wire] is what the mod tool shows; the table stores the lower-case name. */
enum class TicketState(
    val wire: Int,
) {
    OPEN(1),
    PICKED(2),
    CLOSED(WIRE_CLOSED),
    ;

    val stored: String get() = name.lowercase()

    companion object {
        /** A status the code does not know is not on the desk. */
        fun fromStored(stored: String?): TicketState = entries.firstOrNull { it.stored == stored } ?: CLOSED
    }
}
