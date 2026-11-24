package zel.room.furni.place

import zel.protocol.generated.ObjectUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.FurniKind
import zel.room.furni.HeightMapChange
import zel.room.furni.RoomFurni
import zel.room.furni.StackHelperState
import zel.room.tick.Audience
import zel.room.tick.Lane

class StackHelperControl(
    private val units: UnitsOnTiles = UnitsOnTiles.None,
) {
    fun slider(
        scope: RoomScope,
        access: FurniAccess,
        furniId: Int,
        hundredths: Int,
        multiWalk: Boolean?,
    ) {
        val helper = helperOf(scope, access, furniId) ?: return
        multiWalk?.let { helper.state = StackHelperState.stored(helper.z, it) }
        setZ(scope, helper, StackHelperHeights.slider(scope.room, helper, hundredths))
    }

    fun arrow(
        scope: RoomScope,
        access: FurniAccess,
        furniId: Int,
        down: Boolean,
    ) {
        val helper = helperOf(scope, access, furniId) ?: return
        setZ(scope, helper, StackHelperHeights.arrow(scope.room, helper, down))
    }

    private fun helperOf(
        scope: RoomScope,
        access: FurniAccess,
        furniId: Int,
    ): RoomFurni? {
        if (!access.mayUseHelper) return null
        val furni = scope.room.furni[furniId] ?: return null
        return furni.takeIf { it.kind == FurniKind.StackHelper || it.kind == FurniKind.WalkTile }
    }

    private fun setZ(
        scope: RoomScope,
        helper: RoomFurni,
        z: Double,
    ) {
        val tiles = helper.tiles()
        val heights = HeightMapChange.of(scope.room, tiles)
        helper.z = z
        helper.state = StackHelperState.of(helper.state).at(z)
        helper.dirty = true
        scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(helper))
        FurniOut.heights(scope, heights)
        if (helper.kind == FurniKind.WalkTile) units.furniChanged(scope, tiles, tiles)
    }
}
