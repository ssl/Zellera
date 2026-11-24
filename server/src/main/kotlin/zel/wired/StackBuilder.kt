package zel.wired

import zel.room.Room
import zel.room.furni.place.Interactions
import zel.room.map.Point
import zel.wired.box.AddonBox
import zel.wired.box.BoxCatalog
import zel.wired.box.ConditionBox
import zel.wired.box.ConditionMode
import zel.wired.box.EffectBox
import zel.wired.box.NoTrigger
import zel.wired.box.SelectorBox
import zel.wired.box.TriggerBox
import zel.wired.box.VariableBox

class StackBuilder(
    private val catalog: BoxCatalog,
    private val settings: BoxSettingsSource,
) {
    /** Reuses boxes whose code, tile, height and form stayed, and unchanged stacks. */
    fun build(
        room: Room,
        wired: RoomWired,
    ): List<WiredStack> {
        val boxFurni = room.furni.selecting(this) { catalog[Interactions.of(it.definition)] != null }
        val placed = wired.placed
        val now = HashMap<Int, PlacedBox>()
        var changed = false
        boxFurni.forEach { furni ->
            val code = Interactions.of(furni.definition)
            val type = catalog[code]
            val tile = furni.anchor
            if (type != null && tile != null) {
                val form = settings.settings(room, furni) ?: type.form.fresh
                val kept = placed[furni.id]?.takeIf { it.isSame(code, tile, furni.z, form) }
                if (kept == null) changed = true
                now[furni.id] = kept ?: PlacedBox(code, type.kind, tile, furni.z, type.create(furni.id, form))
            }
        }
        if (!changed && now.size == placed.size) return wired.stacks
        placed.clear()
        placed.putAll(now)
        wired.stacks =
            now.values
                .groupBy { it.tile }
                .flatMap { (tile, boxes) -> stacksOn(tile, boxes.sortedWith(STACK_ORDER), now) }
                .sortedBy { it.id }
        return wired.stacks
    }

    private fun stacksOn(
        tile: Point,
        boxes: List<PlacedBox>,
        all: Map<Int, PlacedBox>,
    ): List<WiredStack> {
        val effects = boxes.mapNotNull { it.box as? EffectBox }
        // Effects with no trigger still make a stack, one only a call runs; its id is the bottom box.
        val triggers = boxes.mapNotNull { it.box as? TriggerBox }.ifEmpty { listOfNotNull(none(boxes, effects)) }
        if (triggers.isEmpty()) return emptyList()
        val conditions = boxes.mapNotNull { it.box as? ConditionBox }
        val selectors = boxes.mapNotNull { it.box as? SelectorBox }
        val addons = boxes.mapNotNull { it.box as? AddonBox }
        val variables = boxes.mapNotNull { it.box as? VariableBox }
        val modes = addons.filterIsInstance<ConditionMode>()
        val looped = HashSet<Int>()
        val picked =
            modes.associateWith { mode ->
                mode.appliesTo.mapNotNull { picked(it, listOf(mode.furniId), all, looped) }
            }
        // An add-on without picks takes the stack's conditions that no other add-on picked.
        val claimed =
            picked.values
                .flatten()
                .flatMap(::leaves)
                .mapTo(HashSet()) { it.furniId }
        val groups =
            modes.map { mode ->
                val unclaimed = conditions.filter { it.furniId !in claimed }
                ConditionGroup(mode, if (mode.appliesTo.isEmpty()) unclaimed else picked.getValue(mode))
            }
        return triggers.map { trigger ->
            WiredStack(trigger, tile, conditions, effects, selectors, addons, variables, groups, looped, triggers.size)
        }
    }

    /**
     * A picked condition box, or a picked OR add-on as one condition: its own mode over its own picked conditions,
     * on any tile. A pick that loops back, an add-on whose picks resolve to nothing or
     * one without picks, and any other furni drop out; the add-ons on a loop go into [looped].
     */
    private fun picked(
        id: Int,
        path: List<Int>,
        all: Map<Int, PlacedBox>,
        looped: MutableSet<Int>,
    ): ConditionBox? {
        val box = all[id]?.box
        if (box is ConditionBox) return box
        if (box !is ConditionMode) return null
        if (id in path) {
            looped += path.subList(path.indexOf(id), path.size)
            return null
        }
        val inner = box.appliesTo.mapNotNull { picked(it, path + id, all, looped) }
        return if (inner.isEmpty()) null else NestedGroup(box, inner)
    }

    private fun none(
        boxes: List<PlacedBox>,
        effects: List<EffectBox>,
    ): TriggerBox? = if (effects.isEmpty()) null else NoTrigger(boxes.first().box.furniId)

    private companion object {
        val STACK_ORDER: Comparator<PlacedBox> = compareBy<PlacedBox> { it.z }.thenBy { it.box.furniId }
    }
}
