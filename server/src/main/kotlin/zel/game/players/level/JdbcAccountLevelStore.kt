package zel.game.players.level

import zel.db.Sql
import java.sql.ResultSet
import javax.sql.DataSource

class JdbcAccountLevelStore(
    dataSource: DataSource,
) : AccountLevelStore {
    private val sql = Sql(dataSource)

    override fun read(playerId: Int): AccountStanding? =
        sql.select(READ) { int(playerId) }.first { rows -> standing(playerId, rows) }

    override fun inputs(playerId: Int): LevelInputs {
        val badges =
            sql.select(BADGES) { int(playerId) }.rows { rows -> rows.getString(1) }
        val furni = sql.select(FURNI) { int(playerId) }.first { rows -> rows.getInt(1) } ?: 0
        return AccountScoring.badgeInputs(badges, furni)
    }

    override fun save(standing: AccountStanding) {
        sql.write(SAVE) {
            int(standing.playerId)
            int(standing.inputs.achievements)
            int(standing.inputs.badges)
            int(standing.inputs.furni)
            int(standing.countedPoints)
            int(standing.level)
            int(standing.rewardedLevel)
        }
    }

    override fun addHosting(
        playerId: Int,
        levelMinutes: Long,
    ) {
        sql.write(ADD_HOSTING) {
            long(levelMinutes)
            int(playerId)
        }
    }

    private fun standing(
        playerId: Int,
        row: ResultSet,
    ) = AccountStanding(
        playerId,
        LevelInputs(row.getInt("achievement_points"), row.getInt("badge_points"), row.getInt("furni_points")),
        row.getLong("hosting_level_minutes"),
        row.getInt("counted_points"),
        row.getInt("level"),
        row.getInt("rewarded_level"),
    )

    private companion object {
        const val READ = "SELECT * FROM player_account_levels WHERE player_id = ?"
        const val BADGES = "SELECT badge_code FROM player_badges WHERE player_id = ?"

        const val FURNI =
            "SELECT COUNT(DISTINCT f.definition_id) FROM furni f " +
                "INNER JOIN furni_definitions d ON d.id = f.definition_id " +
                "LEFT JOIN build_pass_furni b ON b.furni_id = f.id " +
                "WHERE f.player_id = ? AND b.furni_id IS NULL " +
                "AND d.behaviour NOT IN ('note.sticky', 'display.image') " +
                "AND d.code NOT LIKE 'post\\_it%' AND d.code NOT LIKE 'post.it%' " +
                "AND d.code NOT LIKE 'external\\_image%'"
        const val SAVE =
            "INSERT INTO player_account_levels (player_id, achievement_points, badge_points, furni_points, " +
                "counted_points, level, rewarded_level) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                "achievement_points = VALUES(achievement_points), badge_points = VALUES(badge_points), " +
                "furni_points = VALUES(furni_points), counted_points = VALUES(counted_points), " +
                "level = VALUES(level), rewarded_level = VALUES(rewarded_level)"
        const val ADD_HOSTING =
            "UPDATE player_account_levels SET hosting_level_minutes = hosting_level_minutes + ? WHERE player_id = ?"
    }
}
