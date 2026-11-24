package zel.wired.variable.box

import zel.wired.box.BoxSettings
import zel.wired.box.SelectionFilter
import zel.wired.firing.Firing
import zel.wired.firing.HeldValue
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableOn
import zel.wired.variable.VariableTarget

class FilterByVariable(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val variables: RoomVariables,
) : SelectionFilter {
    override val rank = FILTER_RANK

    override fun furni(
        firing: Firing,
        furniIds: List<Int>,
    ): List<Int> = kept(firing, VariableTarget.FURNI, furniIds) { Holder.Furni(it) }

    override fun units(
        firing: Firing,
        unitIndexes: List<Int>,
    ): List<Int> {
        val units = firing.scope.room.units
        return kept(firing, VariableTarget.USER, unitIndexes) { index -> units[index]?.let { Holder.of(it) } }
    }

    private fun kept(
        firing: Firing,
        target: Int,
        ids: List<Int>,
        holderOf: (Int) -> Holder?,
    ): List<Int> {
        val variable =
            variables
                .find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty())
                ?.takeIf { it.entry.target == target }
                ?: return ids
        val on = VariableOn(firing.scope, firing)
        val held = ids.mapNotNull { id -> holderOf(id)?.let { variable.read(on, it) }?.let { id to it } }
        val sorted = held.sortedWith(order(settings.intAt(SORT))).map { it.first }
        val amount = amount(firing, on)
        return if (amount == null || amount <= 0) sorted else sorted.take(amount)
    }

    private fun amount(
        firing: Firing,
        on: VariableOn,
    ): Int? {
        if (settings.intAt(FROM_VARIABLE) == 0) return settings.intAt(AMOUNT)
        val reference =
            variables.find(firing.scope.room, settings.variableIds.getOrNull(1).orEmpty()) ?: return null
        val holder =
            when (settings.intAt(REFERENCE_TARGET)) {
                VariableTarget.GLOBAL -> Holder.Room
                VariableTarget.CONTEXT -> Holder.Context
                else -> firing.triggeringUnit?.let { firing.scope.room.units[it] }?.let { Holder.of(it) }
            } ?: return null
        return reference
            .read(on, holder)
            ?.value
            ?.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
            ?.toInt()
    }

    private fun order(sort: Int): Comparator<Pair<Int, HeldValue>> {
        val by: Comparator<Pair<Int, HeldValue>> =
            when (sort) {
                SORT_LOWEST -> compareBy { it.second.value }
                SORT_OLDEST_CREATED -> compareBy { it.second.created }
                SORT_NEWEST_CREATED -> compareByDescending { it.second.created }
                SORT_OLDEST_UPDATED -> compareBy { it.second.updated }
                SORT_NEWEST_UPDATED -> compareByDescending { it.second.updated }
                else -> compareByDescending { it.second.value }
            }
        return by.thenBy { it.first }
    }

    private companion object {
        const val FILTER_RANK = 0
        const val AMOUNT = 0
        const val SORT = 1
        const val FROM_VARIABLE = 2
        const val REFERENCE_TARGET = 3
        const val SORT_LOWEST = 1
        const val SORT_OLDEST_CREATED = 2
        const val SORT_NEWEST_CREATED = 3
        const val SORT_OLDEST_UPDATED = 4
        const val SORT_NEWEST_UPDATED = 5
    }
}
