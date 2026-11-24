package zel.wired.signal

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.wired.WiredRooms
import zel.wired.WiredStack
import zel.wired.firing.Firing
import zel.wired.firing.FiringStart
import zel.wired.firing.SignalPayload

class SignalRouter(
    private val rooms: () -> WiredRooms?,
) {
    fun send(
        firing: Firing,
        antennaId: Int,
        payload: SignalPayload,
        unit: Int?,
        furni: Int?,
    ) {
        val scope = firing.scope
        if (!flipped(firing, antennaId)) return
        val wired = rooms() ?: return
        val start = FiringStart(unit, furni, signal = payload.copy(through = antennaId))
        val into = (firing.event as? RoomEvent.Says)?.hiding?.afterLine
        firing.flash.signalling(into) {
            wired.runEach(scope, firing.event) { stack ->
                start.takeIf { listens(stack, antennaId) && !held(firing, stack, wired, start, into) }
            }
        }
    }

    /**
     * Flips the antenna for one signal through it; false when it is gone. Every signal flips it, also several in one
     * flash, which shows where it ended.
     */
    fun flipped(
        firing: Firing,
        antennaId: Int,
    ): Boolean {
        val antenna = firing.scope.room.furni[antennaId] ?: return false
        if (firing.chain.quiet) {
            // Under a delayed call the antenna flips unseen: the next firing's flash shows where it got to.
            if (firing.chain.flipsOnce(antennaId)) flip(antenna)
        } else {
            flip(antenna)
            firing.flash.changed(antennaId)
        }
        return true
    }

    private fun listens(
        stack: WiredStack,
        antennaId: Int,
    ): Boolean = (stack.trigger as? ReceiveSignal)?.listens(antennaId) == true

    /**
     * A receiver still on the chain (it sent the signal that came back to it) is not dropped: the signal waits until
     * that stack is done and then runs it, so a stack can roll again until a check stack stops signalling it back.
     * The send signal hop limit ends a loop that never stops.
     */
    private fun held(
        firing: Firing,
        stack: WiredStack,
        wired: WiredRooms,
        start: FiringStart,
        into: MutableList<(RoomScope) -> Unit>?,
    ): Boolean {
        if (!firing.chain.isRunning(stack.id)) return false
        firing.chain.hold(stack.id) {
            firing.flash.signalling(into) {
                wired.runEach(firing.scope, firing.event) { other -> start.takeIf { other.id == stack.id } }
            }
        }
        return true
    }

    private fun flip(antenna: RoomFurni) {
        antenna.state = if (antenna.state == ON) OFF else ON
        antenna.dirty = true
    }

    private companion object {
        const val ON = "1"
        const val OFF = "0"
    }
}
