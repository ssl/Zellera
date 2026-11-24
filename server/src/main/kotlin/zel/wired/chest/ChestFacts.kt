package zel.wired.chest

import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.wired.variable.VariableFacts

class ChestFacts(
    private val next: VariableFacts,
    private val chests: RoomChests,
    private val deals: ChestDeals,
) : VariableFacts by next {
    override fun chest(furni: RoomFurni): Map<String, Long> {
        val chest = chests.known(furni.id) ?: return emptyMap()
        return buildMap {
            put(AVAILABLE, chest.contents.toLong())
            if (chest.locked) put(LOCKED, 0)
            if (chest.autoLock) put(AUTO_LOCK, 0)
            put(CAPACITY, chest.capacity.toLong())
            if (furni.state != CLOSED) put(OPEN, 0)
            if (chest.preferences.everyoneDonates) put(DONATABLE, 0)
        }
    }

    override fun transaction(
        scope: RoomScope,
        playerId: Int,
    ): Map<String, Long> {
        val table = deals.table(scope.room, playerId) ?: return emptyMap()
        val (canAccept, sets) = deals.offerState(table)
        return buildMap {
            put(IN_TRADE, 0)
            put(START_TIME, table.startedAt)
            put(CONTRACT, table.contractId.toLong())
            put(STATE, if (table.acceptedAt != null) STATE_ACCEPTED else STATE_ADDING)
            if (canAccept) put(CAN_ACCEPT, 0)
            put(SETS, sets.toLong())
        }
    }

    private companion object {
        const val CLOSED = "0"
        const val AVAILABLE = "~130"
        const val LOCKED = "~131"
        const val AUTO_LOCK = "~132"
        const val CAPACITY = "~133"
        const val OPEN = "~134"
        const val DONATABLE = "~135"
        const val IN_TRADE = "-281"
        const val START_TIME = "-282"
        const val CONTRACT = "-283"
        const val STATE = "-284"
        const val CAN_ACCEPT = "-285"
        const val SETS = "-286"
        const val STATE_ADDING = 1L
        const val STATE_ACCEPTED = 2L
    }
}
