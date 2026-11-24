package zel.game.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.rows
import zel.game.items.Item
import java.sql.Connection
import java.sql.SQLException

class RecyclerCatalog(
    private val services: CatalogServices,
    private val data: CatalogData,
) {
    @Volatile
    var entries: List<RecyclerPrizes.Entry> = emptyList()
        private set

    val boxItem: Item? get() = services.items.recyclerBox()

    fun load() {
        val rows =
            try {
                services.database.dataSource.connection
                    .use(::readRows)
            } catch (e: SQLException) {
                log.error("Loading the recycler prizes failed", e)
                emptyList()
            }
        val items = services.items
        val resolved =
            RecyclerPrizes.resolve(rows, { items.getItem(it) }, { items.getItem(it) }) {
                log.warn("Recycler prize of level {} has no furni: {} / {}", it.level, it.itemId, it.itemName)
            }
        entries = resolved
        val byLevel = HashMap<Int, MutableSet<Item>>()
        resolved.forEach { entry -> entry.item?.let { byLevel.getOrPut(entry.level, ::LinkedHashSet) += it } }
        CatalogData.refill(data.recyclerPrizes, byLevel)
    }

    private fun readRows(connection: Connection): List<RecyclerPrizes.Row> =
        connection.rows(PRIZES) { set ->
            RecyclerPrizes.Row(
                set.getInt("rarity"),
                set.getInt("definition_id"),
                set.getString("furni_code"),
                set.getInt("chat_bubble_id"),
                set.getInt("sort_order"),
            )
        }

    /** The `recycler.prize.level_<level>.one_in` setting: the level wins 1 in this many draws; 0 (unset) never. */
    fun rarityChance(level: Int): Int = services.config.getInt("recycler.prize.level_$level.one_in", 0)

    /** Draws from every prize the list shows, chat styles too; falls back to the nearest lower level with a prize. */
    fun draw(): RecyclerDraw? {
        val random = services.random
        val level = RecyclerPrizes.drawLevel({ rarityChance(it) }, random)
        val shown = RecyclerPrizes.byLevel(entries)
        return (level downTo 1).firstNotNullOfOrNull { candidate ->
            val prizes = shown[candidate].orEmpty()
            if (prizes.isEmpty()) null else RecyclerDraw(candidate, prizes[random.nextInt(prizes.size)])
        }
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RecyclerCatalog::class.java)
        const val PRIZES = "SELECT rarity, definition_id, furni_code, chat_bubble_id, sort_order FROM recycler_prizes"
    }
}
