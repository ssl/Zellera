package zel.wired.variable

import zel.wired.firing.HeldValue

/** Touched only under the room's lock, a shared box's map excepted. */
class VariableSpace(
    val stored: StoredValues,
    val startedAt: Long = 0,
) {
    val homes = HashMap<Int, ValueHome>()

    val internalFlags = HashMap<String, MutableSet<Holder>>()

    val players = HashMap<Int, Holder>()

    val icons = HashMap<Int, Pair<Int, Long>>()

    val flights = HashMap<Int, ProjectileFlights.Flight>()

    fun loaded(
        boxId: Int,
        target: Int,
    ): Map<Holder, HeldValue> =
        when (target) {
            VariableTarget.USER -> stored.users[boxId].orEmpty().mapKeys { Holder.Player(it.key) }
            VariableTarget.FURNI -> stored.furni[boxId].orEmpty().mapKeys { Holder.Furni(it.key) }
            VariableTarget.GLOBAL -> stored.rooms[boxId]?.let { mapOf<Holder, HeldValue>(Holder.Room to it) }.orEmpty()
            else -> emptyMap()
        }
}
