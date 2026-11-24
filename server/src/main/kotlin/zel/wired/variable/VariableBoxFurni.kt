package zel.wired.variable

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.place.Placeable
import zel.wired.protocol.BoxUse

class VariableBoxFurni(
    private val use: BoxUse,
    private val variables: RoomVariables,
) : Usable by use,
    Placeable {
    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) = use.onPlaced(scope, furni)

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        use.onPickedUp(scope, furni)
        variables.boxGone(scope.room, furni)
    }
}
