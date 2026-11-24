package zel.wired.firing

import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.unit.RoomUnit
import zel.wired.box.BoxCodes
import zel.wired.box.ConditionBox
import zel.wired.box.WiredBox
import kotlin.math.absoluteValue

object Targets {
    /**
     * Sources the filter add-ons leave alone: a box's own picks (both lists), the signal's furni and the selection,
     * which [zel.wired.StackRun] filtered once already.
     */
    private val UNFILTERED_FURNI =
        setOf(FurniSource.PICKED, FurniSource.PICKED_SECOND, FurniSource.SIGNAL, FurniSource.SELECTION)

    fun furni(
        firing: Firing,
        box: WiredBox,
        slot: Int = 0,
        picksForTriggering: Boolean = true,
        filtered: Boolean = true,
    ): List<RoomFurni> {
        val source = box.settings.furniSources.getOrNull(slot) ?: FurniSource.PICKED
        val room = firing.scope.room
        val ids =
            when (source) {
                FurniSource.TRIGGERING -> {
                    val fallback = picksForTriggering && box !is ConditionBox
                    firing.triggeringFurni?.let(::listOf)
                        ?: if (fallback) withoutEventFurni(firing, box, slot) else emptyList()
                }

                FurniSource.PICKED, FurniSource.SNAPSHOT -> {
                    picked(firing, box.settings.picks)
                }

                FurniSource.PICKED_SECOND -> {
                    picked(firing, box.settings.picks2)
                }

                FurniSource.SELECTION -> {
                    // Wired boxes in the selection count too: a toggle or a move acts on them.
                    firing.selection.furniIds.toList()
                }

                FurniSource.SIGNAL -> {
                    firing.signal?.furniIds.orEmpty()
                }

                FurniSource.ALL -> {
                    room.furni
                        .all()
                        .filterNot {
                            BoxCodes.isBox(
                                Interactions.of(it.definition),
                            )
                        }.map { it.id }
                }

                FurniSource.MOVED -> {
                    firing.moved.toList()
                }

                else -> {
                    emptyList()
                }
            }
        val present = ids.distinct().filter { room.furni[it] != null }
        val kept =
            if (!filtered || source in UNFILTERED_FURNI) {
                present
            } else {
                firing.filters.fold(present) { list, filter -> filter.furni(firing, list) }
            }
        return kept.mapNotNull { room.furni[it] }.also { firing.reachedCount += it.size }
    }

    fun units(
        firing: Firing,
        box: WiredBox,
        slot: Int = 0,
    ): List<RoomUnit> {
        val source = box.settings.userSources.getOrNull(slot) ?: UserSource.TRIGGERING
        val units = firing.scope.room.units
        val indexes =
            when (source) {
                UserSource.TRIGGERING -> listOfNotNull(firing.triggeringUnit)
                UserSource.REACHED -> listOfNotNull(firing.start.reachedUnit)
                UserSource.CLICKED -> listOfNotNull(firing.start.clicked)
                UserSource.SELECTION -> firing.selection.unitIndexes.toList()
                UserSource.SIGNAL -> firing.signal?.unitIndexes.orEmpty()
                UserSource.ALL -> units.all().map { it.index }
                else -> emptyList()
            }
        val present = indexes.distinct().filter { units[it] != null }
        val kept =
            if (source == UserSource.SELECTION) {
                present
            } else {
                firing.filters.fold(present) { list, filter -> filter.units(firing, list) }
            }
        return kept.mapNotNull { units[it] }.also { firing.reachedCount += it.size }
    }

    /**
     * Resolves targets the usage meter does not charge as reached: a selector's picks, the furni and users a
     * signal only carries, a call's picked stacks and the holders of a variable write (charged per firing only).
     */
    fun <T> uncounted(
        firing: Firing,
        resolve: () -> T,
    ): T {
        val reached = firing.reachedCount
        return resolve().also { firing.reachedCount = reached }
    }

    private fun withoutEventFurni(
        firing: Firing,
        box: WiredBox,
        slot: Int,
    ): List<Int> =
        (if (slot == 0) picked(firing, box.settings.picks) else emptyList())
            .ifEmpty { listOfNotNull(firing.triggerBox) }

    private fun picked(
        firing: Firing,
        picks: List<Int>,
    ): List<Int> =
        picks.mapNotNull { pick ->
            firing.scope.room.furni[pick.absoluteValue]
                ?.takeIf { it.isWall == (pick < 0) }
                ?.id
        }
}
