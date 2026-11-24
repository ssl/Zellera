package zel.wired.form

import zel.room.Room
import zel.room.furni.place.Interactions
import zel.wired.box.SourceSlot
import zel.wired.firing.UserSource

object SourceOffers {
    private const val REACHED_TRIGGER = "wf_trg_bot_reached_avtr"
    private const val CLICKED_TRIGGER = "wf_trg_click_user"

    fun userSlots(
        room: Room,
        slots: List<SourceSlot>,
    ): List<SourceSlot> {
        if (slots.none(::takesTriggerUsers)) return slots
        val codes = room.furni.all().mapTo(HashSet()) { Interactions.of(it.definition) }
        val extra =
            listOfNotNull(
                UserSource.REACHED.takeIf { REACHED_TRIGGER in codes },
                UserSource.CLICKED.takeIf { CLICKED_TRIGGER in codes },
            )
        if (extra.isEmpty()) return slots
        return slots.map { slot -> if (takesTriggerUsers(slot)) slot.copy(offered = withExtra(slot, extra)) else slot }
    }

    private fun takesTriggerUsers(slot: SourceSlot): Boolean =
        UserSource.TRIGGERING in slot.offered && UserSource.BY_NAME !in slot.offered

    private fun withExtra(
        slot: SourceSlot,
        extra: List<Int>,
    ): Set<Int> =
        slot.offered.flatMapTo(LinkedHashSet()) { source ->
            if (source == UserSource.TRIGGERING) listOf(source) + extra else listOf(source)
        }
}
