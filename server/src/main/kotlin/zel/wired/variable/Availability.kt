package zel.wired.variable

object Availability {
    const val WHILE_IN_ROOM = 0
    const val WHILE_ROOM_ACTIVE = 1
    const val PERMANENT = 10
    const val SHARED = 11
    const val REFERENCE = 20
    const val EXECUTION = 999

    fun isStored(availability: Int): Boolean = availability == PERMANENT || availability == SHARED
}
