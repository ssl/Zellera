package zel.wired.variable.addon

import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class ImportedAddons(
    val connectors: List<String> = emptyList(),
    val levels: List<CopiedBox> = emptyList(),
    val fx: List<CopiedBox> = emptyList(),
) {
    fun writeTo(
        keys: JsonObject,
        token: String,
    ) {
        section(keys, CONNECTORS).add(token, JsonArray().also { a -> connectors.forEach(a::add) })
        section(keys, LEVELS).add(token, boxes(levels))
        section(keys, FX).add(token, boxes(fx))
    }

    private fun section(
        keys: JsonObject,
        name: String,
    ): JsonObject = (keys.get(name) as? JsonObject) ?: JsonObject().also { keys.add(name, it) }

    private fun boxes(list: List<CopiedBox>): JsonArray =
        JsonArray().also { array ->
            list.forEach { box ->
                array.add(
                    JsonObject().apply {
                        addProperty(BASE, box.baseItemId)
                        add(INTS, JsonArray().also { a -> box.ints.forEach(a::add) })
                        addProperty(TEXT, box.text)
                        addProperty(BOX, box.boxId)
                    },
                )
            }
        }

    companion object {
        const val CONNECTORS = "textConnectors"
        const val LEVELS = "subvariableCreators"
        const val FX = "variableFx"
        private const val BASE = "baseItemId"
        private const val INTS = "ints"
        private const val TEXT = "text"
        private const val BOX = "boxId"

        fun read(keys: JsonObject?): ImportedAddons =
            ImportedAddons(
                entries(keys, CONNECTORS).flatMap { (it as? JsonArray)?.mapNotNull { e -> e.asString }.orEmpty() },
                boxes(keys, LEVELS),
                boxes(keys, FX),
            )

        private fun entries(
            keys: JsonObject?,
            name: String,
        ) = (keys?.get(name) as? JsonObject)?.entrySet()?.map { it.value }.orEmpty()

        private fun boxes(
            keys: JsonObject?,
            name: String,
        ): List<CopiedBox> =
            entries(keys, name).flatMap { list ->
                (list as? JsonArray)
                    ?.mapNotNull { element ->
                        (element as? JsonObject)?.let { box ->
                            CopiedBox(
                                box.get(BASE)?.asInt ?: 0,
                                box.get(BOX)?.asInt ?: 0,
                                (box.get(INTS) as? JsonArray)?.map { it.asInt }.orEmpty(),
                                box.get(TEXT)?.asString.orEmpty(),
                            )
                        }
                    }.orEmpty()
            }
    }
}
