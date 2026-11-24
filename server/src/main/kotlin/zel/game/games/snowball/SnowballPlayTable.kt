package zel.game.games.snowball

import zel.db.Sql
import zel.db.orFallback
import org.slf4j.LoggerFactory
import java.sql.Date
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import javax.sql.DataSource

private val log = LoggerFactory.getLogger(SnowballPlayTable::class.java)

private const val PLAYED_TODAY_SQL = "SELECT games FROM snowball_plays WHERE player_id = ? AND day = ?"
private const val RECORD_PLAY_SQL =
    "INSERT INTO snowball_plays (player_id, day, games) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE games = games + 1"
private const val TOKENS_SQL = "SELECT tokens FROM player_game_tokens WHERE player_id = ?"
private const val ADD_TOKENS_SQL =
    "INSERT INTO player_game_tokens (player_id, tokens) VALUES (?, ?) " +
        "ON DUPLICATE KEY UPDATE tokens = tokens + VALUES(tokens)"
private const val TAKE_TOKEN_SQL =
    "UPDATE player_game_tokens SET tokens = tokens - 1 WHERE player_id = ? AND tokens > 0"

/** A read that fails counts as nothing left. */
class SnowballPlayTable internal constructor(
    pool: DataSource,
    private val clock: Clock,
) : SnowballPlayStore {
    private val sql = Sql(pool)

    private fun today(): Date = Date.valueOf(LocalDate.now(clock.withZone(ZoneOffset.UTC)))

    override fun playedToday(playerId: Int): Int =
        log.orFallback("the Snowball games of player $playerId today", Int.MAX_VALUE) {
            sql.select(PLAYED_TODAY_SQL, playerId, today()).first { it.getInt(1) } ?: 0
        }

    override fun recordPlay(playerId: Int) {
        log.orFallback("counting a Snowball game of player $playerId", Unit) {
            sql.write(RECORD_PLAY_SQL, playerId, today())
        }
    }

    override fun tokens(playerId: Int): Int =
        log.orFallback("the Snowball tokens of player $playerId", 0) {
            sql.select(TOKENS_SQL, playerId).first { it.getInt(1) } ?: 0
        }

    override fun addTokens(
        playerId: Int,
        games: Int,
    ): Boolean =
        log.orFallback("adding $games Snowball tokens to player $playerId", false) {
            sql.write(ADD_TOKENS_SQL, playerId, games) > 0
        }

    override fun takeToken(playerId: Int): Boolean =
        log.orFallback("taking a Snowball token of player $playerId", false) {
            sql.write(TAKE_TOKEN_SQL, playerId) == 1
        }
}
