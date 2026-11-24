package zel.wired.signal

import zel.core.rules.rules
import zel.wired.RunMode
import zel.wired.WiredRooms
import zel.wired.box.BoxSettings
import zel.wired.box.EffectBox
import zel.wired.firing.Firing
import zel.wired.firing.FiringStart
import zel.wired.firing.Selection
import zel.wired.firing.Targets

class CallStacks(
    override val furniId: Int,
    override val settings: BoxSettings,
    override val negative: Boolean,
    private val rooms: () -> WiredRooms?,
) : EffectBox {
    override val needsUser = false

    override fun run(firing: Firing) {
        val wired = rooms() ?: return
        val own =
            firing.scope.room.furni[furniId]
                ?.anchor
        // The picked stacks are not charged as reached: their repeaters add nothing to the usage.
        val tiles =
            Targets.uncounted(firing) {
                Targets
                    .furni(firing, this)
                    .mapNotNull { it.anchor }
                    .distinct()
                    .filter { it != own }
                    .take(rules.limits.wiredCallTiles)
                    .toSet()
            }
        if (tiles.isEmpty()) return
        // A called stack starts with no selection and no context variables of its own; the triggering
        // user and furni are handed on.
        val start =
            FiringStart(
                firing.triggeringUnit,
                firing.triggeringFurni,
                firing.start.reachedUnit,
                firing.start.clicked,
                Selection(),
                firing.signal,
                LinkedHashMap(),
            )
        // A delayed call's stacks blink nothing; a delayed signal's receivers do.
        wired.runStacks(firing.scope, firing.event, start, RunMode.Call(negative), quiet = firing.delayed) { stacks ->
            stacks.filter { it.tile in tiles }.take(rules.limits.wiredCallStacks)
        }
    }
}
