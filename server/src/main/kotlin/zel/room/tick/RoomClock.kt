package zel.room.tick

import zel.util.NANOS_PER_MILLI

fun interface RoomClock {
    fun now(): Long

    companion object {
        val System: RoomClock = RoomClock { java.lang.System.nanoTime() / NANOS_PER_MILLI }
    }
}
