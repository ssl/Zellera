package zel.wired.score

import zel.protocol.generated.ObjectDataUpdateOut
import zel.protocol.generated.ObjectDataUpdateOutCodec
import zel.protocol.generated.ObjectUpdateOutCodec
import zel.protocol.generated.StuffData
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.Ticking
import zel.room.furni.Toggleable
import zel.room.furni.Usable
import zel.room.furni.place.FurniWire
import zel.room.furni.place.Interactions
import zel.room.furni.place.StuffOnWire
import zel.room.games.GameHost
import zel.room.games.GameOut
import zel.room.games.GameResult
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.GameZone
import zel.util.MILLIS_PER_SECOND
import zel.wired.store.WiredWriter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class Highscores(
    private val store: HighscoreStore,
    private val writer: WiredWriter,
    private val levelOf: (Room, Int) -> Int?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val fixedZone: ZoneId? = null,
) : Usable,
    Toggleable,
    Ticking,
    StuffOnWire,
    RoomPart {
    private val zone: ZoneId get() = fixedZone ?: GameZone.current

    /** [names] holds every player of [entries], so a refresh at midnight reads nothing from the store. */
    private class Board(
        val entries: MutableList<ScoreEntry>,
        val names: MutableMap<Int, String>,
        var nextDay: Long,
    ) {
        var rows: List<StuffData.HighScoreEntry> = emptyList()
    }

    private val boards = ConcurrentHashMap<Int, Board>()

    override fun unload(scope: RoomScope) {
        scope.room.furni
            .all()
            .forEach { boards.remove(it.id) }
    }

    override fun stuff(furni: RoomFurni): StuffData {
        val kind = BoardKind.of(furni.definition.name)
        val board = board(furni)
        return StuffData(
            FORMAT,
            StuffData.Value.HighScore(
                shownState(furni),
                kind.type.wire,
                kind.clear.wire,
                board.rows,
            ),
        )
    }

    fun inventoryStuff(furni: RoomFurni): StuffData {
        val kind = BoardKind.of(furni.definition.name)
        return StuffData(
            FORMAT,
            StuffData.Value.HighScore(shownState(furni), kind.type.wire, kind.clear.wire, emptyList()),
        )
    }

    private fun shownState(furni: RoomFurni): String = if (furni.state == "1") "1" else "0"

    private fun board(furni: RoomFurni): Board =
        boards.getOrPut(furni.id) {
            val entries = store.entries(furni.id, periodStart(furni)).toMutableList()
            val names = HashMap(store.names(entries.flatMap { it.userIds }.distinct()))
            Board(entries, names, nextMidnight()).also { it.rows = shown(furni, it) }
        }

    /** Reads the store only for players new to the board, so only after a game added them. */
    private fun shown(
        furni: RoomFurni,
        board: Board,
    ): List<StuffData.HighScoreEntry> {
        val kind = BoardKind.of(furni.definition.name)
        val rows = HighscoreRanking.rows(kind.type, board.entries.filter { within(kind.clear, it.timestamp) })
        val missing = rows.flatMap { it.userIds }.distinct().filterNot { it in board.names }
        if (missing.isNotEmpty()) {
            val found = store.names(missing)
            missing.forEach { board.names[it] = found[it].orEmpty() }
        }
        return rows.map { row -> StuffData.HighScoreEntry(row.score, row.userIds.map { board.names[it].orEmpty() }) }
    }

    private fun within(
        clear: ClearType,
        timestamp: Int,
    ): Boolean {
        val first = firstDay(clear) ?: return true
        return !LocalDate.ofInstant(Instant.ofEpochSecond(timestamp.toLong()), zone).isBefore(first)
    }

    /** The first day of the period a board of [clear] shows, a week being Monday to Sunday; null for all time. */
    private fun firstDay(clear: ClearType): LocalDate? {
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(clock()), zone)
        return when (clear) {
            ClearType.ALL_TIME -> null
            ClearType.DAILY -> today
            ClearType.WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ClearType.MONTHLY -> today.withDayOfMonth(1)
        }
    }

    /** Unix seconds; 0 for an all-time board. */
    private fun periodStart(furni: RoomFurni): Int {
        val first = firstDay(BoardKind.of(furni.definition.name).clear) ?: return 0
        return first.atStartOfDay(zone).toEpochSecond().toInt()
    }

    private fun prune(
        roomId: Int,
        furni: RoomFurni,
        board: Board,
    ) {
        val since = periodStart(furni)
        if (!board.entries.removeAll { it.timestamp < since }) return
        val id = furni.id
        writer.write(roomId, Runnable { store.dropBefore(id, since) })
    }

    private fun nextMidnight(): Long =
        LocalDate
            .ofInstant(Instant.ofEpochMilli(clock()), zone)
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    override fun onUse(
        scope: RoomScope,
        furni: RoomFurni,
        playerId: Int,
        param: Int,
    ) {
        if ((levelOf(scope.room, playerId) ?: 0) < RIGHTS) return
        flip(furni)
        scope.out.send(Lane.Now, Audience.Room, ObjectUpdateOutCodec, FurniWire.objectUpdate(furni))
        scope.raise(
            RoomEvent.StateChanged(
                furni.id,
                scope.room.units
                    .ofPlayer(playerId)
                    ?.index,
            ),
        )
    }

    private fun flip(furni: RoomFurni) {
        furni.state = if (furni.state == "1") "0" else "1"
        furni.dirty = true
    }

    override fun toggle(
        scope: RoomScope,
        furni: RoomFurni,
        backward: Boolean,
    ): Boolean {
        flip(furni)
        return true
    }

    override fun toggleRandom(
        scope: RoomScope,
        furni: RoomFurni,
        random: Random,
    ): Boolean {
        flip(furni)
        return true
    }

    override fun onTick(
        scope: RoomScope,
        furni: RoomFurni,
    ) {
        // a board is read when it is first shown; the tick only filters what the board already holds
        val board = boards[furni.id] ?: return
        if (clock() < board.nextDay) return
        board.nextDay = nextMidnight()
        prune(scope.room.info.id, furni, board)
        refresh(scope, furni, board)
    }

    private fun refresh(
        scope: RoomScope,
        furni: RoomFurni,
        board: Board,
    ) {
        board.rows = shown(furni, board)
        scope.out.send(
            Lane.Now,
            Audience.Room,
            ObjectDataUpdateOutCodec,
            ObjectDataUpdateOut(furni.id.toString(), stuff(furni)),
        )
    }

    fun finished(
        scope: RoomScope,
        result: GameResult,
    ) {
        val boardsHere = boardsOf(scope.room)
        val scored = result.teams.filter { it.playerIds.isNotEmpty() && it.score > 0 }
        if (boardsHere.isEmpty() || scored.isEmpty()) return
        val winner = scored.maxBy { it.score }
        val now = (clock() / MILLIS_PER_SECOND).toInt()
        val entries = scored.asReversed().map { ScoreEntry(it.playerIds, it.score, it === winner, now, result.seconds) }
        boardsHere.forEach { furni ->
            val board = board(furni)
            prune(scope.room.info.id, furni, board)
            board.entries += entries
            val id = furni.id
            writer.write(scope.room.info.id, Runnable { store.add(id, entries) })
            board.rows = shown(furni, board)
        }
        GameOut.states(scope, boardsHere)
    }

    fun command(
        scope: RoomScope,
        playerId: Int,
        text: String,
    ): Boolean {
        if (text.trim().lowercase() != RESET) return false
        if ((levelOf(scope.room, playerId) ?: 0) >= OWNER) {
            boardsOf(scope.room).forEach { furni ->
                val board = board(furni)
                board.entries.clear()
                val id = furni.id
                writer.write(scope.room.info.id, Runnable { store.clear(id) })
                refresh(scope, furni, board)
            }
        }
        return true
    }

    private fun boardsOf(room: Room): List<RoomFurni> =
        room.furni.all().filter { Interactions.of(it.definition) == CODE }

    fun games(next: GameHost): GameHost =
        object : GameHost by next {
            override fun finished(
                scope: RoomScope,
                result: GameResult,
            ) {
                next.finished(scope, result)
                this@Highscores.finished(scope, result)
            }
        }

    companion object {
        const val CODE = "wf_highscore"
        private const val FORMAT = 6
        private const val RIGHTS = 1
        private const val OWNER = 4
        private const val RESET = ":resetscores"
    }
}
