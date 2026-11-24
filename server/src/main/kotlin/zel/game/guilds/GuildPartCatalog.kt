package zel.game.guilds

import javax.sql.DataSource

class GuildPartCatalog(
    dataSource: DataSource,
) {
    private val byType: Map<GuildPartType, Map<Int, GuildPart>> = load(dataSource)

    val bases: Collection<GuildPart> get() = partsOf(GuildPartType.BASE).values
    val symbols: Collection<GuildPart> get() = partsOf(GuildPartType.SYMBOL).values
    val baseColors: Collection<GuildPart> get() = partsOf(GuildPartType.BASE_TINT).values
    val symbolColors: Collection<GuildPart> get() = partsOf(GuildPartType.SYMBOL_TINT).values
    val backgroundColors: Collection<GuildPart> get() = partsOf(GuildPartType.BACKGROUND_TINT).values

    fun partsOf(type: GuildPartType): Map<Int, GuildPart> = byType[type].orEmpty()

    fun part(
        type: GuildPartType,
        id: Int,
    ): GuildPart? = byType[type]?.get(id)

    fun hasSymbolColour(id: Int): Boolean = part(GuildPartType.SYMBOL_TINT, id) != null

    fun hasBackgroundColour(id: Int): Boolean = part(GuildPartType.BACKGROUND_TINT, id) != null

    /** White when the colour is missing or switched off. */
    fun symbolColourHex(guild: Guild): String = colourHex(GuildPartType.SYMBOL_TINT, guild.colorOne)

    /** White when the colour is missing or switched off. */
    fun backgroundColourHex(guild: Guild): String = colourHex(GuildPartType.BACKGROUND_TINT, guild.colorTwo)

    private fun colourHex(
        type: GuildPartType,
        id: Int,
    ): String = part(type, id)?.asset ?: WHITE

    private companion object {
        const val WHITE = "ffffff"
        const val SELECT_PARTS = "SELECT id, kind, asset, asset_mask FROM guild_badge_parts WHERE enabled = 1"

        fun load(dataSource: DataSource): Map<GuildPartType, Map<Int, GuildPart>> {
            val rows = ArrayList<Pair<GuildPartType?, GuildPart>>()
            dataSource.readInto("badge parts", rows, SELECT_PARTS) { row ->
                val type = GuildPartType.entries.firstOrNull { it.kind == row.getString("kind") }
                type to
                    GuildPart(row.getInt("id"), row.getString("asset").orEmpty(), row.getString("asset_mask").orEmpty())
            }
            return GuildPartType.entries.associateWith { type ->
                rows
                    .filter { it.first == type }
                    .map { it.second }
                    .sortedWith(compareBy({ editorOrder(type, it.id) }, { it.id }))
                    .associateByTo(LinkedHashMap()) { it.id }
            }
        }

        fun editorOrder(
            type: GuildPartType,
            id: Int,
        ): Int =
            when (type) {
                GuildPartType.BASE -> GuildBadgeCode.editorOrder(true, id)
                GuildPartType.SYMBOL -> GuildBadgeCode.editorOrder(false, id)
                GuildPartType.BASE_TINT -> GuildBadgeCode.colourOrder(id)
                GuildPartType.SYMBOL_TINT, GuildPartType.BACKGROUND_TINT -> id
            }
    }
}
