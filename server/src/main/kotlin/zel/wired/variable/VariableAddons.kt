package zel.wired.variable

import zel.room.Room
import zel.room.furni.RoomFurni

interface VariableAddons {
    fun texts(
        room: Room,
        box: RoomFurni,
    ): ValueTexts? = null

    fun levels(
        room: Room,
        box: RoomFurni,
    ): ((Long) -> Int)? = null

    fun subvariables(
        room: Room,
        variable: Variable,
        box: RoomFurni,
    ): List<Variable> = emptyList()

    companion object {
        val None: VariableAddons = object : VariableAddons {}
    }
}
