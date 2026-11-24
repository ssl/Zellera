package zel.game.catalog.layouts

import zel.game.catalog.CatalogPage

/**
 * Reads a slot list such as `header,teaser~;,-` into what a page writes. A name is a page field (header, teaser,
 * special, one, two, details), `-` an empty string, and `name~sep` the field cut at `sep`.
 */
internal object PageSlots {
    private val images: Map<String, (CatalogPage) -> String> =
        mapOf(
            "header" to { it.info.images.header },
            "teaser" to { it.info.images.teaser },
            "special" to { it.info.images.special },
        )

    private val texts: Map<String, (CatalogPage) -> String> =
        mapOf(
            "one" to { it.info.wording.one },
            "two" to { it.info.wording.two },
            "details" to { it.info.wording.details },
            "teaser" to { it.info.wording.teaser },
        )

    fun images(spec: String): (CatalogPage) -> List<String> = read(spec, images)

    fun texts(spec: String): (CatalogPage) -> List<String> = read(spec, texts)

    private fun read(
        spec: String,
        fields: Map<String, (CatalogPage) -> String>,
    ): (CatalogPage) -> List<String> {
        val parts =
            spec
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map { slot ->
                    val name = slot.substringBefore('~')
                    val cut = if ('~' in slot) slot.substringAfter('~') else null
                    require(name == BLANK || name in fields) { "unknown page slot $name" }
                    require(cut == null || cut.isNotEmpty()) { "empty cut in page slot $slot" }
                    Triple(name, cut, if (name == BLANK) { _: CatalogPage -> "" } else fields.getValue(name))
                }
        return { page ->
            parts.flatMap { (_, cut, field) ->
                val value = field(page)
                if (cut == null) listOf(value) else value.splitParts(cut)
            }
        }
    }

    private const val BLANK = "-"
}
