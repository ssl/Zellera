package zel.wired.variable

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.furni.place.PickupConfirm

class VariableBoxPickup(
    private val variables: RoomVariables,
) : PickupConfirm {
    override fun needed(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean {
        val variable = definition(scope, furni) ?: return false
        val entry = variable.entry
        return Availability.isStored(entry.availability) ||
            (
                entry.target == VariableTarget.FURNI &&
                    entry.flags.hasValue &&
                    variable.holders(VariableOn(scope)).isNotEmpty()
            )
    }

    override fun staysOnSweep(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean = definition(scope, furni)?.entry?.availability == Availability.SHARED

    fun keepsRoom(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean = definition(scope, furni)?.entry?.availability?.let(Availability::isStored) == true

    private fun definition(
        scope: RoomScope,
        furni: RoomFurni,
    ): Variable? = if (Interactions.of(furni.definition) in BOXES) variables.definition(scope.room, furni) else null

    private companion object {
        val BOXES = setOf(RoomVariables.USER_BOX, RoomVariables.ROOM_BOX, RoomVariables.FURNI_BOX)
    }
}
