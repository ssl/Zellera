package zel.wired.variable.box

import zel.util.MILLIS_PER_DAY
import zel.util.MILLIS_PER_HOUR
import zel.util.MILLIS_PER_MINUTE
import zel.util.MILLIS_PER_SECOND
import zel.util.MILLIS_PER_WEEK
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.HeldValue
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.Variable
import zel.wired.variable.VariableOn
import zel.wired.variable.VariableTarget

class VariableAgeMatch(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val variables: RoomVariables,
    private val clock: () -> Long,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val target = settings.intAt(TARGET)
        if (BoxHolders.namesNoVariable(this)) return true
        val variable = variables.find(firing.scope.room, settings.variableIds.firstOrNull().orEmpty()) ?: return false
        if (variable.boxId == 0) return false
        val on = VariableOn(firing.scope, firing)
        val holders = BoxHolders.of(firing, this, target)
        if (holders.isEmpty()) return BoxHolders.nonePass(this, target)
        val amount = amount(firing, on) ?: return false
        val results =
            holders.map { holder ->
                (variable.read(on, holder) ?: sinceLoad(firing, variable, holder))?.let {
                    matches(variable, it, amount)
                } == true
            }
        return if (settings.quantifier == ANY) results.any { it } else results.all { it }
    }

    private fun amount(
        firing: Firing,
        on: VariableOn,
    ): Long? {
        val units =
            if (settings.intAt(FROM_VARIABLE) != 0) {
                val reference =
                    variables.find(firing.scope.room, settings.variableIds.getOrNull(1).orEmpty()) ?: return null
                val holder = BoxHolders.of(firing, this, settings.intAt(REFERENCE_TARGET), 1).firstOrNull()
                holder?.let { reference.read(on, it)?.value } ?: return null
            } else {
                longOf(settings.intAt(HIGH), settings.intAt(LOW))
            }
        val millis = unitMillis(settings.intAt(UNIT))
        // an age past what a long holds counts as the longest one, not as a wrapped negative
        return try {
            Math.multiplyExact(units, millis)
        } catch (_: ArithmeticException) {
            if ((units < 0) == (millis < 0)) Long.MAX_VALUE else Long.MIN_VALUE
        }
    }

    /** A room variable without a value counts as last changed at the room's load. */
    private fun sinceLoad(
        firing: Firing,
        variable: Variable,
        holder: Holder,
    ): HeldValue? {
        if (holder != Holder.Room || variable.entry.target != VariableTarget.GLOBAL) return null
        val loaded = variables.space(firing.scope.room).startedAt
        return HeldValue(0, loaded, loaded)
    }

    private fun matches(
        variable: Variable,
        held: HeldValue,
        amount: Long,
    ): Boolean {
        val created = settings.intAt(MODE) == CREATED
        if (created && variable.entry.target == VariableTarget.GLOBAL) return false
        val age = clock() - if (created) held.created else held.updated
        return when (settings.intAt(COMPARISON)) {
            YOUNGER -> age < amount
            OLDER -> age > amount
            else -> false
        }
    }

    private fun unitMillis(unit: Int): Long = UNITS.getOrElse(unit) { 1L }

    private companion object {
        const val ANY = 1
        const val TARGET = 0
        const val COMPARISON = 1
        const val MODE = 2
        const val HIGH = 3
        const val LOW = 4
        const val UNIT = 5
        const val FROM_VARIABLE = 6
        const val REFERENCE_TARGET = 7
        const val CREATED = 0
        const val YOUNGER = 0
        const val OLDER = 2
        const val MONTH = 30 * MILLIS_PER_DAY
        const val YEAR = 365 * MILLIS_PER_DAY
        val UNITS =
            listOf(
                1L,
                MILLIS_PER_SECOND,
                MILLIS_PER_MINUTE,
                MILLIS_PER_HOUR,
                MILLIS_PER_DAY,
                MILLIS_PER_WEEK,
                MONTH,
                YEAR,
            )
    }
}
