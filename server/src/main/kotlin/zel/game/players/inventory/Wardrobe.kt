package zel.game.players.inventory

import zel.db.SqlFailure
import zel.game.players.Gender
import zel.game.players.PlayerSaveQueue
import zel.server.GameServices
import it.unimi.dsi.fastutil.ints.IntCollection
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import org.slf4j.LoggerFactory
import java.sql.SQLException
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger(Wardrobe::class.java)

private const val SAVE_OUTFIT_SQL =
    "INSERT INTO player_looks (player_id, slot, gender, figure) VALUES (?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE gender = VALUES(gender), figure = VALUES(figure)"
private const val OUTFITS_SQL = "SELECT slot, gender, figure FROM player_looks WHERE player_id = ?"

/** One row per set a clothing unlocks; a clothing without sets still counts as owned. */
private const val CLOTHING_SQL =
    "SELECT c.clothing_id, s.set_id FROM player_clothing c " +
        "LEFT JOIN clothing_sets s ON s.clothing_id = c.clothing_id WHERE c.player_id = ?"

class Wardrobe(
    private val services: GameServices,
    private val playerId: Int,
) {
    data class Outfit(
        val slot: Int,
        val figure: String,
        val gender: Gender,
    )

    private val bySlot = ConcurrentHashMap<Int, Outfit>()
    private val clothingIds: IntSet = IntOpenHashSet()
    private val setIds: IntSet = IntOpenHashSet()
    private val writes = PlayerSaveQueue { services.threading.run(it) }

    init {
        try {
            readOutfits()
            readClothing()
        } catch (e: SQLException) {
            log.error("Could not read the wardrobe of player {}", playerId, e)
        }
    }

    val outfits: Map<Int, Outfit> get() = bySlot

    val clothing: IntCollection get() = clothingIds

    val clothingSets: IntCollection get() = setIds

    fun addClothing(
        clothingId: Int,
        sets: IntArray,
    ) {
        clothingIds.add(clothingId)
        sets.forEach(setIds::add)
    }

    fun save(
        slot: Int,
        figure: String,
        gender: Gender,
    ) {
        val outfit = Outfit(slot, figure, gender)
        bySlot[slot] = outfit
        writes.submit(playerId) { store(outfit) }
    }

    fun dispose() = bySlot.clear()

    private fun store(outfit: Outfit) {
        try {
            services.database.queries.write(SAVE_OUTFIT_SQL) {
                int(playerId)
                int(outfit.slot)
                string(outfit.gender.name)
                string(outfit.figure)
            }
        } catch (e: SqlFailure) {
            log.error("Could not save outfit {} of player {}", outfit.slot, playerId, e)
        }
    }

    private fun readOutfits() =
        services.database.queries.select(OUTFITS_SQL) { int(playerId) }.each { row ->
            val slot = row.getInt("slot")
            val gender = Gender.ofCode(row.getString("gender")) ?: Gender.M
            bySlot[slot] = Outfit(slot, row.getString("figure"), gender)
        }

    private fun readClothing() =
        services.database.queries.select(CLOTHING_SQL) { int(playerId) }.each { row ->
            clothingIds.add(row.getInt("clothing_id"))
            val setId = row.getInt("set_id")
            if (!row.wasNull()) setIds.add(setId)
        }
}
