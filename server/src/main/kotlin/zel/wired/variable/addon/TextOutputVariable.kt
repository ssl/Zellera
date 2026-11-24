package zel.wired.variable.addon

import zel.wired.addon.PlaceholderBox
import zel.wired.addon.PlaceholderText
import zel.wired.addon.Placeholders
import zel.wired.box.BoxSettings
import zel.wired.firing.Firing
import zel.wired.firing.Targets
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.Variable
import zel.wired.variable.VariableOn
import zel.wired.variable.VariableTarget

class TextOutputVariable(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val variables: RoomVariables,
) : PlaceholderBox {
    private val parts = settings.text.split('\t')
    private val multiple = settings.ints.getOrElse(0) { 0 } != 0
    private val target = settings.ints.getOrElse(TARGET) { VariableTarget.USER }
    private val asText = settings.ints.getOrElse(AS_TEXT) { 0 } != 0

    override val name: String = parts.first().removeSurrounding("$(", ")")

    /** A save with show multiple on needs a delimiter; ", " only serves boxes stored without one (imports). */
    private val delimiter =
        parts.getOrNull(1)?.take(PlaceholderText.MAX_DELIMITER)?.takeIf { it.isNotEmpty() } ?: DEFAULT_DELIMITER

    /**
     * Show multiple on joins what the holders print, "null" for a holder that lacks the variable: no holder at all
     * leaves an empty text. Off, no holder leaves the `$(name)` literal, the first holder with a value prints it and
     * holders that all lack it print "null". A variable that does not exist leaves the literal; a room variable read
     * for users or furni counts as missing too.
     */
    override fun text(firing: Firing): String? {
        val room = firing.scope.room
        val variable = variables.find(room, settings.variableIds.firstOrNull().orEmpty()) ?: return null
        if (variable.entry.target == VariableTarget.GLOBAL && target != VariableTarget.GLOBAL) return null
        val on = VariableOn(firing.scope, firing)
        val holders = holders(firing)
        if (multiple) {
            val all = holders.asSequence().map { holder -> print(variable, on, holder) ?: NOTHING_HELD }
            return if (holders.isEmpty()) "" else Placeholders.join(all, delimiter) ?: NOTHING_HELD
        }
        if (holders.isEmpty()) return null
        return holders.asSequence().mapNotNull { holder -> print(variable, on, holder) }.firstOrNull() ?: NOTHING_HELD
    }

    override val blinksAfterEffects: Boolean get() = true

    private fun print(
        variable: Variable,
        on: VariableOn,
        holder: Holder,
    ): String? =
        // a variable without a value (is_maxed) prints like one not held
        variable.read(on, holder)?.takeIf { variable.entry.flags.hasValue }?.value?.let { value ->
            if (asText) variable.entry.textOf(value) else value.toString()
        }

    private fun holders(firing: Firing): List<Holder> =
        when (target) {
            VariableTarget.FURNI -> Targets.furni(firing, this).map { Holder.of(it) }
            VariableTarget.USER -> Targets.units(firing, this).map { Holder.of(it) }
            VariableTarget.GLOBAL -> listOf(Holder.Room)
            VariableTarget.CONTEXT -> listOf(Holder.Context)
            else -> emptyList()
        }

    private companion object {
        const val TARGET = 1
        const val AS_TEXT = 2
        const val DEFAULT_DELIMITER = ", "
        const val NOTHING_HELD = "null"
    }
}
