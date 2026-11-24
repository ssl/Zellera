package zel.game.achievements

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.db.Sql
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

internal class AchievementStore(
    private val dataSource: DataSource,
) {
    private val sql = Sql(dataSource)

    /** An achievement with an unknown category is left out. */
    fun loadAll(owner: AchievementProgress): Map<String, Achievement> {
        val drafts = mutableMapOf<String, Draft>()
        sql.select(SELECT_DEFINITIONS).each { set ->
            val code: String = set.getString("code")
            val draft =
                drafts[code] ?: AchievementCategory.ofCode(set.getString("category"))?.let { category ->
                    Draft(set, category).also { drafts[code] = it }
                }
            if (draft == null) {
                LOGGER.warn("Achievement {} has an unknown category {}; left out", code, set.getString("category"))
            } else {
                draft.ladder += levelOf(set)
            }
        }
        return drafts.mapValues { (_, draft) -> draft.build(owner) }
    }

    fun loadTriggers(): List<AchievementTrigger> =
        sql.select(SELECT_TRIGGERS).rows {
            AchievementTrigger(it.getInt("achievement_id"), it.getString("event"), it.getString("detail").orEmpty())
        }

    /** Counts for a player who is away: the progress moves now, the levels it reaches wait for the next login. */
    fun countAway(
        playerId: Int,
        achievement: Achievement,
        amount: Int,
    ) {
        try {
            dataSource.connection.use { AchievementRewards.count(it, playerId, achievement, amount) }
        } catch (exception: SQLException) {
            LOGGER.error("Could not count achievement {} of player {}", achievement.id, playerId, exception)
        }
    }

    /** Per achievement id the stored progress and the level handed out for it. */
    fun standing(playerId: Int): Map<Int, Standing> =
        sql
            .select(SELECT_STANDING, playerId)
            .rows { it.getInt("achievement_id") to Standing(it.getInt("progress"), it.getInt("rewarded_level")) }
            .toMap()

    data class Standing(
        val progress: Int,
        val rewardedLevel: Int,
    )

    private class Draft(
        set: ResultSet,
        val category: AchievementCategory,
    ) {
        val id = set.getInt("id")
        val code: String = set.getString("code")
        val state = set.getInt("state").toShort()
        val shownAs = set.getInt("display_method")
        val subcategory = set.getString("subcategory").orEmpty()
        val ladder = ArrayList<AchievementLevel>()

        fun build(owner: AchievementProgress): Achievement {
            val details = Achievement.Details(state, shownAs, subcategory, ladder.associateBy { it.level })
            return Achievement(id, code, category, details, owner)
        }
    }

    private companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(AchievementStore::class.java)

        fun levelOf(set: ResultSet) =
            AchievementLevel(
                level = set.getInt("level"),
                progressNeeded = set.getInt("progress_needed"),
                scorePoints = set.getInt("score_points"),
                rewardCurrency = set.getInt("reward_currency"),
                rewardAmount = set.getInt("reward_amount"),
                notificationId = set.getInt("notification_id"),
                bonusPoints = set.getInt("bonus_points"),
            )

        const val SELECT_DEFINITIONS =
            "SELECT a.id, a.code, a.category, a.subcategory, a.state, a.display_method, l.level, " +
                "l.notification_id, l.progress_needed, l.score_points, l.reward_currency, l.reward_amount, " +
                "l.bonus_points FROM achievements a " +
                "JOIN achievement_levels l ON l.achievement_id = a.id ORDER BY a.code, l.level"

        const val SELECT_TRIGGERS = "SELECT achievement_id, event, detail FROM achievement_triggers"

        const val SELECT_STANDING =
            "SELECT achievement_id, progress, rewarded_level FROM player_achievements WHERE player_id = ?"
    }
}
