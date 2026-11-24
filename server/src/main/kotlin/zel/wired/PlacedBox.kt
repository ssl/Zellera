package zel.wired

import zel.room.map.Point
import zel.wired.box.BoxKind
import zel.wired.box.BoxSettings
import zel.wired.box.WiredBox

class PlacedBox(
    val code: String,
    val kind: BoxKind,
    val tile: Point,
    val z: Double,
    val box: WiredBox,
) {
    fun isSame(
        code: String,
        tile: Point,
        z: Double,
        settings: BoxSettings,
    ): Boolean = this.code == code && this.tile == tile && this.z == z && box.settings == settings
}
