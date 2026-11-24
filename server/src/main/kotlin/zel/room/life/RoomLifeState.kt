package zel.room.life

class RoomLifeState {
    var ticks: Long = 0

    var emptySince: Long? = null

    var unloadRequested: Boolean = false

    var lastVisitors: Set<Int> = emptySet()

    val units = HashMap<Int, UnitTrack>()
}
