package zel.wired.variable.box

import zel.room.event.RoomEvent
import zel.wired.box.BoxSettings
import zel.wired.box.TriggerBox
import zel.wired.firing.Firing
import zel.wired.variable.ChangeType
import zel.wired.variable.VariableChange

class VariableChanged(
    override val furniId: Int,
    override val settings: BoxSettings,
) : TriggerBox {
    override fun matches(
        event: RoomEvent,
        firing: Firing,
    ): Boolean = false

    fun watches(change: VariableChange): Boolean =
        watchesVariable(change) && wantsOrigin(change.origin) && wantsType(change)

    private fun watchesVariable(change: VariableChange): Boolean =
        if (settings.intAt(MODE) != 0) {
            change.boxId in settings.picks
        } else {
            settings.variableIds.firstOrNull() == change.variableId
        }

    private fun wantsOrigin(origin: Int): Boolean = settings.intAt(ORIGINS, -1) and (1 shl origin) != 0

    private fun wantsType(change: VariableChange): Boolean =
        when (change.type) {
            ChangeType.CREATED -> settings.intAt(CREATED) != 0
            ChangeType.DELETED -> settings.intAt(DELETED) != 0
            else -> settings.intAt(CHANGED) != 0 && settings.intAt(SUB_MASK) and direction(change) != 0
        }

    private fun direction(change: VariableChange): Int {
        val old = change.oldValue ?: 0
        val new = change.newValue ?: 0
        return when {
            new > old -> INCREASED
            new < old -> DECREASED
            else -> UNCHANGED
        }
    }

    private companion object {
        const val CREATED = 0
        const val CHANGED = 1
        const val DELETED = 2
        const val SUB_MASK = 3
        const val ORIGINS = 4
        const val MODE = 5
        const val INCREASED = 1
        const val DECREASED = 2
        const val UNCHANGED = 4
    }
}
