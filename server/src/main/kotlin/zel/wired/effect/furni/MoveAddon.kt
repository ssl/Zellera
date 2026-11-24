package zel.wired.effect.furni

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.box.AddonBox
import zel.wired.firing.Firing

interface MoveAddon : AddonBox {
    fun style(
        firing: Firing,
        style: MoveStyle,
    ): MoveStyle = style

    fun shape(
        firing: Firing,
        shape: MoveShape,
        from: Point,
        to: Point,
    ): MoveShape = shape

    fun through(firing: Firing): MoveThrough? = null

    fun refuses(
        firing: Firing,
        furni: RoomFurni,
        tiles: List<Point>,
    ): Boolean = false

    fun landed(
        firing: Firing,
        move: FloorMove,
    ) {}
}
