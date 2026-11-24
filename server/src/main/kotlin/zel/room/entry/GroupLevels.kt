package zel.room.entry

/** The member levels [EntryStore.memberLevel] reads, lowest first. */
object GroupLevels {
    const val OWNER = 0
    const val ADMIN = 1
    const val MEMBER = 2

    /** Asked to join, not a member yet. */
    const val REQUESTED = 3
}
