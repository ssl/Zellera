package zel.game.quests

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.orFallback
import java.sql.ResultSet
import javax.sql.DataSource

/** A quest whose goal nothing reports is left out with a warning. */
class StoredQuests(
    dataSource: DataSource,
) : QuestRecords {
    private val sql = Sql(dataSource)

    override fun definitions(): List<Quest> =
        log.orFallback("quest storage", emptyList()) {
            sql
                .select(ENABLED_QUESTS)
                .rows(::questOf)
                .filterNotNull()
        }

    /** Throws [zel.db.SqlFailure] rather than give no rows: an empty state would let done quests be paid again. */
    override fun rowsOf(playerId: Int): List<QuestEntry> =
        sql
            .select(
                "SELECT quest_id, progress, tracked_since, completed_at FROM player_quests WHERE player_id = ?",
                playerId,
            ).rows { row ->
                QuestEntry(
                    row.getInt("quest_id"),
                    row.getInt("progress"),
                    row.getInt("tracked_since"),
                    row.getInt("completed_at"),
                )
            }

    override fun store(
        playerId: Int,
        entry: QuestEntry,
    ) {
        log.orFallback("quest storage", Unit) {
            sql.write(
                "INSERT INTO player_quests (player_id, quest_id, progress, tracked_since, completed_at) " +
                    "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE progress = VALUES(progress), " +
                    "tracked_since = VALUES(tracked_since), completed_at = VALUES(completed_at)",
                playerId,
                entry.quest,
                entry.progress,
                entry.trackedSince.takeIf { it > 0 },
                entry.completedAt.takeIf { it > 0 },
            )
        }
    }

    private fun questOf(row: ResultSet): Quest? {
        val goalCode = row.getString("goal_type")
        val goal =
            CountedAction.named(goalCode) ?: run {
                log.warn("quest {} counts '{}', which nothing reports; left out", row.getInt("id"), goalCode)
                return null
            }
        return Quest(
            id = row.getInt("id"),
            code = row.getString("code"),
            slot =
                Quest.Slot(
                    campaign = row.getString("campaign_code"),
                    chain = row.getString("chain_code"),
                    position = row.getInt("sort_order"),
                ),
            goal = QuestGoal(goal, row.getString("goal_data"), row.getInt("goal_count")),
            reward = QuestReward.Currency(row.getInt("reward_currency"), row.getInt("reward_amount")),
            badge = row.getString("reward_badge"),
            isEasy = row.getBoolean("is_easy"),
            isDaily = row.getBoolean("is_daily"),
            deadline = row.getInt("seasonal_ends_at"),
            display =
                Quest.Display(
                    catalogPage = row.getString("catalogue_page_name"),
                    imageVersion = row.getString("image_version"),
                ),
        )
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(StoredQuests::class.java)

        const val ENABLED_QUESTS =
            "SELECT id, code, campaign_code, chain_code, sort_order, goal_type, goal_data, goal_count," +
                " reward_currency, reward_amount, reward_badge, is_easy, is_daily, seasonal_ends_at," +
                " catalogue_page_name, image_version FROM quests WHERE enabled = TRUE" +
                " ORDER BY campaign_code, sort_order, id"
    }
}
