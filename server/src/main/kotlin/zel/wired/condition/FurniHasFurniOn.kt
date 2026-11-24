package zel.wired.condition

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class FurniHasFurniOn(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val room = firing.scope.room
        val targets = Targets.furni(firing, this)
        // An empty set, read per target like the rest: "all" holds, "any" does not, the not box alike.
        if (targets.isEmpty()) return Quantified.noFurniPass(this) && settings.intAt(0) == ALL
        return Quantified.perTarget(
            targets,
            settings.intAt(0) == ALL,
            negative,
        ) { covered(room, it) }
    }

    private fun covered(
        room: Room,
        furni: RoomFurni,
    ): Boolean =
        furni.tiles().any { tile ->
            room.furni
                .at(tile)
                .furni
                .any { it !== furni && it.z >= furni.top - TOLERANCE }
        }

    private companion object {
        const val ALL = 1
        const val TOLERANCE = 0.0001
    }
}
