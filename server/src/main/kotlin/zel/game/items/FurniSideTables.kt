package zel.game.items

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class FurniSideTables internal constructor(
    private val queries: ItemQueries,
) {
    @Volatile
    private var crackables: Map<Int, CrackableKind> = emptyMap()

    @Volatile
    private var plants: Map<String, PlantKind> = emptyMap()

    @Volatile
    private var tracks: Map<String, JukeboxTrack> = emptyMap()

    @Volatile
    private var welcomeGifts: List<WelcomeGift> = emptyList()

    fun load() {
        val prizes = LinkedHashMap<Int, MutableList<CrackableKind.Prize>>()
        queries.forEachRow(CRACKABLE_PRIZES_SQL) { row ->
            val prize = CrackableKind.Prize(row.getInt("prize_definition_id"), row.getInt("chance"))
            prizes.getOrPut(row.getInt("definition_id")) { ArrayList() } += prize
        }
        crackables =
            keyed("SELECT * FROM crackables") { row ->
                val id = row.getInt("definition_id")
                id to CrackableKind.read(row, prizes[id].orEmpty())
            }
        plants =
            keyed("SELECT * FROM plant_kinds") { row ->
                row.getString("furni_code")?.let {
                    it.lowercase() to PlantKind(row.getInt("grow_count"), row.getInt("death_count"))
                }
            }
        tracks =
            keyed("SELECT * FROM soundtracks") { row ->
                row.getString("code") to
                    JukeboxTrack(
                        row.getInt("id"),
                        row.getString("code"),
                        row.getString("name"),
                        row.getString("author"),
                        row.getString("track"),
                        row.getInt("length_seconds"),
                    )
            }
        welcomeGifts =
            keyed("SELECT * FROM welcome_gifts") { it.getInt("id") to WelcomeGift.read(it) }
                .toSortedMap()
                .values
                .toList()
    }

    private fun <K, V> keyed(
        sql: String,
        pair: (ResultSet) -> Pair<K, V>?,
    ): Map<K, V> {
        val read = LinkedHashMap<K, V>()
        queries.forEachRow(sql) { row ->
            runCatching { pair(row)?.let { (key, value) -> read[key] = value } }
                .onFailure { LOGGER.error("Row skipped in {}", sql, it) }
        }
        return read
    }

    fun crackable(definitionId: Int): CrackableKind? = crackables[definitionId]

    fun plant(furniCode: String?): PlantKind? = furniCode?.let { plants[it.lowercase()] }

    fun track(code: String?): JukeboxTrack? = code?.let { tracks[it] }

    fun track(id: Int): JukeboxTrack? = tracks.values.firstOrNull { it.id == id }

    fun welcomeGifts(): List<WelcomeGift> = welcomeGifts

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(FurniSideTables::class.java)

        const val CRACKABLE_PRIZES_SQL =
            "SELECT definition_id, prize_definition_id, chance FROM crackable_prizes" +
                " ORDER BY definition_id, prize_definition_id"
    }
}
