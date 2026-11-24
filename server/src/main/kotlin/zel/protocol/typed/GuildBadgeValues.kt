package zel.protocol.typed

import zel.game.guilds.GuildPartCatalog
import zel.game.guilds.GuildPartType

internal object GuildBadgeValues {
    private const val PER_LAYER = 3
    private const val MAX_POSITION = 8

    fun allowed(
        catalog: GuildPartCatalog,
        values: List<Int>,
    ): Boolean =
        (catalog.bases.isEmpty() && catalog.symbols.isEmpty()) ||
            layersKnown(catalog, values)

    private fun layersKnown(
        catalog: GuildPartCatalog,
        values: List<Int>,
    ): Boolean =
        values.size % PER_LAYER == 0 &&
            values.chunked(PER_LAYER).all { (id, colour, position) ->
                val known =
                    catalog.part(GuildPartType.BASE, id) != null || catalog.part(GuildPartType.SYMBOL, id) != null
                known && catalog.part(GuildPartType.BASE_TINT, colour) != null && position in 0..MAX_POSITION
            }
}
