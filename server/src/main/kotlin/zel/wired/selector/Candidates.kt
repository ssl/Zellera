package zel.wired.selector

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.unit.UnitKind
import zel.wired.box.BoxCodes
import zel.wired.box.BoxKind

object Candidates {
    /**
     * Furni a furni selector may take: with [anyBox] every wired box counts (a selector's own matches and its invert);
     * without it only add-on boxes do (what effects read from the selection).
     */
    fun selectable(
        furni: RoomFurni,
        wallItems: Boolean,
        anyBox: Boolean = false,
    ): Boolean {
        val kind = BoxCodes.kindOf(Interactions.of(furni.definition))
        return (anyBox || selectableKind(kind)) && (wallItems || !furni.isWall)
    }

    private fun selectableKind(kind: BoxKind?): Boolean = kind == null || kind == BoxKind.Addon

    fun furni(
        room: Room,
        wallItems: Boolean,
    ): Set<Int> =
        room.furni
            .all()
            .filter { selectable(it, wallItems, anyBox = true) }
            .map { it.id }
            .toSet()

    fun units(
        room: Room,
        avatarsOnly: Boolean,
    ): Set<Int> =
        room.units
            .all()
            .filter { !avatarsOnly || it.kind is UnitKind.Player }
            .map { it.index }
            .toSet()
}
