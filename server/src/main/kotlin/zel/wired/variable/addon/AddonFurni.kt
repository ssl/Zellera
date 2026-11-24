package zel.wired.variable.addon

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.place.Placeable
import zel.wired.protocol.BoxUse

class AddonFurni(
    private val use: BoxUse,
    private val placed: (RoomScope, RoomFurni) -> Unit,
    private val pickedUp: (RoomScope, RoomFurni) -> Unit,
) : Usable by use,
    Placeable {
    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        use.onPlaced(scope, furni)
        placed(scope, furni)
    }

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        use.onPickedUp(scope, furni)
        pickedUp(scope, furni)
    }
}
