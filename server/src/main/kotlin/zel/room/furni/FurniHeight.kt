package zel.room.furni

import zel.game.items.FurniHeights
import zel.game.items.Item
import kotlin.math.roundToLong

object FurniHeight {
    private const val HUNDRED = 100.0
    private const val STATE_SEPARATOR = ';'

    fun current(
        definition: Item,
        kind: FurniKind,
        state: String,
    ): Double =
        when (kind) {
            FurniKind.MultiHeight -> multiHeight(definition, state)
            FurniKind.Recolorable -> recolorable(definition, state)
            FurniKind.Plain, FurniKind.StackHelper, FurniKind.WalkTile -> definition.height
        }

    fun onWire(
        definition: Item,
        kind: FurniKind,
        state: String,
    ): String =
        when (kind) {
            FurniKind.Recolorable -> recolorableOnWire(definition, state).toString()
            else -> FurniHeights.formatHeight(current(definition, kind, state))
        }

    private fun multiHeight(
        definition: Item,
        state: String,
    ): Double {
        val heights = definition.stateHeights
        val index = if (state.isEmpty()) 0 else state.toIntOrNull() ?: return definition.height
        if (heights.isEmpty()) return steppedHeight(definition, index)
        return heights[Math.floorMod(index, heights.size)]
    }

    /**
     * A multi-height furni without state heights whose params are a negative step: max(0, height + state x step) in
     * 32-bit floats.
     */
    private fun steppedHeight(
        definition: Item,
        index: Int,
    ): Double {
        val step = stepHeight(definition.params).toFloat()
        if (step == 0.0f || definition.stateCount <= 0) return definition.height
        val state = Math.floorMod(index, definition.stateCount)
        return maxOf(0.0f, definition.height.toFloat() + state * step).toDouble()
    }

    private fun recolorable(
        definition: Item,
        state: String,
    ): Double {
        val step = blockState(state)
        if (step <= 0) return definition.height
        val lowered = definition.height + step * stepHeight(definition.params)
        return maxOf(0.0, (lowered * HUNDRED).roundToLong() / HUNDRED)
    }

    private fun recolorableOnWire(
        definition: Item,
        state: String,
    ): Float {
        val step = blockState(state)
        if (step <= 0) return definition.height.toFloat()
        return maxOf(0.0f, definition.height.toFloat() + step * stepHeight(definition.params).toFloat())
    }

    private fun blockState(state: String): Int = state.substringBefore(STATE_SEPARATOR).toIntOrNull() ?: 0

    private fun stepHeight(customParams: String): Double =
        customParams.trim().toDoubleOrNull()?.takeIf { it <= 0.0 } ?: 0.0
}
