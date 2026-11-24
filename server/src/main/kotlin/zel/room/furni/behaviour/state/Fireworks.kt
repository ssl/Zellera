package zel.room.furni.behaviour.state

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import kotlin.random.Random

/** Lit from beside it, by a rights holder from anywhere; the show lasts [SHOW_MILLIS]. */
class Fireworks(
    private val part: StatePart,
) : Usable,
    Toggleable {
    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val unit = scope.room.units.ofPlayer(playerId) ?: return
        if (!part.hooks.hasRights(scope, playerId) && !Near.adjacent(furni, unit.tile)) {
            Near.walkBeside(scope, part.hooks, furni, unit)
            return
        }
        if (!charged(scope, furni)) return
        explode(scope, furni)
        StateShow.data(scope, furni, EXPLODING)
        scope.raise(RoomEvent.StateChanged(furni.id, unit.index))
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        if (!charged(scope, furni)) return false
        StateShow.set(furni, EXPLODING)
        explode(scope, furni)
        return true
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean = false

    /** Charged, or left exploding by a recharge that never ran. */
    private fun charged(
        scope: RoomScope,
        furni: RoomFurni,
    ): Boolean = furni.state == CHARGED || (furni.state == EXPLODING && !part.pending(scope.room, furni))

    private fun explode(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        part.later(scope, furni, SHOW_MILLIS, CHARGED) { inner, target -> StateShow.data(inner, target, CHARGED) }
    }

    private companion object {
        const val CHARGED = "1"
        const val EXPLODING = "2"
        const val SHOW_MILLIS = 5000L
    }
}
