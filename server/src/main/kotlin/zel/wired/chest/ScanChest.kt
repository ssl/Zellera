package zel.wired.chest

import zel.wired.box.BoxSettings
import zel.wired.box.SelectorBox
import zel.wired.firing.Firing
import zel.wired.firing.Selection
import zel.wired.firing.Targets
import zel.wired.variable.ChangeOrigin
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.VariableOn

internal class ScanChest(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val chests: RoomChests,
    private val variables: RoomVariables,
) : SelectorBox {
    override fun select(
        firing: Firing,
        selection: Selection,
    ) {
        val types = Targets.furni(firing, this, TYPES, picksForTriggering = false).map { it.definition.id }.toSet()
        val count =
            Targets
                .furni(firing, this, CHESTS)
                .mapNotNull { chests.chest(it) }
                .filter { it.wiredEnabled && !it.isCoins }
                .sumOf { chest ->
                    val scanned =
                        if (settings.intAt(0) ==
                            PREVIEWED
                        ) {
                            chest.furni.take(chest.preferences.previewAmount)
                        } else {
                            chest.furni
                        }
                    scanned.count { types.isEmpty() || it.definitionId in types }
                }
        val variable = variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return
        variable.give(VariableOn(firing.scope, firing), Holder.Context, count.toLong(), true, ChangeOrigin.IN_ROOM)
    }

    private companion object {
        const val TYPES = 0
        const val CHESTS = 1
        const val PREVIEWED = 1
    }
}
