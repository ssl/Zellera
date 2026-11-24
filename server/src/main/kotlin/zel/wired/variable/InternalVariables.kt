package zel.wired.variable

import zel.content.ClientStrings
import zel.game.pets.HorsePet
import zel.game.pets.MonsterPlant
import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class InternalVariables(
    val values: InternalValues,
    private val writes: InternalWrites,
    private val texts: ValueTables = ValueTables.None,
) {
    private val rows: List<VariableEntry> =
        InternalTable.ROWS
            .lines()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(::row)

    private val tables: Map<String, ValueTexts> =
        InternalTable.TEXTS
            .lines()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .groupBy({ it.substringBefore(' ') }) { line ->
                val rest = line.substringAfter(' ')
                rest.substringBefore(' ').toInt() to rest.substringAfter(' ')
            }.mapValues { ValueTexts(it.value) }

    private val byId: Map<String, VariableEntry> = rows.associateBy { it.id }

    private fun row(line: String): VariableEntry {
        val parts = line.split(' ', limit = PARTS)
        return VariableEntry(
            parts[ID],
            parts[TYPE].toInt(),
            parts[NAME],
            Availability.EXECUTION,
            parts[TARGET].toInt(),
            VariableFlags.of(parts[FLAGS]),
        )
    }

    fun byId(
        room: Room,
        id: String,
    ): Variable? = byId[id]?.let(::named)?.let { view(it, kinds(room)) }

    fun listed(room: Room): List<Variable> {
        val kinds = kinds(room)
        return rows.mapNotNull(::named).map { view(it, kinds) }
    }

    /** A row named by the client profile exists only while the profile names it. */
    private fun named(row: VariableEntry): VariableEntry? {
        if (!row.name.startsWith(PROFILE_NAME)) return row
        return ClientStrings[row.name.removePrefix(PROFILE_NAME)]?.let { row.copy(name = it) }
    }

    private fun view(
        row: VariableEntry,
        kinds: Set<String>,
    ): Variable {
        val visible = row.id.startsWith('~') && kinds.any { row.name.startsWith(it) }
        val entry =
            row.copy(
                flags = if (visible) row.flags.copy(isInvisible = false) else row.flags,
                texts = tables[row.id] ?: texts.of(row.id),
            )
        return InternalVariable(entry, values, writes)
    }

    private fun kinds(room: Room): Set<String> {
        val furni = room.furni.all().mapNotNullTo(HashSet()) { kindOf(it) }
        val pets =
            room.units
                .all()
                .flatMap { petKinds(room, it) }
        return furni + pets
    }

    private fun kindOf(furni: RoomFurni): String? {
        val code = Interactions.of(furni.definition)
        return when {
            code == "room.toner" -> "~background_color."
            code == "transit.door" || code == "transit.tile" -> "~teleport."
            code == "tile.walk" -> "~walk_helper."
            code == "area.hide" -> "~area_hide."
            code == "state.recolor" -> "~recolorable_furni."
            code.startsWith("wf_storage_") -> "~chest."
            values.facts.clock(furni) != null -> "~clock."
            else -> null
        }
    }

    private fun petKinds(
        room: Room,
        unit: RoomUnit,
    ): List<String> {
        if (unit.kind !is UnitKind.Pet) return emptyList()
        val pet = values.facts.pet(room, unit) ?: return emptyList()
        return when (pet) {
            is MonsterPlant -> listOf(PET_KIND, "~plant.")
            is HorsePet -> listOf(PET_KIND, "~horse.")
            else -> listOf(PET_KIND)
        }
    }

    private companion object {
        const val PARTS = 5
        const val ID = 0
        const val TYPE = 1
        const val TARGET = 2
        const val FLAGS = 3
        const val NAME = 4
        const val PROFILE_NAME = "profile:"
        const val PET_KIND = "~pet."
    }
}
