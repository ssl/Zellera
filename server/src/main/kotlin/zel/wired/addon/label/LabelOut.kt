package zel.wired.addon.label

import zel.protocol.generated.FurnitureLabelsAndRegionsOut
import zel.protocol.generated.FurnitureLabelsAndRegionsUpdateOut
import zel.protocol.generated.FurnitureLabelsAndRegionsUpdateOutCodec
import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.StuffData
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.store.RoomBoxForms

/** A label only wired readers see goes to the others as a removal. */
class LabelOut(
    private val forms: RoomBoxForms,
    private val levels: (zel.room.Room) -> Map<Int, Int>,
) {
    fun entries(
        scope: RoomScope,
        labels: List<Pair<Int, Label>>,
        level: Int,
    ): FurnitureLabelsAndRegionsOut {
        val reads = forms.masks(scope.room).mayRead(level)
        return FurnitureLabelsAndRegionsOut(
            labels.filter { (_, label) -> label.visibleTo(reads) }.map { (id, label) -> item(id, label) },
        )
    }

    fun changed(
        scope: RoomScope,
        id: Int,
        label: Label,
        data: Boolean,
    ) {
        if (data) {
            val update = ObjectDataUpdateOut(id.toString(), StuffData(0, StuffData.Value.Legacy("")))
            scope.out.send(Lane.Now, Audience.Room, ObjectDataUpdateOutCodec, update)
        }
        val masks = forms.masks(scope.room)
        levels(scope.room).forEach { (playerId, level) ->
            val update =
                if (label.visibleTo(masks.mayRead(level))) {
                    FurnitureLabelsAndRegionsUpdateOut(listOf(update(id, label)), emptyList())
                } else {
                    FurnitureLabelsAndRegionsUpdateOut(emptyList(), listOf(id))
                }
            scope.out.send(Lane.Now, Audience.Player(playerId), FurnitureLabelsAndRegionsUpdateOutCodec, update)
        }
    }

    fun removed(
        scope: RoomScope,
        id: Int,
    ) {
        val update = FurnitureLabelsAndRegionsUpdateOut(emptyList(), listOf(id))
        scope.out.send(Lane.Now, Audience.Room, FurnitureLabelsAndRegionsUpdateOutCodec, update)
    }

    private fun item(
        id: Int,
        label: Label,
    ) = FurnitureLabelsAndRegionsOut.List1Item(0, id, label.text, label.color, label.visibility, label.alwaysOpen)

    private fun update(
        id: Int,
        label: Label,
    ) = FurnitureLabelsAndRegionsUpdateOut.List1Item(
        0,
        id,
        label.text,
        label.color,
        label.visibility,
        label.alwaysOpen,
    )
}
