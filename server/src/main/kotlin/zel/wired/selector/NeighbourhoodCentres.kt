package zel.wired.selector

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.box.BoxSettings
import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource
import zel.wired.firing.UserSource

internal object NeighbourhoodCentres {
    private const val AROUND_FURNI = 0

    fun of(
        firing: Firing,
        box: WiredBox,
    ): List<Point> {
        val settings = box.settings
        val shiftX = settings.intAt(1).toLong()
        val shiftY = settings.intAt(2).toLong()
        val fit: (Long) -> Int = if (box is FurniSelector) ::wrapped else ::clamped
        val sources = if (settings.intAt(0, 1) == AROUND_FURNI) furni(firing, box) else units(firing, box)
        return sources.map { Point(fit(it.x - shiftX), fit(it.y - shiftY)) }
    }

    private fun furni(
        firing: Firing,
        box: WiredBox,
    ): List<Point> {
        val source = box.settings.furniSources.firstOrNull() ?: FurniSource.PICKED
        val settings =
            if (source == FurniSource.SIGNAL && firing.signal == null) {
                box.settings.copy(furniSources = listOf(FurniSource.TRIGGERING))
            } else {
                box.settings
            }
        return SelectorSources.furni(firing, retargeted(box, settings)).mapNotNull(RoomFurni::anchor)
    }

    private fun units(
        firing: Firing,
        box: WiredBox,
    ): List<Point> {
        val source = box.settings.userSources.firstOrNull() ?: UserSource.TRIGGERING
        val settings =
            if (source == UserSource.SIGNAL && firing.signal == null) {
                box.settings.copy(userSources = listOf(UserSource.TRIGGERING))
            } else {
                box.settings
            }
        return SelectorSources.units(firing, retargeted(box, settings)).map { it.tile }
    }

    private fun retargeted(
        box: WiredBox,
        form: BoxSettings,
    ): WiredBox =
        object : WiredBox {
            override val furniId = box.furniId
            override val settings = form
        }

    private fun wrapped(value: Long): Int = value.toShort().toInt()

    private fun clamped(value: Long): Int = value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
}
