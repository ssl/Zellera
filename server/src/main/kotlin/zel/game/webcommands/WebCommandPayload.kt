package zel.game.webcommands

import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal class WebCommandPayload private constructor(
    private val json: JsonObject,
) {
    @Suppress("ReturnCount") // each malformed shape reads as absent
    fun int(name: String): Int? {
        val value = json.get(name)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) return null
        val text = value.asString
        if (!WHOLE_NUMBER.matches(text)) return null
        return text.toLong().takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
    }

    fun string(name: String): String? {
        val value = json.get(name)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) return null
        return value.asString
    }

    fun bool(name: String): Boolean? {
        val value = json.get(name)
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) return null
        return value.asBoolean
    }

    companion object {
        private val WHOLE_NUMBER = Regex("-?\\d{1,10}")

        @Suppress("TooGenericExceptionCaught") // the parser throws unchecked exceptions of its own for bad text
        fun parse(text: String?): WebCommandPayload? =
            try {
                val element = JsonParser.parseString(text.orEmpty())
                if (element.isJsonObject) WebCommandPayload(element.asJsonObject) else null
            } catch (_: RuntimeException) {
                null
            }
    }
}
