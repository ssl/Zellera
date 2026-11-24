package zel.game.treasurehunt

import zel.db.Database
import zel.db.SqlFailure
import zel.db.allOrNothing
import zel.db.first
import zel.db.write
import zel.game.treasurehunt.TreasureHunt.Prize
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

class HuntRecords(
    database: Database,
) {
    private val dataSource: DataSource = database.dataSource
    private val sql = database.queries

    /** Where a player stands after a find: [finishedNow] and [firstOfAll] are true once per player and per hunt. */
    data class Standing(
        val found: Int,
        val finishedNow: Boolean = false,
        val firstOfAll: Boolean = false,
    )

    /** The open hunts that have at least one open clue. */
    fun openHunts(): List<TreasureHunt> {
        val clues = sql.select(CLUES).rows { it.getInt("hunt_id") to it.clue() }.groupBy({ it.first }, { it.second })
        val prizes = sql.select(PRIZES).rows { it.getInt("hunt_id") to it.prize() }.groupBy({ it.first }, { it.second })
        return sql
            .select(HUNTS)
            .rows { row ->
                val id = row.getInt("id")
                TreasureHunt(
                    id,
                    row.getString("code"),
                    TreasureHunt.Entry(row.getInt("level_needed"), row.getInt("club_level_needed")),
                    prizes[id].orEmpty().filterNotNull(),
                    clues[id].orEmpty(),
                )
            }.filter { it.clues.isNotEmpty() }
    }

    /** One transaction: the find, the count it leads to, and for a full count the finish and the first place. */
    fun found(
        hunt: TreasureHunt,
        clue: HuntClue,
        playerId: Int,
        at: Int,
    ): Standing =
        try {
            dataSource.connection.use { connection ->
                checkNotNull(connection.allOrNothing { connection.found(hunt, clue, playerId, at) })
            }
        } catch (failure: SQLException) {
            throw SqlFailure(FIND, failure)
        }

    private fun Connection.found(
        hunt: TreasureHunt,
        clue: HuntClue,
        playerId: Int,
        at: Int,
    ): Standing {
        write(FIND) {
            int(playerId)
            int(clue.id)
            int(at)
        }
        val found =
            first(FOUND_SO_FAR, {
                int(playerId)
                int(hunt.id)
            }) { it.getInt(1) } ?: 0
        if (found < hunt.steps) return Standing(found)
        val finishedNow =
            write(FINISH) {
                int(hunt.id)
                int(playerId)
                int(at)
            } > 0
        val firstOfAll =
            finishedNow &&
                write(FIRST_PLACE) {
                    int(playerId)
                    int(hunt.id)
                } > 0
        return Standing(found, finishedNow, firstOfAll)
    }

    private fun ResultSet.clue(): HuntClue {
        val id = getInt("id")
        val furni = getInt("furni_id")
        return if (wasNull()) HuntClue.Step(id, getString("step_code")) else HuntClue.Furni(id, furni)
    }

    private fun ResultSet.prize(): Prize? {
        val amount = getInt("amount")
        val badge = getString("badge_code")
        val definition = getInt("definition_id").takeUnless { wasNull() }
        val pointsType = getInt("points_type").takeUnless { wasNull() }
        return when {
            badge != null -> Prize.Badge(badge)
            definition != null -> Prize.Furni(definition, amount)
            pointsType != null -> Prize.Points(pointsType, amount)
            else -> null
        }
    }

    private companion object {
        const val HUNTS = "SELECT id, code, level_needed, club_level_needed FROM treasure_hunts WHERE is_open = 1"
        const val CLUES =
            "SELECT id, hunt_id, furni_id, step_code FROM treasure_hunt_clues WHERE is_open = 1 ORDER BY id"
        const val PRIZES =
            "SELECT hunt_id, badge_code, definition_id, points_type, amount FROM treasure_hunt_prizes " +
                "ORDER BY hunt_id, position"
        const val FIND = "INSERT IGNORE INTO treasure_hunt_found (player_id, clue_id, found_at) VALUES (?, ?, ?)"
        const val FOUND_SO_FAR =
            "SELECT COUNT(*) FROM treasure_hunt_found f JOIN treasure_hunt_clues c ON c.id = f.clue_id " +
                "WHERE f.player_id = ? AND c.hunt_id = ? AND c.is_open = 1"
        const val FINISH =
            "INSERT IGNORE INTO treasure_hunt_finishers (hunt_id, player_id, finished_at) VALUES (?, ?, ?)"
        const val FIRST_PLACE =
            "UPDATE treasure_hunts SET first_finisher_id = ? WHERE id = ? AND first_finisher_id IS NULL"
    }
}
