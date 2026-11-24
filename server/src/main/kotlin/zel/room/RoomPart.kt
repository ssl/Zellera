package zel.room

interface RoomPart {
    /** Fills this part of a room that is loading, before its first tick. */
    fun load(room: Room) {}

    fun unload(scope: RoomScope) {}
}
