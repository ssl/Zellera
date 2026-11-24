package zel.wired.addon.label

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Placeable

class LabelFurni(
    private val labels: RoomLabels,
    private val out: LabelOut,
) : Placeable {
    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        labels.placed(scope.room, furni.id)
        out.changed(scope, furni.id, Label(), data = false)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        labels.pickedUp(scope.room, furni.id)
        out.removed(scope, furni.id)
    }
}
