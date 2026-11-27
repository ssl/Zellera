package zel.game.players.inventory

import org.slf4j.LoggerFactory
import zel.db.Database
import zel.db.SqlFailure
import zel.game.players.level.AccountStanding
import zel.game.players.level.AccountStandings

private val log = LoggerFactory.getLogger(BadgeLeaderboard::class.java)

private const val HOLDERS_SQL =
    "SELECT p.id, p.name, p.figure, b.badge_code FROM player_badges b JOIN players p ON p.id = b.player_id"
private const val CACHE_MS = 60_000L

// level-ups go on all the time on a busy game; each reload reads every badge row
private const val RELOAD_GAP_MS = 5_000L

class BadgeLeaderboard(
    private val database: Database,
    private val standingOf: (Int) -> AccountStanding = AccountStandings(database)::of,
) {
    data class Entry(
        val userId: Int,
        val username: String,
        val figure: String,
        val score: Int,
    )

    class Board(
        private val ranked: List<Entry>,
    ) {
        private val indexByUser = ranked.withIndex().associate { (index, entry) -> entry.userId to index }

        fun total(): Int = minOf(ranked.size, MAX_RANKED)

        fun page(
            page: Int,
            size: Int,
        ): List<Entry> {
            val from = page * size
            val to = minOf(total(), from + size)
            return if (from in 0 until to) ranked.subList(from, to) else emptyList()
        }

        /** 1 for the top; -1 outside the ranked part. */
        fun rankOf(userId: Int): Int = indexByUser[userId]?.takeIf { it < MAX_RANKED }?.plus(1) ?: -1

        fun scoreOf(userId: Int): Int = indexByUser[userId]?.let { ranked[it].score } ?: 0
    }

    data class Holder(
        val userId: Int,
        val username: String,
        val figure: String,
        val badges: Set<String>,
    )

    private val boards = TimedSnapshot<Map<String, Board>>(CACHE_MS, emptyMap(), RELOAD_GAP_MS, ::load)

    fun board(
        type: Int,
        rarity: Int,
    ): Board? = key(type, rarity)?.let { boards.get()[it] }

    fun totalBadgesRank(userId: Int): Int = board(TYPE_TOTAL_BADGES, -1)?.rankOf(userId) ?: -1

    fun accountStanding(userId: Int): AccountStanding = standingOf(userId)

    fun invalidate() = boards.invalidate()

    private fun load(): Map<String, Board>? =
        try {
            build(holders())
        } catch (e: SqlFailure) {
            log.error("The badge boards could not be read", e)
            null
        }

    private fun holders(): List<Holder> =
        database.queries.select(HOLDERS_SQL).result { rows ->
            val names = LinkedHashMap<Int, Pair<String, String>>()
            val badges = HashMap<Int, MutableSet<String>>()
            while (rows.next()) {
                val userId = rows.getInt("id")
                names.getOrPut(userId) { rows.getString("name") to rows.getString("figure") }
                badges.getOrPut(userId, ::HashSet) += rows.getString("badge_code")
            }
            names.map { (userId, named) -> Holder(userId, named.first, named.second, badges[userId].orEmpty()) }
        }

    companion object {
        const val TYPE_TOTAL_BADGES = 0
        const val TYPE_RARITY = 1
        const val TYPE_ACHIEVEMENT_LEVEL = 2
        const val MAX_RANKED = 500

        private const val KEY_TOTAL = "total"
        private const val KEY_LEVEL = "level"
        private val ACHIEVEMENT_BADGE = Regex("^ACH_(.*?)(\\d+)$")

        // a longer level is no real one (a badge given by hand): it counts nothing, as on the site
        private const val MAX_LEVEL_DIGITS = 4

        fun key(
            type: Int,
            rarity: Int,
        ): String? =
            when (type) {
                TYPE_TOTAL_BADGES -> KEY_TOTAL
                TYPE_RARITY -> rarity.takeIf { it in BadgeTier.codes }?.let { "rarity:$it" }
                TYPE_ACHIEVEMENT_LEVEL -> KEY_LEVEL
                else -> null
            }

        fun build(holders: List<Holder>): Map<String, Board> {
            val owners = HashMap<String, Int>()
            holders.forEach { holder -> holder.badges.forEach { owners[it] = (owners[it] ?: 0) + 1 } }
            val scores = HashMap<String, MutableMap<Int, Int>>()

            fun put(
                key: String,
                userId: Int,
                score: Int,
            ) {
                if (score > 0) scores.getOrPut(key, ::HashMap)[userId] = score
            }
            holders.forEach { holder ->
                put(KEY_TOTAL, holder.userId, holder.badges.size)
                put(KEY_LEVEL, holder.userId, achievementLevel(holder.badges))
                holder.badges
                    .groupingBy { BadgeTier.forHolders(owners[it] ?: 0).code }
                    .eachCount()
                    .forEach { (tier, count) -> put("rarity:$tier", holder.userId, count) }
            }
            val byUser = holders.associateBy(Holder::userId)
            val keys = listOf(KEY_TOTAL, KEY_LEVEL) + BadgeTier.codes.map { "rarity:$it" }
            return keys.associateWith { key ->
                val ranked =
                    scores[key]
                        .orEmpty()
                        .entries
                        .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
                        .take(MAX_RANKED)
                        .map { (userId, score) ->
                            val holder = byUser.getValue(userId)
                            Entry(userId, holder.username, holder.figure, score)
                        }
                Board(ranked)
            }
        }

        fun achievementLevel(badges: Set<String?>): Int {
            val levels = HashMap<String, Int>()
            badges.filterNotNull().forEach { code ->
                ACHIEVEMENT_BADGE.matchEntire(code)?.let { match ->
                    val (name, digits) = match.destructured
                    val level = digits.takeIf { it.length <= MAX_LEVEL_DIGITS }?.toIntOrNull() ?: return@forEach
                    levels[name] = maxOf(levels[name] ?: 0, level)
                }
            }
            return levels.values.sum()
        }
    }
}
