package zel.room.furni.behaviour.state

import zel.game.achievements.GameEvents
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.unit.RoomUnit

/**
 * A furni its owner sets alight one state at a time. Its definition's params name the burnt-down state and the
 * effects that carry a flame: `state,effect,effect`.
 */
class Ignite(
    part: StatePart,
) : Multistate(part) {
    private data class Fire(
        val burnt: Int,
        val torches: Set<Int>,
    )

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        val before = furni.state
        super.onUse(scope, furni, playerId, param)
        if (furni.state != before && furni.state == fire(furni).burnt.toString()) {
            part.hooks.progress(playerId, GameEvents.FURNI_IGNITED, 1)
        }
    }

    override fun mayUse(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        playerId: Int,
    ): Boolean {
        val torches = fire(furni).torches
        return furni.ownerId == playerId && (torches.isEmpty() || part.hooks.effect(unit) in torches)
    }

    override fun clicked(
        scope: RoomScope,
        furni: RoomFurni,
    ): String? {
        val state = furni.state.trim().toIntOrNull() ?: 0
        return if (state < fire(furni).burnt) (state + 1).toString() else null
    }

    private fun fire(furni: RoomFurni): Fire {
        val named =
            furni.definition.params
                .split(',')
                .mapNotNull { it.trim().toIntOrNull() }
        return Fire(named.firstOrNull() ?: (furni.definition.stateCount - 1), named.drop(1).toSet())
    }
}
