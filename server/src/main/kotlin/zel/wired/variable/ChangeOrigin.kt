package zel.wired.variable

object ChangeOrigin {
    const val IN_ROOM = 0
    const val ANOTHER_ROOM = 1
    const val INSPECTION = 2
    const val EXTERNAL = 3

    /** A change in a shared variable's room as its references see it: a web API write stays external. */
    fun seenElsewhere(origin: Int): Int = if (origin == EXTERNAL) EXTERNAL else ANOTHER_ROOM
}
