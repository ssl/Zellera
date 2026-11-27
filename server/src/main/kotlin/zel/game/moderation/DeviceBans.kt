package zel.game.moderation

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import zel.core.Configuration
import zel.db.Params
import zel.db.Sql
import zel.db.orFallback
import zel.game.players.DeviceReading
import zel.game.players.Player
import javax.sql.DataSource

/**
 * Keeps a device out: one a banned player logged in from lately (rule 1 by its id, rule 2 by graphics card, screen
 * and fonts when hardly anyone else has the three), or one a pattern of staff names. Pieces count when they are
 * equal; one the login does not give or cannot trust equals nothing. An exempt player is never refused.
 */
class DeviceBans(
    dataSource: DataSource,
    private val config: Configuration,
) {
    private val sql = Sql(dataSource)

    /** What refuses a login of [playerId] from [device]: `rule 1`, `rule 2` or `pattern <number>`; null lets it in. */
    fun refusal(
        playerId: Int,
        device: DeviceReading,
        now: Int,
    ): String? = if (exempt(playerId)) null else byBannedPlayer(playerId, device, now) ?: byPattern(device, now)

    /** The players of [online] a ban on [bannedId] keeps out from now on, by rule 1 or 2. */
    fun keptOutBy(
        bannedId: Int,
        online: Collection<Player>,
        now: Int,
    ): List<Player> =
        log.orFallback("devices of banned player $bannedId", emptyList()) {
            val devices = sql.select(DEVICES_OF, bannedId, since(now)).rows { it.getString(1) to triple(it::getString) }
            val ids: Set<String?> = devices.map { it.first }.toSet()
            val triples = devices.map { it.second }.toSet()
            online.filter { player ->
                val id = player.profile.id
                val device = player.device
                id != bannedId && device != null &&
                    (device.usable("id") in ids || triple(device::usable) in triples) &&
                    !exempt(id) && byBannedPlayer(id, device, now, bannedId) != null
            }
        }

    private fun exempt(playerId: Int): Boolean = sql.select(EXEMPT, playerId).first { true } == true

    /** With [bannedId] only that player's devices count, else those of every banned player. */
    private fun byBannedPlayer(
        playerId: Int,
        device: DeviceReading,
        now: Int,
        bannedId: Int? = null,
    ): String? {
        val only = if (bannedId == null) "" else " AND d.player_id = $bannedId"
        val id = device.usable("id")
        val triple = triple(device::usable)
        val lately: Params.() -> Unit = {
            int(playerId)
            long(window())
            int(now)
        }
        return when {
            id != null &&
                found(RULE_1 + only) {
                    string(id)
                    lately()
                }
            -> {
                "rule 1"
            }

            null !in triple &&
                found(RULE_2 + only) {
                    triple.forEach(::string)
                    lately()
                    triple.forEach(::string)
                    int(playerId)
                    int(now)
                    int(others())
                }
            -> {
                "rule 2"
            }

            else -> {
                null
            }
        }
    }

    private fun byPattern(
        device: DeviceReading,
        now: Int,
    ): String? =
        sql
            .select(PATTERN) {
                int(now)
                DeviceReading.NAMES.forEach { string(device.usable(it)) }
            }.first { "pattern ${it.getInt(1)}" }

    private fun found(
        query: String,
        bind: Params.() -> Unit,
    ): Boolean = sql.select("$query LIMIT 1", bind).first { true } == true

    private fun triple(piece: (String) -> String?): List<String?> = TRIPLE.map(piece)

    private fun window(): Long = config.getInt(DAYS, DEFAULT_DAYS) * DAY_SECONDS

    private fun since(now: Int): Long = maxOf(0L, now - window())

    private fun others(): Int = config.getInt(OTHERS, 0)

    private companion object {
        val log: Logger = LoggerFactory.getLogger(DeviceBans::class.java)
        val TRIPLE = listOf("gpu", "scr", "fnt")
        const val DAYS = "login.device.ban.days"
        const val OTHERS = "login.device.ban.others"
        const val DEFAULT_DAYS = 30
        const val DAY_SECONDS = 86_400L
        const val EXEMPT = "SELECT 1 FROM device_ban_exemptions WHERE player_id = ?"
        const val DEVICES_OF = "SELECT id, gpu, scr, fnt FROM player_devices WHERE player_id = ? AND last_seen_at >= ?"

        // a device of another player whose account is banned now, seen in the days before that ban or since
        const val RULE_1 =
            "SELECT 1 FROM player_devices d JOIN bans b ON b.player_id = d.player_id " +
                "WHERE d.id = ? AND d.player_id <> ? AND d.last_seen_at + ? >= b.issued_at AND b.ends_at > ? " +
                "AND b.avatar_only = 0"

        // the same, and few enough players who are not banned themselves share the three pieces
        const val RULE_2 =
            "SELECT 1 FROM player_devices d JOIN bans b ON b.player_id = d.player_id " +
                "WHERE d.gpu = ? AND d.scr = ? AND d.fnt = ? AND d.player_id <> ? " +
                "AND d.last_seen_at + ? >= b.issued_at AND b.ends_at > ? AND b.avatar_only = 0 " +
                "AND (SELECT COUNT(DISTINCT o.player_id) FROM player_devices o " +
                "WHERE o.gpu = ? AND o.scr = ? AND o.fnt = ? AND o.player_id <> ? AND NOT EXISTS " +
                "(SELECT 1 FROM bans x WHERE x.player_id = o.player_id AND x.ends_at > ?)) <= ?"
        val PATTERN =
            "SELECT id FROM device_bans WHERE (expires_at IS NULL OR expires_at > ?)" +
                DeviceReading.NAMES.joinToString("") { " AND (p_$it IS NULL OR p_$it = ?)" } +
                " ORDER BY id LIMIT 1"
    }
}
