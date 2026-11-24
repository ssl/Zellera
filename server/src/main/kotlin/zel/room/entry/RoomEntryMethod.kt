package zel.room.entry

/**
 * How a user came into the room, the values of `@room_entry.method` (-266). The door / navigator gives [DEFAULT];
 * a room linker trip and a teleporter pair into another room give [TELEPORT]; any other path keeps [DEFAULT].
 */
object RoomEntryMethod {
    const val DEFAULT = 0
    const val TELEPORT = 2

    /** A teleporter pair: reads as [TELEPORT] and names its arrival furni at once; a room link names it after enter. */
    const val TELEPORTER = -2
}
