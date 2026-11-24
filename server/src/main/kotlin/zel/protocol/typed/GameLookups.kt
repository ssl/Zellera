package zel.protocol.typed

import zel.core.Configuration
import zel.core.threading.GameThreads
import zel.db.Sql
import zel.game.Game
import zel.game.catalog.BundleDiscount
import zel.game.catalog.CatalogMode
import zel.game.catalog.CatalogSettings
import zel.game.players.Player
import zel.wired.protocol.WiredFormPackets
import java.sql.SQLException
import javax.sql.DataSource

private fun int(check: (Int) -> Boolean?) = AllowedLookup({ _, value -> (value as? Int)?.let(check) ?: true })

private fun text(check: (String) -> Boolean?) = AllowedLookup({ _, value -> (value as? String)?.let(check) ?: true })

private fun ints(check: (List<Int>) -> Boolean?) =
    AllowedLookup({ _, value -> (value as? List<*>)?.filterIsInstance<Int>()?.let(check) ?: true })

private fun playerInt(
    now: (Player, Int) -> Boolean?,
    later: ((Int) -> Boolean)? = null,
) = AllowedLookup(
    { client, value ->
        val player = client.player
        if (player != null && value is Int) now(player, value) else true
    },
    later?.let { read -> { value -> (value as? Int)?.let(read) ?: true } },
)

fun gameAllowedValues(
    game: Game,
    config: Configuration,
    dataSource: DataSource,
    threading: GameThreads,
): AllowedValues =
    AllowedValues(
        gameLookups(game, config, dataSource),
        { config.getBoolean("protocol.unlisted_values.are_dropped", false) },
    ) { task ->
        // a busy database costs these checks, never play
        val pressure = threading.writePressure()
        if (pressure == null || pressure.waiting * 2 < pressure.capacity) threading.write(task)
    }

@Suppress("LongMethod", "CyclomaticComplexMethod") // one line or two per lookup
fun gameLookups(
    game: Game,
    config: Configuration,
    dataSource: DataSource,
): AllowedLookups {
    fun parts() = game.guilds?.parts

    fun catalogSettings() = game.catalog?.settings

    val sql = Sql(dataSource)

    // a check the database cannot answer lets the value through
    fun found(
        query: String,
        vararg values: Any?,
    ): Boolean =
        try {
            sql.select(query, *values).first { true } ?: false
        } catch (_: SQLException) {
            true
        }

    fun anyRow(table: String): Boolean = found("SELECT 1 FROM $table LIMIT 1")

    return AllowedLookups(
        mapOf(
            // the models live in the database only; room creation refuses an unknown one itself
            "roomModel" to
                AllowedLookup({ _, _ -> null }, { value ->
                    value !is String || game.roomListings.modelExists(value) || !anyRow("room_models")
                }),
            "roomCategory" to
                int {
                    it == 0 || game.roomListings.category(it) != null || game.roomListings.categoriesInOrder().isEmpty()
                },
            "guildBadge" to ints { values -> parts()?.let { GuildBadgeValues.allowed(it, values) } ?: true },
            "guildSymbolColour" to
                int { id -> id == 0 || parts()?.let { it.hasSymbolColour(id) || it.symbolColors.isEmpty() } ?: true },
            "guildBackgroundColour" to
                int { id ->
                    id == 0 ||
                        parts()?.let { it.hasBackgroundColour(id) || it.backgroundColors.isEmpty() } ?: true
                },
            "catalogAmount" to
                int { amount ->
                    val maximum =
                        catalogSettings()?.bundleDiscount?.maximumItems ?: BundleDiscount.STANDARD_MAXIMUM_ITEMS
                    amount in 1..maximum
                },
            "giftSprite" to
                int { sprite ->
                    game.catalog?.let {
                        sprite in it.contents.wrappers || sprite in it.contents.giftFurni ||
                            (it.contents.wrappers.isEmpty() && it.contents.giftFurni.isEmpty())
                    } ?: true
                },
            "giftBox" to int { it in (catalogSettings()?.giftBoxTypes ?: CatalogSettings.STANDARD_GIFT_BOX_TYPES) },
            "giftRibbon" to
                int { it in (catalogSettings()?.giftRibbonTypes ?: CatalogSettings.STANDARD_GIFT_RIBBON_TYPES) },
            "figure" to text { FIGURE.matches(it) },
            "wiredDelay" to int { it in 0..config.getInt(WIRED_MAX_DELAY_KEY, WiredFormPackets.DEFAULT_MAX_DELAY) },
            "cfhTopic" to
                int { topic ->
                    game.moderationService?.let {
                        it.catalog.topic(
                            topic,
                        ) != null || it.catalog.cfhCategories.isEmpty()
                    }
                        ?: true
                },
            "room" to
                AllowedLookup(
                    { _, value -> if (value is Int && game.rooms.isLoaded(value)) true else null },
                    { value -> game.rooms.summary(value as Int) != null },
                ),
            "player" to
                AllowedLookup(
                    { _, value -> if (value is Int && game.players?.find(value) != null) true else null },
                    { value -> found("SELECT 1 FROM players WHERE id = ?", value as Int) },
                ),
            "guild" to AllowedLookup({ _, _ -> null }, { value -> game.guilds?.byId(value as Int) != null }),
            "furniInRoom" to
                playerInt({ player, id ->
                    val room = game.rooms.roomOf(player.profile.id)
                    room == null || game.rooms.furniIn(room, id) != null
                }),
            "ownFurni" to playerInt({ player, id -> player.inventory.furni[id] != null }),
            "pet" to
                playerInt(
                    { player, id -> if (player.inventory.pets[id] != null) true else null },
                    { id -> found("SELECT 1 FROM pets WHERE id = ?", id) },
                ),
            "bot" to
                playerInt(
                    { player, id -> if (player.inventory.bots[id] != null) true else null },
                    { id -> found("SELECT 1 FROM bots WHERE id = ?", id) },
                ),
            "catalogPage" to
                int { id ->
                    game.catalog?.let {
                        it.page(id) != null ||
                            it.page(id, CatalogMode.BUILDER) != null ||
                            it.contents.pages.isEmpty()
                    } ?: true
                },
            "catalogOffer" to
                int { id ->
                    game.catalog?.let {
                        it.offer(id) != null ||
                            it.offer(id, CatalogMode.BUILDER) != null ||
                            it.contents.pages.isEmpty()
                    } ?: true
                },
            "marketplaceOffer" to
                AllowedLookup({ _, _ -> null }, { value ->
                    found("SELECT 1 FROM marketplace_listings WHERE id = ?", value as Int)
                }),
            "quest" to
                int { id ->
                    game.quests?.let {
                        it.knows(id) || it.hasNoQuests
                    } ?: true
                },
            // a poll made after the start is read from the database on first use
            "poll" to
                AllowedLookup({ _, _ -> null }, { value ->
                    game.polls?.let { it.getPoll(value as Int) != null || !anyRow("polls") } ?: true
                }),
            "ownBadge" to
                AllowedLookup({ client, value ->
                    val player = client.player
                    player == null || value !is String || player.inventory.badges.owns(value)
                }),
        ),
    )
}

private const val WIRED_MAX_DELAY_KEY = "wired.box.max_delay.half_seconds"

private val FIGURE = Regex("""[a-z]{2}-\d+(-\d*)*(\.[a-z]{2}-\d+(-\d*)*)*""")
