package zel.room.furni.behaviour.state

import zel.room.Room
import zel.room.RoomScope
import zel.room.map.Point
import zel.room.unit.RoomUnit

/** Called under the room lock. */
@Suppress("TooManyFunctions") // one per room service
interface StateHooks {
    fun hasRights(
        scope: RoomScope,
        playerId: Int,
    ): Boolean = playerId == scope.room.info.ownerId

    fun male(unit: RoomUnit): Boolean = true

    fun effect(unit: RoomUnit): Int = 0

    /** Gives an avatar effect (0 removes it) until [endsAt] on the engine clock, or for good when null. */
    fun giveEffect(
        scope: RoomScope,
        unit: RoomUnit,
        effect: Int,
        endsAt: Long?,
    ) {}

    fun blocksEffect(
        playerId: Int,
        effect: Int,
    ): Boolean = false

    fun holdItem(
        scope: RoomScope,
        unit: RoomUnit,
        item: Int,
    ) {}

    fun handItemFromFurni(
        scope: RoomScope,
        unit: RoomUnit,
        item: Int,
    ) = holdItem(scope, unit, item)

    fun walkTo(
        scope: RoomScope,
        unit: RoomUnit,
        goal: Point,
    ): Boolean = false

    fun stopWalk(unit: RoomUnit) {}

    fun holdStill(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun letGo(unit: RoomUnit) {}

    fun sendOut(
        scope: RoomScope,
        unit: RoomUnit,
    ) {}

    fun unkickable(playerId: Int): Boolean = false

    fun heightsChanged(
        scope: RoomScope,
        tiles: Collection<Point>,
    ) {}

    fun saveNow(room: Room) {}

    fun progress(
        playerId: Int,
        achievement: String,
        amount: Int,
    ) {}
}
