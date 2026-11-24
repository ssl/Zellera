package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Placeable
import kotlin.random.Random

class Controller(
    part: StatePart,
    private val viewers: Boolean,
    private val keepsOnPickup: Boolean = false,
    /** The invisible furni controller: its state takes the invisible furni out of view or gives them back. */
    private val hidesFurni: Boolean = false,
) : Multistate(part),
    Placeable {
    override fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String = flipped(furni)

    override fun show(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        StateShow.data(scope, furni, state)
        settled(scope)
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        StateShow.set(furni, flipped(furni))
        settled(scope)
        return true
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = toggle(scope, furni, backward = false)

    override fun onPickedUp(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        val wasOn = furni.state.trim() == RoomControllers.ON
        StateShow.set(furni, RoomControllers.OFF)
        if (!wasOn || !viewers) return
        if (keepsOnPickup) part.of(scope.room).chooserHeld = true
        part.resendViewerStates(scope)
        part.of(scope.room).chooserHeld = false
        if (hidesFurni) InvisibleFurni.sync(part, scope)
    }

    /** The room learns what the controller's new state does. */
    private fun settled(scope: RoomScope) {
        if (viewers) part.resendViewerStates(scope)
        if (hidesFurni) InvisibleFurni.sync(part, scope)
    }

    private fun flipped(furni: RoomFurni): String =
        if (furni.state.trim() == RoomControllers.ON) RoomControllers.OFF else RoomControllers.ON

    companion object {
        /** After [furni]'s state was set other than by its own toggle (a wired variable write, a snapshot). */
        fun written(
            scope: RoomScope,
            furni: RoomFurni,
        ) {
            (furni.behaviour as? Controller)?.settled(scope)
        }
    }
}
