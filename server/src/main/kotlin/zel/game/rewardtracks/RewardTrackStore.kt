package zel.game.rewardtracks

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.SqlFailure
import zel.db.allOrNothing
import zel.db.orFallback
import zel.db.write
import zel.game.quests.CountedAction
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

internal class RewardTrackStore(
    private val dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    fun definitions(allowed: (String) -> Boolean): List<RewardTrackDefinition> {
        val themes = LinkedHashMap<String, String>()
        sql.select(SELECT_TRACKS).each { row ->
            val id = row.getString("id")
            if (allowed(id)) themes[id] = row.getString("theme")
        }
        val levels = HashMap<Pair<String, String>, MutableList<RewardTrackDefinition.Level>>()
        sql.select(SELECT_LEVELS).each { row ->
            levels
                .getOrPut(row.getString("track_id") to row.getString("task_id")) { ArrayList() }
                .add(level(row))
        }
        val tasks = HashMap<String, MutableList<RewardTrackDefinition.Task>>()
        sql.select(SELECT_TASKS).each { row ->
            val trackId = row.getString("track_id")
            tasks.getOrPut(trackId) { ArrayList() }.add(task(row, levels[trackId to row.getString("id")].orEmpty()))
        }
        val prizes = HashMap<String, MutableList<RewardTrackDefinition.Prize>>()
        sql.select(SELECT_PRIZES).each { row ->
            prizes.getOrPut(row.getString("track_id")) { ArrayList() }.add(prize(row))
        }
        return themes.map { (id, theme) ->
            RewardTrackDefinition(id, theme, tasks[id].orEmpty(), prizes[id].orEmpty())
        }
    }

    private fun level(row: ResultSet) =
        RewardTrackDefinition.Level(row.getInt("required_count"), row.getInt("points"), row.getBoolean("is_premium"))

    private fun task(
        row: ResultSet,
        levels: List<RewardTrackDefinition.Level>,
    ): RewardTrackDefinition.Task {
        val actionType = row.getString("action_type")
        return RewardTrackDefinition.Task(
            row.getString("id"),
            actionType,
            row.getString("parameter"),
            row.getBoolean("is_premium"),
            CountedAction.named(actionType),
            levels,
        )
    }

    private fun prize(row: ResultSet) =
        RewardTrackDefinition.Prize(
            row.getString("id"),
            row.getInt("required_points"),
            row.getInt("product_type"),
            row.getString("product_code"),
            row.getString("extra"),
            row.getInt("amount"),
            row.getBoolean("is_premium"),
        )

    /** Throws [SqlFailure] rather than hand out a partial state. */
    fun playerTracks(playerId: Int): MutableMap<String, PlayerTrack> {
        val byTrack = ConcurrentHashMap<String, PlayerTrack>()

        fun track(row: ResultSet) = byTrack.computeIfAbsent(row.getString("track_id")) { PlayerTrack() }
        sql.select(SELECT_POINTS, playerId).each { track(it).restorePoints(it.getInt("points")) }
        sql.select(SELECT_PROGRESS, playerId).each {
            track(it).restoreProgress(it.getString("task_id"), it.getInt("progress"))
        }
        sql.select(SELECT_CLAIMS, playerId).each { track(it).restoreClaim(it.getString("prize_id")) }
        sql.select(SELECT_PASSES, playerId).each { track(it).restorePass() }
        return byTrack
    }

    fun saveProgress(
        playerId: Int,
        trackId: String,
        taskId: String,
        moved: PlayerTrack.Moved,
    ) {
        LOGGER.orFallback("Saving reward track $trackId/$taskId of player $playerId", Unit) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    connection.write(UPSERT_PROGRESS) {
                        int(playerId)
                        string(trackId)
                        string(taskId)
                        int(moved.count)
                    }
                    connection.write(UPSERT_POINTS) {
                        int(playerId)
                        string(trackId)
                        int(moved.points)
                    }
                }
            }
        }
    }

    fun saveClaim(
        playerId: Int,
        trackId: String,
        prizeId: String,
    ) {
        LOGGER.orFallback("Saving reward track claim $trackId/$prizeId of player $playerId", Unit) {
            sql.write(INSERT_CLAIM, playerId, trackId, prizeId)
        }
    }

    fun savePass(
        playerId: Int,
        trackId: String,
        points: Int,
    ) {
        LOGGER.orFallback("Saving the reward track pass $trackId of player $playerId", Unit) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    connection.write(INSERT_PASS) {
                        int(playerId)
                        string(trackId)
                    }
                    connection.write(UPSERT_POINTS) {
                        int(playerId)
                        string(trackId)
                        int(points)
                    }
                }
            }
        }
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(RewardTrackStore::class.java)

        const val SELECT_TRACKS = "SELECT id, theme FROM reward_tracks WHERE enabled = 1 ORDER BY sort_order, id"
        const val SELECT_LEVELS =
            "SELECT track_id, task_id, level, required_count, points, is_premium FROM reward_track_levels " +
                "ORDER BY track_id, task_id, level"
        const val SELECT_TASKS =
            "SELECT track_id, id, action_type, parameter, is_premium, sort_order FROM reward_track_tasks " +
                "ORDER BY track_id, sort_order, id"
        const val SELECT_PRIZES =
            "SELECT track_id, id, sort_order, required_points, product_type, product_code, extra, amount, " +
                "is_premium FROM reward_track_prizes ORDER BY track_id, sort_order, id"
        const val SELECT_POINTS = "SELECT track_id, points FROM player_reward_track_points WHERE player_id = ?"
        const val SELECT_PROGRESS =
            "SELECT track_id, task_id, progress FROM player_reward_track_progress WHERE player_id = ?"
        const val SELECT_CLAIMS = "SELECT track_id, prize_id FROM player_reward_track_claims WHERE player_id = ?"
        const val SELECT_PASSES = "SELECT track_id FROM player_reward_track_passes WHERE player_id = ?"
        const val UPSERT_PROGRESS =
            "INSERT INTO player_reward_track_progress (player_id, track_id, task_id, progress)" +
                " VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE progress = GREATEST(progress, VALUES(progress))"
        const val UPSERT_POINTS =
            "INSERT INTO player_reward_track_points (player_id, track_id, points) VALUES (?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE points = GREATEST(points, VALUES(points))"
        const val INSERT_CLAIM =
            "INSERT IGNORE INTO player_reward_track_claims (player_id, track_id, prize_id, claimed_at)" +
                " VALUES (?, ?, ?, UNIX_TIMESTAMP())"
        const val INSERT_PASS =
            "INSERT IGNORE INTO player_reward_track_passes (player_id, track_id, bought_at)" +
                " VALUES (?, ?, UNIX_TIMESTAMP())"
    }
}
