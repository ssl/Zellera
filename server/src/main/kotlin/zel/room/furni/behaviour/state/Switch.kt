package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit

class Switch(
    part: StatePart,
) : Multistate(part) {
    override fun mayUse(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        playerId: Int,
    ): Boolean {
        if (Near.adjacent(furni, unit.tile)) return true
        Near.walkBeside(scope, part.hooks, furni, unit)
        return false
    }
}
