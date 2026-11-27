package zel.wired.condition

import zel.room.furni.RoomFurni
import zel.room.map.Point
import zel.wired.Hundredths
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.box.FurniSnapshot
import zel.wired.effect.furni.MatchToSnapshot
import zel.wired.firing.Firing
import zel.wired.firing.FurniSource
import zel.wired.firing.Targets

class MatchSnapshot(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val negative: Boolean,
) : ConditionBox {
    private val snapshots = settings.snapshots
    private val flags = SnapshotFlags.of(settings)

    override fun passes(firing: Firing): Boolean {
        val source = ConditionTargets.sourceOf(this, 0)
        val noEventFurni = source == FurniSource.TRIGGERING && firing.triggeringFurni == null
        val room = firing.scope.room
        val positive =
            if (source == FurniSource.PICKED || noEventFurni) {
                val kept = snapshots.filter { room.furni[it.furniId] != null }
                if (kept.isEmpty()) return Quantified.emptyHolds(settings, negative)
                Quantified.holds(kept, settings) { snapshot ->
                    room.furni[snapshot.furniId]?.let { looksLike(snapshot, it) } == true
                }
            } else {
                val targets = Targets.furni(firing, this)
                if (targets.isEmpty() && Quantified.noFurniPass(this)) return Quantified.emptyHolds(settings, negative)
                Quantified.holds(targets, settings) { target ->
                    paired(target)?.let { looksLike(it, target) } == true
                }
            }
        return positive != negative
    }

    private fun paired(furni: RoomFurni): FurniSnapshot? = snapshots.firstOrNull { it.furniId == furni.id }

    private fun looksLike(
        snapshot: FurniSnapshot,
        furni: RoomFurni,
    ): Boolean =
        (!flags.state || MatchToSnapshot.sameState(furni, snapshot.state)) &&
            (!flags.rotation || furni.direction == snapshot.direction) &&
            (!flags.position || samePlace(snapshot, furni)) &&
            (!flags.altitude || Hundredths.of(furni.z) == Hundredths.of(snapshot.z))

    private fun samePlace(
        snapshot: FurniSnapshot,
        furni: RoomFurni,
    ): Boolean = furni.anchor?.let { it == Point(snapshot.x, snapshot.y) } ?: (furni.wallPosition == snapshot.wall)
}
