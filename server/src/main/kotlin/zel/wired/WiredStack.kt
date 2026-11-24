package zel.wired

import zel.room.map.Point
import zel.wired.box.AddonBox
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.box.ConditionMode
import zel.wired.box.EffectBox
import zel.wired.box.SelectorBox
import zel.wired.box.TriggerBox
import zel.wired.box.VariableBox
import zel.wired.firing.Firing

class WiredStack(
    val trigger: TriggerBox,
    val tile: Point,
    val conditions: List<ConditionBox>,
    val effects: List<EffectBox>,
    val selectors: List<SelectorBox>,
    val addons: List<AddonBox>,
    val variables: List<VariableBox>,
    /** One per condition-mode add-on, with the conditions it applies to (picked ones may stand on other tiles). */
    val groups: List<ConditionGroup> = addons.filterIsInstance<ConditionMode>().map { ConditionGroup(it, conditions) },
    /** The OR add-ons on a pick loop: they and what they reach never blink. */
    val looped: Set<Int> = emptySet(),
    /** How many triggers the tile has; each makes a stack of its own over the same boxes. */
    val triggersOnTile: Int = 1,
) {
    val id: Int get() = trigger.furniId

    /** Every condition box a firing evaluates: the stack's own, then picked ones, also through a picked OR add-on. */
    val checked: List<ConditionBox> =
        (conditions + groups.flatMap { it.conditions }).flatMap(::leaves).distinctBy { it.furniId }

    /** The OR add-ons picked by another add-on's group; each blinks when its own result passes. */
    val nested: List<NestedGroup> =
        groups.flatMap { it.conditions }.flatMap(::nestedIn).distinctBy { it.furniId }

    /** The boxes a passing firing may blink for its conditions: each checked one, then each picked OR add-on. */
    val blinking: List<ConditionBox> = checked + nested
}

class ConditionGroup(
    val mode: ConditionMode,
    /** The picked conditions; a picked OR add-on stands here as one [NestedGroup]. */
    val conditions: List<ConditionBox>,
)

/** An OR add-on picked by another one: one condition there, its own mode over its own picked conditions. */
class NestedGroup(
    val mode: ConditionMode,
    val conditions: List<ConditionBox>,
) : ConditionBox {
    override val furniId: Int get() = mode.furniId
    override val settings: BoxSettings get() = mode.settings

    override fun passes(firing: Firing): Boolean = mode.passes(conditions.map { it.passes(firing) })

    /** Its result from the plain conditions' results of this firing, so none is evaluated twice. */
    fun passes(results: Map<Int, Boolean>): Boolean =
        mode.passes(
            conditions.map { if (it is NestedGroup) it.passes(results) else results.getValue(it.furniId) },
        )
}

/** The plain condition boxes behind [box]: itself, or every one a picked OR add-on reaches. */
fun leaves(box: ConditionBox): List<ConditionBox> =
    if (box is NestedGroup) box.conditions.flatMap(::leaves) else listOf(box)

private fun nestedIn(box: ConditionBox): List<NestedGroup> =
    if (box is NestedGroup) listOf(box) + box.conditions.flatMap(::nestedIn) else emptyList()
