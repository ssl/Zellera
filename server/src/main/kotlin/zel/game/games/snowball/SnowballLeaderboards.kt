package zel.game.games.snowball

import zel.game.games.snowball.SnowballScoreStore.Profile
import zel.game.games.snowball.SnowballScoreStore.ScoreRow
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.concurrent.ConcurrentHashMap

/** The Snowball boards; each board's rows are read once per [CACHE_MILLIS] and shared by every viewer and page. */
class SnowballLeaderboards(
    private val store: SnowballScoreStore,
    private val clock: Clock,
) {
    private class Cached(
        val value: Any?,
        val readAt: Long,
    )

    private data class BoardKey(
        val board: String,
        val week: LocalDate?,
        val viewerId: Int,
    )

    private val cache = ConcurrentHashMap<BoardKey, Cached>()

    data class Entry(
        val userId: Int,
        val score: Int,
        val rank: Int,
        val name: String,
        val figure: String,
        val gender: String,
    )

    data class Page(
        val year: Int,
        val week: Int,
        val maxOffset: Int,
        val currentOffset: Int,
        val minutesUntilReset: Int,
        val entries: List<Entry>,
        val totalListSize: Int,
        val gameTypeId: Int,
    )

    /** [favouriteGroupId] 0 means no favourite group and no own row. */
    data class GroupPage(
        val page: Page,
        val favouriteGroupId: Int,
    )

    fun total(
        viewerId: Int,
        friends: Boolean,
        startRank: Int,
        windowSize: Int,
    ): Page = userPage(viewerId, friends, null, 0, 0, startRank, windowSize)

    fun weekly(
        viewerId: Int,
        friends: Boolean,
        weekOffset: Int,
        startRank: Int,
        windowSize: Int,
    ): Page {
        val maxOffset = maxWeekOffset()
        val offset = maxOf(0, minOf(weekOffset, maxOffset))
        return userPage(viewerId, friends, weekStart(clock, offset), maxOffset, offset, startRank, windowSize)
    }

    fun groups(
        viewerId: Int,
        weekly: Boolean,
        weekOffset: Int,
        startRank: Int,
        windowSize: Int,
    ): GroupPage {
        val maxOffset = if (weekly) maxWeekOffset() else 0
        val offset = if (weekly) maxOf(0, minOf(weekOffset, maxOffset)) else 0
        val week = if (weekly) weekStart(clock, offset) else null
        val rows = cached(BoardKey("groups", week, 0)) { store.groupScores(week) }
        val favourite = maxOf(0, store.favouriteGroup(viewerId))
        val ownIndex = indexOf(rows, favourite)
        val start = windowStart(rows.size, ownIndex, startRank, windowSize, true)
        val end = minOf(rows.size, start + window(windowSize))

        val ids = rows.subList(start, end).map { it.id } + listOfNotNull(favourite.takeIf { it > 0 })
        val profiles = store.groupProfiles(ids)
        val entries =
            rows
                .subList(
                    start,
                    end,
                ).mapIndexedTo(mutableListOf()) { i, row -> entry(row, start + i + 1, profiles[row.id], "g") }
        if (favourite > 0) {
            // popped by the receiver as the viewer's group row (only when favouriteGroupId > 0)
            entries.add(ownRow(rows, ownIndex, favourite, profiles[favourite], "g"))
        }
        return GroupPage(page(week, maxOffset, offset, entries, rows.size), favourite)
    }

    private fun userPage(
        viewerId: Int,
        friends: Boolean,
        week: LocalDate?,
        maxOffset: Int,
        offset: Int,
        startRank: Int,
        windowSize: Int,
    ): Page {
        val friendsOf = if (friends) viewerId else 0
        val rows = cached(BoardKey("users", week, friendsOf)) { store.userScores(week, friendsOf) }
        val ownIndex = indexOf(rows, viewerId)
        val start = windowStart(rows.size, ownIndex, startRank, windowSize, friends)
        val end = minOf(rows.size, start + window(windowSize))

        val ids = rows.subList(start, end).map { it.id } + listOfNotNull(viewerId.takeIf { !friends })
        val profiles = store.userProfiles(ids)
        val entries =
            rows
                .subList(
                    start,
                    end,
                ).mapIndexedTo(mutableListOf()) { i, row -> entry(row, start + i + 1, profiles[row.id], "") }
        if (!friends) {
            // popped by the receiver on every reply and pinned under the list as the viewer's own row
            entries.add(ownRow(rows, ownIndex, viewerId, profiles[viewerId], ""))
        }
        return page(week, maxOffset, offset, entries, rows.size)
    }

    private fun page(
        week: LocalDate?,
        maxOffset: Int,
        offset: Int,
        entries: List<Entry>,
        total: Int,
    ): Page {
        val shown = week ?: weekStart(clock, 0)
        return Page(
            shown.get(WeekFields.ISO.weekBasedYear()),
            shown.get(WeekFields.ISO.weekOfWeekBasedYear()),
            maxOffset,
            offset,
            minutesUntilReset(clock),
            entries.toList(),
            total,
            GAME_TYPE,
        )
    }

    private fun maxWeekOffset(): Int {
        val first = cached(BoardKey("first week", null, 0)) { store.firstWeek() } ?: return 0
        val weeks = ChronoUnit.WEEKS.between(first, weekStart(clock, 0))
        return maxOf(0L, minOf(WEEKS_KEPT.toLong(), weeks)).toInt()
    }

    /** What [read] gives, kept for [CACHE_MILLIS]; a full cache first drops what is stale and else stores nothing. */
    private fun <T> cached(
        key: BoardKey,
        read: () -> T,
    ): T {
        val now = clock.millis()
        cache[key]?.takeIf { now - it.readAt < CACHE_MILLIS }?.let {
            @Suppress("UNCHECKED_CAST") // a key's board always holds the type its reader returns
            return it.value as T
        }
        val value = read()
        if (cache.size >= MAX_CACHED) cache.values.removeIf { now - it.readAt >= CACHE_MILLIS }
        if (cache.size < MAX_CACHED) cache[key] = Cached(value, now)
        return value
    }

    companion object {
        const val GAME_TYPE = 0

        internal const val CACHE_MILLIS = 5_000L

        private const val MAX_CACHED = 1_000

        const val WEEKS_KEPT = 4

        internal const val MAX_WINDOW = 100

        /** First row index of a reply: [startRank] 1..n is that rank; below 1 it is rank 1, or the own row's window. */
        internal fun windowStart(
            rows: Int,
            ownIndex: Int,
            startRank: Int,
            windowSize: Int,
            aroundOwn: Boolean,
        ): Int {
            val window = window(windowSize)
            return when {
                startRank >= 1 -> minOf(rows, startRank - 1)
                aroundOwn && ownIndex >= 0 -> ownIndex / window * window
                else -> 0
            }
        }

        internal fun window(windowSize: Int): Int = maxOf(1, minOf(MAX_WINDOW, windowSize))

        private fun indexOf(
            rows: List<ScoreRow>,
            id: Int,
        ): Int = if (id <= 0) -1 else rows.indexOfFirst { it.id == id }

        private fun entry(
            row: ScoreRow,
            rank: Int,
            profile: Profile?,
            gender: String,
        ): Entry {
            val shown = profile ?: Profile("", "", gender)
            return Entry(row.id, clampScore(row.score), rank, shown.name, shown.figure, shown.gender)
        }

        private fun ownRow(
            rows: List<ScoreRow>,
            ownIndex: Int,
            id: Int,
            profile: Profile?,
            gender: String,
        ): Entry =
            if (ownIndex >= 0) {
                entry(rows[ownIndex], ownIndex + 1, profile, gender)
            } else {
                entry(ScoreRow(id, 0), 0, profile, gender)
            }

        internal fun clampScore(score: Long): Int = minOf(Int.MAX_VALUE.toLong(), maxOf(0L, score)).toInt()

        internal fun weekStart(
            clock: Clock,
            offset: Int,
        ): LocalDate =
            LocalDate
                .now(clock.withZone(ZoneOffset.UTC))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(maxOf(0, offset).toLong())

        internal fun minutesUntilReset(clock: Clock): Int {
            val now = ZonedDateTime.now(clock.withZone(ZoneOffset.UTC))
            val reset = now.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay(ZoneOffset.UTC)
            return maxOf(0, Duration.between(now, reset).toMinutes().toInt())
        }
    }
}
