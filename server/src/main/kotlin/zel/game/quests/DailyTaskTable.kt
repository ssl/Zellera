package zel.game.quests

import zel.db.Sql
import java.sql.ResultSet
import java.time.LocalDate
import javax.sql.DataSource

/** Daily task definitions and rows; a task nothing reports is left out; a stored row never returns to claimable. */
class DailyTaskTable(
    dataSource: DataSource,
) : DailyTaskRecords {
    private val sql = Sql(dataSource)

    private enum class Span(
        val selection: String,
    ) {
        THAT_DAY("day = ? ORDER BY task_id"),

        /** The 50 newest unclaimed rows of earlier days: older ones wait until these are claimed. */
        EARLIER_UNCLAIMED("day < ? AND status = ${DailyTaskStatus.COMPLETED.code} ORDER BY day DESC, task_id LIMIT 50"),
    }

    override fun taskList(): List<DailyTask> = sql.select(TASKS).rows(ResultSet::task).filterNotNull()

    override fun dayRows(
        playerId: Int,
        day: LocalDate,
    ): List<DailyTaskProgress> = rowsIn(Span.THAT_DAY, playerId, day)

    override fun leftoverClaims(
        playerId: Int,
        day: LocalDate,
    ): List<DailyTaskProgress> = rowsIn(Span.EARLIER_UNCLAIMED, playerId, day)

    override fun record(
        playerId: Int,
        progress: DailyTaskProgress,
        now: Int,
    ) {
        val (task, day, repeats) = progress
        sql.write(RECORD_SQL, playerId, task, day.toString(), repeats, progress.status.code, now)
    }

    private fun rowsIn(
        span: Span,
        playerId: Int,
        day: LocalDate,
    ): List<DailyTaskProgress> =
        sql.select(PLAYER_ROWS + span.selection, playerId, day.toString()).rows(ResultSet::progress)

    private companion object {
        const val TASKS = "SELECT * FROM daily_tasks WHERE enabled = 1 ORDER BY sort_order, id"
        const val PLAYER_ROWS = "SELECT task_id, day, repeats, status FROM player_daily_tasks WHERE player_id = ? AND "
        const val RECORD_SQL =
            "INSERT INTO player_daily_tasks (player_id, task_id, day, repeats, status, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                "repeats = GREATEST(repeats, VALUES(repeats)), status = GREATEST(status, VALUES(status)), " +
                "updated_at = VALUES(updated_at)"
    }
}

private fun ResultSet.progress() =
    DailyTaskProgress(
        getInt("task_id"),
        getObject("day", LocalDate::class.java),
        getInt("repeats"),
        DailyTaskStatus.fromCode(getInt("status")),
    )

private fun ResultSet.task(): DailyTask? {
    val counted = CountedAction.named(getString("goal_type")) ?: return null
    return DailyTask(
        getInt("id"),
        getString("code"),
        QuestGoal(counted, getString("goal_data").orEmpty(), getInt("required_repeats")),
        getBoolean("is_bonus"),
        DailyTask.StoredReward(
            getString("reward_type").orEmpty(),
            getString("reward_extra").orEmpty(),
            getInt("reward_amount"),
        ),
        getString("catalogue_name").orEmpty(),
        getString("image_version").orEmpty(),
        getInt("sort_order"),
    )
}
