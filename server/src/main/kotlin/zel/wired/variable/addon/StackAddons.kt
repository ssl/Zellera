package zel.wired.variable.addon

import zel.room.Room
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.util.GameZone
import zel.wired.BoxSettingsSource
import zel.wired.variable.RoomVariables
import zel.wired.variable.ValueTexts
import zel.wired.variable.Variable
import zel.wired.variable.VariableAddons
import java.time.ZoneId

class StackAddons(
    private val forms: BoxSettingsSource,
    private val copies: ImportCopies,
    timezone: (Room) -> String,
) : VariableAddons {
    private val zone: (Room) -> ZoneId = { room ->
        runCatching { ZoneId.of(timezone(room)) }.getOrDefault(GameZone.current)
    }

    override fun texts(
        room: Room,
        box: RoomFurni,
    ): ValueTexts? {
        val own = onTile(room, box, AddonCodes.TEXT_CONNECTOR).mapNotNull { forms.settings(room, it)?.text }
        val imported =
            if (Interactions.of(box.definition) == RoomVariables.REFERENCE_BOX) {
                imports(room, box, SHARED_CONNECTORS).flatMap { copies.of(room, it).connectors }
            } else {
                emptyList()
            }
        return TextMappings.table(own + imported)
    }

    override fun levels(
        room: Room,
        box: RoomFurni,
    ): ((Long) -> Int)? = levelTable(room, box)?.let { table -> { value -> table.read(value).level } }

    override fun subvariables(
        room: Room,
        variable: Variable,
        box: RoomFurni,
    ): List<Variable> {
        val times =
            onTile(room, box, AddonCodes.TIME_UTIL).flatMap { util ->
                forms.settings(room, util)?.let { TimeSubvariable.of(variable, util.id, it.ints, zone) }.orEmpty()
            }
        val levels =
            onTile(room, box, AddonCodes.LEVEL_UP).flatMap { level ->
                forms
                    .settings(room, level)
                    ?.let { s ->
                        LevelSubvariable.of(variable, "s${box.id}-${level.id}", s.ints, s.text)
                    }.orEmpty()
            }
        return times + levels + importedLevels(room, variable, box)
    }

    private fun importedLevels(
        room: Room,
        variable: Variable,
        box: RoomFurni,
    ): List<Variable> {
        if (Interactions.of(box.definition) != RoomVariables.REFERENCE_BOX) return emptyList()
        val sourceRoom =
            forms
                .settings(room, box)
                ?.variableIds
                ?.firstOrNull()
                ?.split(':')
                ?.getOrNull(2)
        return imports(room, box, SHARED_LEVELS).flatMap { import ->
            copies.of(room, import).levels.flatMap { copy ->
                LevelSubvariable.of(variable, "s${box.id}-r$sourceRoom-${copy.boxId}", copy.ints, copy.text)
            }
        }
    }

    fun levelTable(
        room: Room,
        tileOf: RoomFurni,
    ): LevelTable? =
        onTile(room, tileOf, AddonCodes.LEVEL_UP).firstNotNullOfOrNull { level ->
            forms.settings(room, level)?.let { LevelTable.of(it.ints, it.text) }
        }

    fun imports(
        room: Room,
        box: RoomFurni,
        category: Int,
    ): List<RoomFurni> =
        onTile(room, box, AddonCodes.IMPORT).filter { import ->
            forms.settings(room, import)?.ints?.getOrElse(category) { 1 } != 0
        }

    fun onTile(
        room: Room,
        box: RoomFurni,
        code: String,
    ): List<RoomFurni> {
        val tile = box.anchor ?: return emptyList()
        return room.furni
            .at(tile)
            .furni
            .filter { Interactions.of(it.definition) == code }
            .sortedWith(compareBy({ it.z }, { it.id }))
    }

    companion object {
        const val SHARED_CONNECTORS = 0
        const val SHARED_LEVELS = 1
        const val SHARED_FX = 2
    }
}
