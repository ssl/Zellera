package zel.game.items

import org.slf4j.LoggerFactory
import zel.core.rules.rules
import zel.core.threading.GameThreads
import zel.db.first
import zel.db.insertForId
import zel.db.write
import zel.game.catalog.Catalog
import zel.game.players.Players
import zel.protocol.outgoing.inventory.AddRoomItem
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

class Items(
    dataSource: DataSource,
    threading: GameThreads,
    catalog: () -> Catalog?,
    private val players: () -> Players?,
    private val texts: () -> FurniTitles?,
    val services: ItemServices,
) {
    private val queries = ItemQueries(dataSource)
    private val definitions = ConcurrentHashMap<Int, Item>()

    val sideTables = FurniSideTables(queries)

    val prizeBoxes = PrizeBoxes(queries, catalog, ::loadFurni)

    val youtube = YoutubePlaylists(dataSource, threading)

    val recyclerEnabled: Boolean
        get() = rules.recycler

    fun load() {
        readDefinitions()
        sideTables.load()
        youtube.load()
        LOGGER.info("{} furni definitions loaded", definitions.size)
    }

    /** A definition already in memory keeps its identity and takes the new row. */
    fun readDefinitions() {
        queries.forEachRow(DEFINITIONS_SQL) { row ->
            val id = row.getInt("id")
            runCatching {
                val existing = definitions[id]
                if (existing == null) {
                    definitions[id] = Item(row, ::ItemInteraction, texts, services)
                } else {
                    existing.reload(row)
                }
            }.onFailure { LOGGER.error("Failed to load furni definition {}", id, it) }
        }
    }

    fun getItem(itemId: Int): Item? = if (itemId <= 0) null else definitions[itemId]

    fun getItem(name: String?): Item? =
        if (name.isNullOrBlank()) null else definitions.values.firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** The definition with this behaviour, and these params when given; null when none or several match. */
    fun byBehaviour(
        behaviour: String,
        params: String? = null,
    ): Item? =
        definitions.values
            .singleOrNull { it.interaction.name == behaviour && (params == null || it.params == params) }

    /** The recycler box a recycling hands out: the one marked so in its params. */
    fun recyclerBox(): Item? = byBehaviour(ItemInteraction.RECYCLER_BOX, RECYCLER_BOX_PARAMS)

    fun getItems(): Map<Int, Item> = definitions

    fun drawCrackablePrize(definitionId: Int): Item? = sideTables.crackable(definitionId)?.let { getItem(it.draw()) }

    fun jukeboxTrack(code: String?): JukeboxTrack? = sideTables.track(code)

    fun jukeboxTrack(id: Int): JukeboxTrack? = sideTables.track(id)

    private fun insertFurni(
        connection: Connection,
        ownerId: Int,
        item: Item,
        editionSize: Int,
        editionNumber: Int,
        extraData: String?,
    ): OwnedFurni? {
        val state = openingState(item, extraData)
        val id =
            connection.insertForId(NEW_FURNI_SQL) {
                intOrNull(ownerId)
                int(item.id)
                string(state)
                int(editionNumber)
                int(editionSize)
            }
        return id?.let { OwnedFurni(it, ownerId, item, state, editionSize, editionNumber) }
    }

    private fun openingState(
        item: Item,
        extraData: String?,
    ): String {
        val state = boundedState(extraData)
        val seed = item.interaction.name == ItemInteraction.MONSTER_PLANT_SEED
        return if (seed && state.isEmpty()) MonsterPlantSeeds.rolledLevel(item) else state
    }

    fun newFurni(
        playerId: Int,
        item: Item?,
        editionSize: Int,
        editionNumber: Int,
        extraData: String?,
    ): OwnedFurni? =
        if (playerId <= 0 || item == null) {
            null
        } else {
            queries.withConnection { newFurni(it, playerId, item, editionSize, editionNumber, extraData) }
        }

    @Throws(SQLException::class)
    fun newFurni(
        connection: Connection,
        playerId: Int,
        item: Item?,
        editionSize: Int,
        editionNumber: Int,
        extraData: String?,
    ): OwnedFurni? =
        if (playerId <= 0 || item == null) {
            null
        } else {
            insertFurni(connection, playerId, item, editionSize, editionNumber, extraData)
        }

    fun newGiftContent(
        item: Item?,
        editionSize: Int,
        editionNumber: Int,
        extraData: String?,
    ): OwnedFurni? =
        item?.let {
            queries.withConnection { connection ->
                insertFurni(connection, BoxOpenings.GIFT_CONTENT_OWNER, it, editionSize, editionNumber, extraData)
            }
        }

    fun deliverGift(
        playerId: Int,
        furni: OwnedFurni,
    ) {
        players()?.find(playerId)?.let { receiver ->
            receiver.inventory.furni.put(furni)
            receiver.client?.send(AddRoomItem(furni))
        }
    }

    /** The teleport pair row goes with it (the pair table cascades). */
    fun deleteFurni(furniId: Int) {
        queries.withConnection { connection -> connection.write(DELETE_FURNI_SQL) { int(furniId) } }
    }

    fun pairTeleports(
        firstId: Int,
        secondId: Int,
    ) {
        queries.withConnection { pairTeleports(it, firstId, secondId) }
    }

    @Throws(SQLException::class)
    fun pairTeleports(
        connection: Connection,
        firstId: Int,
        secondId: Int,
    ) {
        connection.write(PAIR_TELEPORT_SQL) {
            int(firstId)
            int(secondId)
        }
    }

    fun teleportPartner(furniId: Int): Int =
        queries.firstRow(TELEPORT_PARTNER_SQL, furniId, furniId) { row -> row.getInt(1) } ?: 0

    fun loadFurni(furniId: Int): OwnedFurni? = queries.withConnection { loadFurni(it, furniId, null) }

    @Throws(SQLException::class)
    fun readFurni(row: ResultSet): OwnedFurni? =
        getItem(row.getInt("definition_id"))?.let { OwnedFurniRows.read(row, it) }

    private fun loadFurni(
        connection: Connection,
        furniId: Int,
        ownerId: Int?,
    ): OwnedFurni? =
        connection.first(if (ownerId == null) FURNI_SQL else BoxOpenings.OPENED_GIFT_CONTENT_SQL, {
            int(furniId)
            if (ownerId != null) int(ownerId)
        }, ::readFurni)

    fun giveFurni(
        playerId: Int,
        item: Item?,
        extraData: String?,
        editionSize: Int,
        editionNumber: Int,
    ): OwnedFurni? {
        if (playerId <= 0 || item == null) return null
        val length = extraData?.length ?: 0
        if (length > MAX_STATE_LENGTH) {
            LOGGER.warn("New {} for player {}: its state of {} characters is cut to fit", item.name, playerId, length)
        }
        val furni = newFurni(playerId, item, editionSize, editionNumber, boundedState(extraData)) ?: return null
        players()?.find(playerId)?.let { receiver ->
            receiver.inventory.furni.put(furni)
            receiver.client?.send(AddRoomItem(furni))
        }
        return furni
    }

    fun dispose() {
        definitions.clear()
        LOGGER.info("furni definitions released")
    }

    internal companion object {
        private val LOGGER = LoggerFactory.getLogger(Items::class.java)

        /** The width of the furni row's state column. */
        const val MAX_STATE_LENGTH = 1024

        private const val RECYCLER_BOX_PARAMS = "recycler"

        fun boundedState(text: String?): String = text.orEmpty().take(MAX_STATE_LENGTH)

        private const val DEFINITIONS_SQL = """
            SELECT d.*,
                   (SELECT GROUP_CONCAT(h.handitem_id ORDER BY h.position) FROM furni_handitems h
                     WHERE h.definition_id = d.id) AS handitems,
                   (SELECT GROUP_CONCAT(s.height ORDER BY s.state) FROM furni_state_heights s
                     WHERE s.definition_id = d.id) AS state_heights
            FROM furni_definitions d
            """

        private const val NEW_FURNI_SQL =
            "INSERT INTO furni (player_id, definition_id, state, limited_number, limited_total) VALUES (?, ?, ?, ?, ?)"

        private const val FURNI_SQL = "SELECT * FROM furni WHERE id = ? LIMIT 1"

        private const val PAIR_TELEPORT_SQL =
            "INSERT INTO teleport_pairs (first_furni_id, second_furni_id) VALUES (?, ?)"

        private const val DELETE_FURNI_SQL = "DELETE FROM furni WHERE id = ?"

        private const val TELEPORT_PARTNER_SQL =
            "SELECT second_furni_id FROM teleport_pairs WHERE first_furni_id = ? " +
                "UNION ALL SELECT first_furni_id FROM teleport_pairs WHERE second_furni_id = ? LIMIT 1"
    }
}
