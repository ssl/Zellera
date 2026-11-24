package zel.game.items

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

/** A furni's structured state: one JSON object in the row's state column, absent fields read as empty. */
class StateFields private constructor(
    private val json: JsonObject,
) {
    fun text(name: String): String =
        json
            .get(name)
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            .orEmpty()

    fun number(name: String): Int = text(name).toIntOrNull() ?: 0

    fun flag(name: String): Boolean = text(name) == "true"

    fun numbers(name: String): List<Int> =
        (json.get(name) as? JsonArray)
            ?.mapNotNull { element -> element.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull() }
            .orEmpty()

    class Writer {
        private val json = JsonObject()

        fun text(
            name: String,
            value: String,
        ) = apply { json.addProperty(name, value) }

        fun number(
            name: String,
            value: Int,
        ) = apply { json.addProperty(name, value) }

        fun flag(
            name: String,
            value: Boolean,
        ) = apply { json.addProperty(name, value) }

        fun numbers(
            name: String,
            values: Collection<Int>,
        ) = apply { json.add(name, JsonArray().also { array -> values.forEach(array::add) }) }

        fun state(): String = json.toString()
    }

    companion object {
        /** Null for a plain state such as "", "0" or a number. */
        fun read(state: String): StateFields? {
            if (!state.startsWith("{")) return null
            return try {
                (JsonParser.parseString(state) as? JsonObject)?.let(::StateFields)
            } catch (_: JsonParseException) {
                null
            }
        }

        fun write(): Writer = Writer()
    }
}
