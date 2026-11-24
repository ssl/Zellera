package zel.wired.score

import zel.game.items.FurniParams
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.room.RoomListener
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Steppable
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.furni.place.Placeable
import zel.room.games.GamesPart
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class Blobs(
    private val games: () -> GamesPart?,
) : Placeable,
    Steppable,
    RoomListener {
    override fun beforeAdded(furni: RoomFurni) {
        furni.state = COLLECTED
    }

    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        furni.dirty = true
        update(scope, furni)
    }

    override fun onStepOn(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) {
        if (furni.state != OUT) return
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val part = games() ?: return
        if (part.of(scope.room).playing[playerId]?.memberOf(playerId) == null) return
        set(scope, furni, COLLECTED)
        part.giveUserScore(scope, unit, pointsOf(furni.definition.params))
    }

    override fun onStepOff(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
    ) = Unit

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        if (event != RoomEvent.GameStarted) return
        scope.room.furni
            .all()
            .filter { Interactions.of(it.definition) == CODE }
            .forEach { set(scope, it, OUT) }
    }

    private fun set(
        scope: RoomScope,
        furni: RoomFurni,
        state: String,
    ) {
        furni.state = state
        furni.dirty = true
        update(scope, furni)
    }

    private fun update(
        scope: RoomScope,
        furni: RoomFurni,
    ) = scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(furni))

    companion object {
        const val CODE = "wf_blob"
        private const val COLLECTED = "1"
        private const val OUT = "0"
        private const val POINTS = 1

        /** A blob's params are `points,visible`: the super blob names 5. */
        fun pointsOf(params: String): Int = FurniParams.int(params, 0)?.takeIf { it > 0 } ?: POINTS
    }
}
