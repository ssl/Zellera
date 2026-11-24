package zel.room.furni.place

import zel.room.RoomScope
import zel.room.furni.RoomFurni

fun interface PickupConfirm {
    fun needed(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean

    fun staysOnSweep(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean = needed(scope, furni)

    companion object {
        val Never: PickupConfirm = PickupConfirm { _, _ -> false }
    }
}
