package zel.wired.signal

import com.google.gson.JsonArray
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

object FrameText {
    fun pairs(text: String): List<Pair<String, String>> =
        try {
            val parsed = if (text.isBlank()) null else JsonParser.parseString(text)
            val rows = parsed?.takeIf { it.isJsonArray }?.asJsonArray
            rows?.mapNotNull { row -> pair(row.takeIf { it.isJsonArray }?.asJsonArray) }.orEmpty()
        } catch (_: JsonParseException) {
            emptyList()
        }

    /** The rows with each second cell changed; an unreadable or empty text as it is. */
    fun withMessages(
        text: String,
        change: (String) -> String,
    ): String {
        val rows = pairs(text)
        if (rows.isEmpty()) return text
        val out = JsonArray()
        rows.forEach { (name, message) ->
            out.add(
                JsonArray().also {
                    it.add(name)
                    it.add(change(message))
                },
            )
        }
        return out.toString()
    }

    private fun pair(row: JsonArray?): Pair<String, String>? {
        val first = row?.firstOrNull()?.takeIf { it.isJsonPrimitive } ?: return null
        val second =
            row
                .drop(1)
                .firstOrNull()
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
                .orEmpty()
        return first.asString to second
    }
}
