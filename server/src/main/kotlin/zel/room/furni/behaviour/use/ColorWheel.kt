package zel.room.furni.behaviour.use

import zel.room.RoomScope
import zel.room.entry.DoorCheck
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.behaviour.state.StatePart
import zel.room.furni.behaviour.state.StateShow
import kotlin.random.Random

/**
 * The wheel of fortune (`chance.wheel`): a spin shows "-1" and lands on a face 1..faces after [SPIN_MILLIS]. A wired
 * toggle (state or random) spins it as a rights holder's spin does, and the landing fires the wheel's
 * wf_trg_stuff_state stacks.
 */
class ColorWheel(
    private val part: StatePart,
    private val random: Random = Random.Default,
) : Toggleable {
    fun spin(
        scope: RoomScope,
        level: Int,
        furni: RoomFurni,
    ) {
        if (level < DoorCheck.RIGHTS) return
        start(scope, furni)
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean = start(scope, furni)

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = start(scope, furni)

    /** A wheel already spinning ignores the spin. */
    private fun start(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean {
        if (part.pending(scope.room, furni)) return false
        val resting = furni.state.takeUnless { it == SPINNING } ?: STOPPED
        StateShow.data(scope, furni, SPINNING)
        part.later(scope, furni, SPIN_MILLIS, resting) { inner, wheel ->
            val faces = wheel.definition.stateCount.coerceAtLeast(1)
            StateShow.data(inner, wheel, (random.nextInt(faces) + 1).toString())
            inner.raise(RoomEvent.StateChanged(wheel.id, null, settled = true))
        }
        return true
    }

    private companion object {
        const val SPINNING = "-1"
        const val STOPPED = "0"

        /** From the spin's "-1" to the result. */
        const val SPIN_MILLIS = 4000L
    }
}
