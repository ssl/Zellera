package zel.room

sealed interface DoorMode {
    data object Open : DoorMode

    data object Doorbell : DoorMode

    /** [secret] is the sealed password as stored, never the text a visitor types. */
    data class Password(
        val secret: String,
    ) : DoorMode

    data object Invisible : DoorMode

    companion object {
        const val OPEN = "open"
        const val LOCKED = "locked"
        const val PASSWORD = "password"
        const val INVISIBLE = "invisible"

        /** The `rooms.door_mode` values, each at its door code on the wire. */
        val STORED: List<String> = listOf(OPEN, LOCKED, PASSWORD, INVISIBLE)

        /** The door code of a stored `door_mode`; an unknown value is open. */
        fun codeOf(stored: String): Int = STORED.indexOf(stored).coerceAtLeast(0)

        fun of(
            stored: String,
            secret: String,
        ): DoorMode =
            when (stored) {
                LOCKED -> Doorbell
                PASSWORD -> Password(secret)
                INVISIBLE -> Invisible
                else -> Open
            }
    }
}
