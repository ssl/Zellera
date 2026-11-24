package zel.wired.link

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import zel.room.Room
import zel.wired.box.BoxSettings
import zel.wired.form.BoxValidation
import zel.wired.form.SaveOutcome

object FrameSaves {
    const val INVALID = "wiredfurni.error.link.invalid_dataframe"
    const val KEY_LENGTH = "wiredfurni.error.link.key_length"
    const val KEY_SYNTAX = "wiredfurni.error.variables.name_syntax"
    const val KEY_UNIQUE = "wiredfurni.error.variables.name_uniq"
    const val PLACEHOLDER_LENGTH = "wiredfurni.error.placeholder.name_length"
    const val PLACEHOLDER_SYNTAX = "wiredfurni.error.placeholder.name_syntax"
    const val PLACEHOLDER_RESERVED = "wiredfurni.error.placeholder.reserved"
    const val PLACEHOLDER_CONFLICT = "wiredfurni.error.link.destination_conflict"

    private const val MAX_ROWS = 12
    private const val MAX_NAME = 40
    private const val RESERVED = "trigger_text"
    private const val EMPTY = "[]"
    private val KEY = Regex("[A-Za-z0-9_ ]+")
    private val PLACEHOLDER = Regex("[A-Za-z0-9_]+")

    val validations: Map<String, BoxValidation> =
        mapOf(
            "wf_act_send_dataframe" to BoxValidation { _: Room, settings -> check(settings, placeholders = false) },
            "wf_trg_receive_dataframe" to BoxValidation { _: Room, settings -> check(settings, placeholders = true) },
        )

    fun sent(settings: BoxSettings): SaveOutcome = check(settings, placeholders = false)

    private fun check(
        settings: BoxSettings,
        placeholders: Boolean,
    ): SaveOutcome {
        val rows = rows(settings.text)
        val count = settings.ints.firstOrNull() ?: 0
        if (rows == null || rows.size != count || count > MAX_ROWS) return SaveOutcome.Invalid(INVALID)
        val keys = HashSet<String>()
        val targets = HashSet<String>()
        rows.forEach { (key, target) ->
            val refused = keyError(key.trim(), keys) ?: placeholderError(target, targets).takeIf { placeholders }
            if (refused != null) return SaveOutcome.Invalid(refused)
        }
        return SaveOutcome.Saved(
            when {
                rows.isEmpty() -> settings.copy(text = EMPTY)
                rows.any { (key, _) -> key != key.trim() } -> settings.copy(text = trimmed(rows))
                else -> settings
            },
        )
    }

    /** The rows again with their names trimmed, as the saved text. */
    private fun trimmed(rows: List<Pair<String, String>>): String {
        val all = JsonArray()
        rows.forEach { (key, target) ->
            val row = JsonArray()
            row.add(key.trim())
            row.add(target)
            all.add(row)
        }
        return all.toString()
    }

    private fun keyError(
        key: String,
        seen: MutableSet<String>,
    ): String? =
        when {
            key.isBlank() || key.length > MAX_NAME -> KEY_LENGTH
            !KEY.matches(key) -> KEY_SYNTAX
            !seen.add(key.lowercase()) -> KEY_UNIQUE
            else -> null
        }

    private fun placeholderError(
        name: String,
        seen: MutableSet<String>,
    ): String? =
        when {
            name.isEmpty() || name.length > MAX_NAME -> PLACEHOLDER_LENGTH
            !PLACEHOLDER.matches(name) -> PLACEHOLDER_SYNTAX
            name.equals(RESERVED, ignoreCase = true) -> PLACEHOLDER_RESERVED
            !seen.add(name.lowercase()) -> PLACEHOLDER_CONFLICT
            else -> null
        }

    private fun rows(text: String): List<Pair<String, String>>? =
        if (text.isEmpty()) {
            emptyList()
        } else {
            try {
                val parsed = JsonParser.parseString(text)
                if (parsed.isJsonArray) rowsOf(parsed.asJsonArray.toList()) else null
            } catch (_: JsonParseException) {
                null
            }
        }

    private fun rowsOf(elements: List<JsonElement>): List<Pair<String, String>>? {
        val rows =
            elements.map { element ->
                val cells =
                    element
                        .takeIf { it.isJsonArray }
                        ?.asJsonArray
                        ?.toList()
                        .orEmpty()
                val texts = cells.filter { it.isJsonPrimitive && it.asJsonPrimitive.isString }.map { it.asString }
                if (cells.size == 2 && texts.size == 2) texts[0] to texts[1] else null
            }
        return rows.filterNotNull().takeIf { it.size == rows.size }
    }
}
