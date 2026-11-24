package zel.wired.effect.user

import zel.protocol.generated.ObjectRemoveMultipleOut
import zel.protocol.generated.ObjectRemoveMultipleOutCodec
import zel.room.furni.HeightMapChange
import zel.room.furni.place.FurniOut
import zel.room.furni.place.UnitsOnTiles
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class RemoveFurni(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val state: UserEffectState,
    private val host: UserEffectHost,
    private val units: UnitsOnTiles,
) : EffectBox {
    override val negative = false
    override val needsUser = false

    override fun run(firing: Firing) {
        val scope = firing.scope
        val room = scope.room
        val kept = state.of(room)
        val copies = Targets.furni(firing, this).filter { it.id in kept.copies }
        if (copies.isEmpty()) return
        val tiles = copies.flatMap { it.tiles() }.distinct()
        val heights = HeightMapChange.of(room, tiles)
        copies.forEach { room.furni.remove(it.id) }
        val ids = copies.map { it.id }
        ids.forEach {
            kept.copies -= it
            kept.visuals.furniGone(it)
        }
        scope.out.send(
            Lane.Now,
            Audience.Room,
            ObjectRemoveMultipleOutCodec,
            ObjectRemoveMultipleOut(ids, NO_PICKER, false),
        )
        FurniOut.heights(scope, heights)
        units.furniChanged(scope, tiles, emptyList())
        host.destroyCopies(copies.map { it.rowId })
    }

    private companion object {
        const val NO_PICKER = -1
    }
}
