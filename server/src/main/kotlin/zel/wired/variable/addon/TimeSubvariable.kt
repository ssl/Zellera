package zel.wired.variable.addon

import zel.room.Room
import zel.wired.firing.HeldValue
import zel.wired.variable.Availability
import zel.wired.variable.Holder
import zel.wired.variable.Variable
import zel.wired.variable.VariableEntry
import zel.wired.variable.VariableFlags
import zel.wired.variable.VariableOn
import java.time.ZoneId

class TimeSubvariable(
    override val entry: VariableEntry,
    private val parent: Variable,
    private val mode: Int,
    private val part: TimePart?,
    private val zone: (Room) -> ZoneId,
) : Variable {
    private fun millis(held: HeldValue): Long =
        when (mode) {
            MODE_VALUE -> held.value
            MODE_CREATED -> held.created
            else -> held.updated
        }

    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? =
        parent.read(on, holder)?.let { held ->
            val millis = millis(held)
            held.copy(value = part?.read(millis, zone(on.scope.room)) ?: millis)
        }

    override fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean {
        val written = part.takeIf { mode == MODE_VALUE } ?: return false
        val held = parent.read(on, holder) ?: return false
        return parent.write(on, holder, written.write(held.value, value, zone(on.scope.room)), origin)
    }

    override fun holders(on: VariableOn): List<Holder> = parent.holders(on)

    companion object {
        const val MODE_VALUE = 0
        const val MODE_CREATED = 1
        const val MODE_UPDATED = 2
        const val ID_SUB_TIME = 0
        private const val TYPE_SUBVARIABLE = 2
        private val READ_FLAGS = VariableFlags.of("001000000")
        private val WRITE_FLAGS = VariableFlags.of("001110000")

        /** Only times have a part 0 of their own. */
        fun of(
            parent: Variable,
            boxId: Int,
            ints: List<Int>,
            zone: (Room) -> ZoneId,
        ): List<Variable> {
            val mask = ints.getOrElse(0) { 0 }
            val mode = ints.getOrElse(1) { MODE_CREATED }.coerceIn(MODE_VALUE, MODE_UPDATED)
            val prefix =
                when (mode) {
                    MODE_VALUE -> "${parent.entry.name}.value"
                    MODE_CREATED -> "${parent.entry.name}.creation_time"
                    else -> "${parent.entry.name}.last_update_time"
                }

            fun sub(
                sub: Int,
                name: String,
                part: TimePart?,
            ): Variable {
                val flags = if (mode == MODE_VALUE) WRITE_FLAGS else READ_FLAGS
                val id = "s${parent.boxId}-$boxId-$sub\t$mode"
                val target = parent.entry.target
                val entry = VariableEntry(id, TYPE_SUBVARIABLE, name, Availability.EXECUTION, target, flags)
                return TimeSubvariable(entry, parent, mode, part, zone)
            }
            val parts = TimePart.picked(mask).map { sub(it.bit, "$prefix.${it.partName}", it) }
            return if (mode == MODE_VALUE) parts else listOf(sub(ID_SUB_TIME, prefix, null)) + parts
        }
    }
}
