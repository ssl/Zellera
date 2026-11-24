package zel.wired.variable.addon

import zel.room.Room
import zel.room.furni.place.Interactions
import zel.wired.addon.PlaceholderText
import zel.wired.box.BoxSettings
import zel.wired.form.BoxValidation
import zel.wired.form.SaveChecks
import zel.wired.form.SaveOutcome
import zel.wired.form.SaveRequest
import zel.wired.variable.RoomVariables

class VariableAddonRules(
    private val variables: () -> RoomVariables,
    private val fx: VariableFx,
    private val webApi: WebApiBoxes,
) {
    val validations: Map<String, BoxValidation> =
        buildMap {
            put(AddonCodes.SHARE, BoxValidation { _, settings -> SaveOutcome.Saved(ticks(settings).copy(text = "")) })
            put(AddonCodes.IMPORT, BoxValidation { _, settings -> SaveOutcome.Saved(ticks(settings).copy(text = "")) })
            put(AddonCodes.TEXT_CONNECTOR, BoxValidation { _, settings -> connector(settings) })
            put(AddonCodes.LEVEL_UP, BoxValidation { _, settings -> levelUp(settings) })
            put(AddonCodes.TEXT_OUTPUT, BoxValidation { room, settings -> textOutput(room, settings) })
            put(AddonCodes.TEXT_INPUT, BoxValidation { room, settings -> textInput(room, settings) })
            put(AddonCodes.WEB_API, WebApi())
            FxKind.entries.forEach { put(it.code, Fx(it)) }
        }

    private fun ticks(settings: BoxSettings): BoxSettings =
        settings.copy(ints = settings.ints.map { if (it != 0) 1 else 0 })

    private fun connector(settings: BoxSettings): SaveOutcome =
        if (settings.text.length > MAX_CONNECTOR_TEXT) {
            SaveOutcome.Invalid(SaveChecks.TOO_MUCH_TEXT)
        } else {
            SaveOutcome.Saved(settings)
        }

    private fun levelUp(settings: BoxSettings): SaveOutcome {
        val ints = settings.ints

        fun int(i: Int) = ints.getOrElse(i) { 0 }
        val mode = int(1)
        return when (mode) {
            LevelTable.MANUAL -> {
                val table = ManualLines.parse(settings.text)
                if (table.refused) {
                    SaveOutcome.Report
                } else {
                    SaveOutcome.Saved(settings.copy(ints = onlyMode(int(0), mode), text = table.text))
                }
            }

            LevelTable.LINEAR -> {
                SaveOutcome.Saved(settings.copy(ints = listOf(int(0), mode, int(2), int(LINEAR_LAST), 0)))
            }

            LevelTable.EXPONENTIAL -> {
                SaveOutcome.Saved(settings)
            }

            else -> {
                SaveOutcome.Saved(settings.copy(ints = onlyMode(int(0), mode)))
            }
        }
    }

    private fun onlyMode(
        mask: Int,
        mode: Int,
    ) = listOf(mask, mode, 0, 0, 0)

    private fun nameProblem(name: String): SaveOutcome.Invalid? =
        when {
            name.isEmpty() || name.length > MAX_NAME -> SaveOutcome.Invalid(NAME_LENGTH)
            !NAME.matches(name) -> SaveOutcome.Invalid(NAME_SYNTAX)
            name.equals(RESERVED_NAME, ignoreCase = true) -> SaveOutcome.Invalid(RESERVED)
            else -> null
        }

    @Suppress("ReturnCount") // one early answer per missing part
    private fun textOutput(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome {
        val parts = settings.text.split('\t')
        nameProblem(parts.first())?.let { return it }
        val multiple = settings.ints.getOrElse(0) { 0 } != 0
        if (multiple && parts.getOrNull(1).isNullOrEmpty()) return SaveOutcome.Invalid(DELIMITER_EMPTY)
        val long = (parts.getOrNull(1)?.length ?: 0) > PlaceholderText.MAX_DELIMITER
        if (long) return SaveOutcome.Invalid(DELIMITER_LENGTH)
        val delimiter = parts.getOrNull(1)?.let { "\t" + it }.orEmpty()
        val saved = settings.copy(text = parts.first() + delimiter)
        val id = settings.variableIds.firstOrNull().orEmpty()
        if (id.isEmpty() || id == RoomVariables.NONE) return SaveOutcome.Saved(saved)
        val entry = variables().find(room, id)?.entry
        val fits = entry?.flags?.hasValue == true
        return if (fits) SaveOutcome.Saved(saved) else SaveOutcome.Invalid(INVALID_VARIABLE)
    }

    private fun textInput(
        room: Room,
        settings: BoxSettings,
    ): SaveOutcome {
        nameProblem(settings.text)?.let { return it }
        val id = settings.variableIds.firstOrNull().orEmpty()
        if (id.isEmpty() || id == RoomVariables.NONE) return SaveOutcome.Saved(settings)
        val entry = variables().find(room, id)?.entry
        val fits = entry != null && entry.flags.hasValue
        return if (fits) SaveOutcome.Saved(settings) else SaveOutcome.Invalid(INVALID_VARIABLE)
    }

    private inner class WebApi : BoxValidation {
        override fun check(
            room: Room,
            settings: BoxSettings,
        ): SaveOutcome = SaveOutcome.Saved(settings)

        override fun check(
            room: Room,
            settings: BoxSettings,
            request: SaveRequest,
        ): SaveOutcome {
            val box =
                room.furni[request.boxId]?.takeIf { Interactions.of(it.definition) == AddonCodes.WEB_API }
                    ?: return SaveOutcome.Saved(settings)
            return if (webApi.apply(room, box, request.text)) {
                val bulk = request.ints.getOrElse(0) { 0 } != 0 && webApi.hasWriteKey(box)
                SaveOutcome.Saved(settings.copy(ints = listOf(if (bulk) 1 else 0), text = ""))
            } else {
                SaveOutcome.Invalid(INVALID_KEYS)
            }
        }
    }

    /** The room hears of the config before the save is judged: its range is checked last. */
    private inner class Fx(
        private val kind: FxKind,
    ) : BoxValidation {
        override fun check(
            room: Room,
            settings: BoxSettings,
        ): SaveOutcome = SaveOutcome.Saved(settings)

        override fun check(
            room: Room,
            settings: BoxSettings,
            request: SaveRequest,
        ): SaveOutcome {
            val form = FxForm(request.ints)
            val refused = kind.hasRange && form.max <= form.min
            room.furni[request.boxId]?.let { box ->
                fx.saved(room, box, FxConfig.of(box.id, kind, request.ints), accepted = !refused)
            }
            if (refused) return SaveOutcome.Invalid(RANGE)
            val filled = FxConfig.filled(kind, settings)
            return SaveOutcome.Saved(filled.copy(variableIds = threeVariables(settings.variableIds)))
        }

        private fun threeVariables(ids: List<String>): List<String> =
            List(VARIABLE_SLOTS) { ids.getOrElse(it) { RoomVariables.NONE } }
    }

    companion object {
        const val NAME_LENGTH = "wiredfurni.error.placeholder.name_length"
        const val NAME_SYNTAX = "wiredfurni.error.placeholder.name_syntax"
        const val RESERVED = "wiredfurni.error.placeholder.reserved"
        const val DELIMITER_EMPTY = "wiredfurni.error.placeholder.delimiter_empty"
        const val DELIMITER_LENGTH = "wiredfurni.error.placeholder.delimiter_length"
        const val INVALID_VARIABLE = "wiredfurni.params.variables.validation.invalid_variable"
        const val INVALID_KEYS = "wiredfurni.error.invalid_api_keys"
        const val RANGE = "wiredfurni.params.variablefx.validation.range"
        private const val RESERVED_NAME = "trigger_text"
        private const val MAX_NAME = 40
        private const val MAX_CONNECTOR_TEXT = 1000
        private const val VARIABLE_SLOTS = 3
        private const val LINEAR_LAST = 3
        private val NAME = Regex("[A-Za-z0-9_]+")
    }
}
