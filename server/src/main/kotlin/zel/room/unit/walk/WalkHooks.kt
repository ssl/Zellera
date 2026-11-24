package zel.room.unit.walk

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.room.unit.RoomUnit

@Suppress("TooManyFunctions") // one hook per walk moment the room parts listen to
interface WalkHooks {
    fun mayWalk(unit: RoomUnit): Boolean = true

    /** True: no walk starts. */
    fun takesClick(
        scope: RoomScope,
        unit: RoomUnit,
        clicked: Point,
    ): Boolean = false

    fun effect(unit: RoomUnit): Int = 0

    fun asleep(unit: RoomUnit): Boolean = false

    fun active(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun stepped(
        scope: RoomScope,
        unit: RoomUnit,
        onto: Point,
    ) {}

    fun horseJumped(
        scope: RoomScope,
        rider: RoomUnit,
        obstacle: RoomFurni,
    ) {}

    fun horseOnBar(
        scope: RoomScope,
        rider: RoomUnit,
        obstacle: RoomFurni,
    ) {}

    fun railStep(
        scope: RoomScope,
        unit: RoomUnit,
        inARow: Int,
    ) {}

    fun walkEnded(
        scope: RoomScope,
        unit: RoomUnit,
        steps: Int,
    ) {}

    fun postureTaken(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    /** A user stopped before the door tile and leaves with [leave] 0.5 s later. */
    fun leavingByDoor(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun leave(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    companion object {
        val None: WalkHooks = object : WalkHooks {}
    }
}
