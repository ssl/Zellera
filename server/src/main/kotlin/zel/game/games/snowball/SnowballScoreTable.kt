@file:Suppress("MagicNumber") // JDBC parameter and column positions

package zel.game.games.snowball

import org.slf4j.LoggerFactory
import zel.db.Sql
import zel.db.allOrNothing
import zel.db.orFallback
import zel.db.writeEach
import zel.game.games.snowball.SnowballScoreStore.Profile
import zel.game.games.snowball.SnowballScoreStore.ScoreRow
import java.security.SecureRandom
import java.sql.Date
import java.sql.ResultSet
import java.time.Clock
import java.time.LocalDate
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(SnowballScoreTable::class.java)
private val seeds = SecureRandom()

private const val ADD_TO_WEEK_SQL =
    "INSERT INTO snowball_week_scores (week_start, player_id, score) VALUES (?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE score = score + VALUES(score)"
private const val ADD_TO_TOTAL_SQL =
    "INSERT INTO snowball_totals (player_id, score, games) VALUES (?, ?, 1) " +
        "ON DUPLICATE KEY UPDATE score = score + VALUES(score), games = games + 1"
private const val GAMES_PLAYED_SQL = "SELECT games FROM snowball_totals WHERE player_id = ?"
private const val FIRST_WEEK_SQL = "SELECT MIN(week_start) FROM snowball_week_scores"
private const val FAVOURITE_GROUP_SQL = "SELECT favourite_guild_id FROM player_stats WHERE player_id = ? LIMIT 1"
private const val WEEK_SETTLED_SQL = "SELECT 1 FROM snowball_weeks WHERE week_start = ?"
private const val WEEK_SEED_SQL = "SELECT draw_seed FROM snowball_weeks WHERE week_start = ?"
private const val SETTLE_WEEK_SQL =
    "INSERT IGNORE INTO snowball_weeks (week_start, processed_at, draw_seed) VALUES (?, UNIX_TIMESTAMP(), ?)"
private const val PLAYER_NAMES_SQL = "SELECT id, name, figure, gender FROM players WHERE id IN "
private const val GROUP_NAMES_SQL = "SELECT id, name, badge_code FROM guilds WHERE id IN "

private const val OF_WEEK = "SELECT b.player_id, b.score FROM snowball_week_scores b WHERE b.week_start = ?"
private const val OF_ALL_TIME = "SELECT b.player_id, b.score FROM snowball_totals b WHERE b.score > 0"
private const val AMONG_FRIENDS =
    " AND (b.player_id = ? OR b.player_id IN (SELECT friend_id FROM friendships WHERE player_id = ?))"
private const val BEST_FIRST = " ORDER BY b.score DESC, b.player_id"

private const val GROUP_SUMS = "SELECT s.favourite_guild_id, SUM(b.score) AS total FROM "
private const val BY_FAVOURITE_GROUP =
    " b INNER JOIN player_stats s ON s.player_id = b.player_id INNER JOIN guilds g ON g.id = s.favourite_guild_id"
private const val GROUPS_BEST_FIRST =
    " GROUP BY s.favourite_guild_id HAVING total > 0 ORDER BY total DESC, s.favourite_guild_id"
private const val GROUP_SCORES_SQL = GROUP_SUMS + "snowball_totals" + BY_FAVOURITE_GROUP + GROUPS_BEST_FIRST
private const val GROUP_WEEK_SCORES_SQL =
    GROUP_SUMS + "snowball_week_scores" + BY_FAVOURITE_GROUP + " WHERE b.week_start = ?" + GROUPS_BEST_FIRST
private const val GROUP_SCORERS_SQL =
    "SELECT b.player_id FROM snowball_week_scores b INNER JOIN player_stats s ON s.player_id = b.player_id " +
        "WHERE b.week_start = ? AND s.favourite_guild_id = ? ORDER BY b.player_id"

/**
 * A week's rows carry the weekly boards and prizes; the totals carry the all-time boards and the games count, so
 * neither is summed over every week ever played. A match adds to both or to neither.
 */
class SnowballScoreTable internal constructor(
    private val pool: DataSource,
    private val clock: Clock,
) : SnowballScoreStore {
    private val sql = Sql(pool)

    fun gamesPlayed(playerId: Int): Int =
        log.orFallback("the Snowball games of player $playerId", 0) {
            sql.select(GAMES_PLAYED_SQL, playerId).first { it.getInt(1) } ?: 0
        }

    /** A match without points still counts as a game, and gets no row in the week. */
    fun recordScores(scoreByPlayer: Map<Int, Int>) {
        if (scoreByPlayer.isEmpty()) return
        val week = Date.valueOf(SnowballLeaderboards.weekStart(clock, 0))
        val scores = scoreByPlayer.mapValues { maxOf(0, it.value) }
        log.orFallback("the Snowball match scores", Unit) {
            pool.connection.use { connection ->
                connection.allOrNothing {
                    connection.writeEach(ADD_TO_WEEK_SQL, scores.filterValues { it > 0 }.entries) { (playerId, score) ->
                        value(week)
                        int(playerId)
                        int(score)
                    }
                    connection.writeEach(ADD_TO_TOTAL_SQL, scores.entries) { (playerId, score) ->
                        int(playerId)
                        int(score)
                    }
                }
            }
        }
    }

    override fun userScores(
        week: LocalDate?,
        friendsOf: Int,
    ): List<ScoreRow> {
        val circle = if (friendsOf > 0) listOf(friendsOf, friendsOf) else emptyList()
        val board = if (week == null) OF_ALL_TIME else OF_WEEK
        val query = board + (if (circle.isEmpty()) "" else AMONG_FRIENDS) + BEST_FIRST
        val params = listOfNotNull(week?.let(Date::valueOf)) + circle
        return scoreRows(query, params, "the Snowball leaderboard")
    }

    override fun groupScores(week: LocalDate?): List<ScoreRow> =
        scoreRows(
            if (week == null) GROUP_SCORES_SQL else GROUP_WEEK_SCORES_SQL,
            listOfNotNull(week?.let(Date::valueOf)),
            "the Snowball group leaderboard",
        )

    override fun groupScorers(
        groupId: Int,
        week: LocalDate,
    ): List<Int> =
        log.orFallback("the Snowball players of group $groupId", emptyList()) {
            sql.select(GROUP_SCORERS_SQL, Date.valueOf(week), groupId).rows { it.getInt(1) }
        }

    override fun userProfiles(userIds: Collection<Int>): Map<Int, Profile> =
        namesOf(PLAYER_NAMES_SQL, userIds) { Profile(it.getString(2), it.getString(3).orEmpty(), it.getString(4)) }

    override fun groupProfiles(groupIds: Collection<Int>): Map<Int, Profile> =
        namesOf(GROUP_NAMES_SQL, groupIds) { Profile(it.getString(2), it.getString(3).orEmpty(), "g") }

    override fun favouriteGroup(userId: Int): Int =
        log.orFallback("the favourite group for the Snowball group leaderboard", 0) {
            sql.select(FAVOURITE_GROUP_SQL, userId).first { it.getInt(1) } ?: 0
        }

    override fun firstWeek(): LocalDate? =
        log.orFallback("the Snowball leaderboard history", null) {
            sql.select(FIRST_WEEK_SQL).first { it.getDate(1)?.toLocalDate() }
        }

    /** Unknown counts as settled, so a database problem never hands the prizes out twice. */
    override fun weekSettled(week: LocalDate): Boolean =
        log.orFallback("the Snowball weekly results of $week", true) {
            sql.select(WEEK_SETTLED_SQL, Date.valueOf(week)).rows { }.isNotEmpty()
        }

    override fun settleWeek(week: LocalDate): Boolean =
        log.orFallback("the settling of Snowball week $week", false) {
            sql.write(SETTLE_WEEK_SQL) {
                value(Date.valueOf(week))
                long(seeds.nextLong())
            } == 1
        }

    override fun weekSeed(week: LocalDate): Long =
        log.orFallback("the Snowball draw seed of $week", 0L) {
            sql.select(WEEK_SEED_SQL, Date.valueOf(week)).first { it.getLong(1) } ?: 0L
        }

    private fun scoreRows(
        query: String,
        params: List<Any>,
        what: String,
    ): List<ScoreRow> =
        log.orFallback(what, emptyList()) {
            sql.select(query) { params.forEach { value(it) } }.rows { ScoreRow(it.getInt(1), it.getLong(2)) }
        }

    private fun namesOf(
        selectIn: String,
        ids: Collection<Int>,
        profile: (ResultSet) -> Profile,
    ): Map<Int, Profile> {
        val wanted = ids.filter { it > 0 }.distinct()
        if (wanted.isEmpty()) return emptyMap()
        val placeholders = wanted.joinToString(",", "(", ")") { "?" }
        return log.orFallback("the Snowball leaderboard names", emptyMap()) {
            sql
                .select(selectIn + placeholders) { wanted.forEach { int(it) } }
                .rows { it.getInt(1) to profile(it) }
                .toMap()
        }
    }
}
