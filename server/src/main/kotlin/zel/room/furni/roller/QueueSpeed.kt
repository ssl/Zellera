package zel.room.furni.roller

import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.Usable
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Placeable
import zel.room.tick.Audience
import zel.room.tick.Lane

class QueueSpeed(
    private val mayUse: (RoomScope, Int) -> Boolean = { scope, playerId -> scope.room.info.ownerId == playerId },
) : Usable,
    Placeable {
    override fun onPlaced(
        scope: RoomScope,
        furni: RoomFurni,
    ) = show(scope, furni, furni.state.trim().toIntOrNull() ?: 0)

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if (mayUse(scope, playerId)) nextSpeed(scope, furni)
    }

    fun nextSpeed(
        scope: RoomScope,
        furni: RoomFurni,
    ) = show(scope, furni, (speedOf(furni.state) + 1) % SPEEDS * FRAMES)

    companion object {
        const val INTERACTION = "roller.speed"

        private const val FRAMES = 3
        private const val SPEEDS = 4

        fun speedOf(state: String): Int = (state.trim().toIntOrNull() ?: 0).coerceIn(0, SPEEDS * FRAMES - 1) / FRAMES

        fun isController(furni: RoomFurni): Boolean = furni.definition.interaction.name == INTERACTION

        fun animate(
            scope: RoomScope,
            furni: RoomFurni,
        ) {
            val state = furni.state.trim().toIntOrNull() ?: 0
            val speed = speedOf(furni.state)
            show(scope, furni, speed * FRAMES + (Math.floorMod(state, FRAMES) + 1) % FRAMES)
        }

        private fun show(
            scope: RoomScope,
            furni: RoomFurni,
            state: Int,
        ) {
            furni.state = state.toString()
            furni.dirty = true
            scope.out.send(
                Lane.Now,
                Audience.Room,
                ObjectDataUpdateOutCodec,
                ObjectDataUpdateOut(furni.id.toString(), FurniWire.stuff(furni)),
            )
        }
    }
}
