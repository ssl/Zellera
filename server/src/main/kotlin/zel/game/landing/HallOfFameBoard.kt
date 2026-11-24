package zel.game.landing

import zel.db.Database
import zel.db.SqlFailure
import org.slf4j.LoggerFactory
import java.sql.ResultSet

/** Only [COMPETITION] has standings. */
class HallOfFameBoard(
    private val database: Database,
) {
    data class Standing(
        val rank: Int,
        val playerId: Int,
        val name: String,
        val figure: String,
        val points: Int,
    )

    @Volatile
    var standings: List<Standing> = emptyList()
        private set

    init {
        reload()
    }

    fun reload() {
        try {
            standings = rank(read())
        } catch (e: SqlFailure) {
            LOGGER.error("Reading the hall of fame failed", e)
        }
    }

    fun standings(code: String): List<Standing> = if (code == COMPETITION) standings else emptyList()

    private fun read(): List<Standing> = database.queries.select(TOP_PLAYERS).rows(::unranked)

    private fun unranked(row: ResultSet) =
        Standing(
            rank = 0,
            playerId = row.getInt("id"),
            name = row.getString("name").orEmpty(),
            figure = row.getString("figure").orEmpty(),
            points = row.getInt("hall_of_fame_points"),
        )

    companion object {
        const val COMPETITION = "xmasRoomComp"

        internal const val TOP_PLAYERS =
            "SELECT p.id, p.name, p.figure, s.hall_of_fame_points FROM player_stats s " +
                "INNER JOIN players p ON p.id = s.player_id WHERE s.hall_of_fame_points > 0 " +
                "ORDER BY s.hall_of_fame_points DESC, p.id LIMIT 10"

        private val LOGGER = LoggerFactory.getLogger(HallOfFameBoard::class.java)

        /** Keeps a player's last row; the stable sort keeps ties in read order. */
        internal fun rank(rows: List<Standing>): List<Standing> =
            rows
                .associateBy { it.playerId }
                .values
                .sortedByDescending { it.points }
                .mapIndexed { index, standing -> standing.copy(rank = index + 1) }
    }
}
