package zel.wired.variable.addon

import zel.wired.firing.HeldValue
import zel.wired.variable.Availability
import zel.wired.variable.Holder
import zel.wired.variable.Variable
import zel.wired.variable.VariableEntry
import zel.wired.variable.VariableFlags
import zel.wired.variable.VariableOn

class LevelSubvariable(
    override val entry: VariableEntry,
    private val parent: Variable,
    private val table: LevelTable,
    private val part: LevelPart,
) : Variable {
    override fun read(
        on: VariableOn,
        holder: Holder,
    ): HeldValue? {
        val held = parent.read(on, holder) ?: return null
        val progress = table.read(held.value)
        if (part == LevelPart.IS_MAXED && !progress.maxed) return null
        return held.copy(value = part.of(progress))
    }

    /** Only the current xp takes a write, as the variable's own value. */
    override fun write(
        on: VariableOn,
        holder: Holder,
        value: Long,
        origin: Int,
    ): Boolean = part == LevelPart.CURRENT_XP && parent.write(on, holder, value, origin)

    override fun holders(on: VariableOn): List<Holder> = parent.holders(on).filter { read(on, it) != null }

    companion object {
        private const val TYPE_SUBVARIABLE = 2
        private val VALUE_FLAGS = VariableFlags.of("001010000")
        private val FLAG_FLAGS = VariableFlags.of("000010000")
        private val WRITABLE_FLAGS = VariableFlags.of("001110000")

        fun of(
            parent: Variable,
            idPrefix: String,
            ints: List<Int>,
            text: String,
        ): List<Variable> {
            val mask = ints.getOrElse(0) { 0 }
            val table = LevelTable.of(ints, text)
            return LevelPart.entries.filter { mask and (1 shl it.bit) != 0 }.map { part ->
                val flags =
                    when (part) {
                        LevelPart.IS_MAXED -> FLAG_FLAGS
                        LevelPart.CURRENT_XP -> WRITABLE_FLAGS
                        else -> VALUE_FLAGS
                    }
                val entry =
                    VariableEntry(
                        "$idPrefix-${part.bit}",
                        TYPE_SUBVARIABLE,
                        "${parent.entry.name}.${part.partName}",
                        Availability.EXECUTION,
                        parent.entry.target,
                        flags,
                    )
                LevelSubvariable(entry, parent, table, part)
            }
        }
    }
}
