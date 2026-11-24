package zel.wired.score

import com.google.gson.JsonObject
import zel.protocol.generated.VariableLeaderboardChunkUpdateOutCodec
import zel.protocol.generated.VariableLeaderboardSnapshotOut
import zel.protocol.generated.VariableLeaderboardSnapshotOutCodec
import zel.protocol.typed.PacketSession
import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.tick.TickPart
import zel.room.unit.UnitKind
import zel.wired.BoxSettingsSource
import zel.wired.store.StoredBox
import zel.wired.store.WiredStore
import zel.wired.store.WiredWriter
import zel.wired.variable.Availability
import zel.wired.variable.EchoVariable
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.SharedToken
import zel.wired.variable.Variable
import zel.wired.variable.VariableChange
import zel.wired.variable.VariableOn
import zel.wired.variable.VariableTarget
import java.util.concurrent.ConcurrentHashMap

class Leaderboards(
    private val store: LeaderboardStore,
    private val variables: () -> RoomVariables?,
    private val forms: () -> BoxSettingsSource?,
    private val persist: (Runnable) -> Unit,
    private val writer: WiredWriter = WiredWriter { _, job -> persist(job) },
) : RoomPart,
    RoomListener {
    private class Published(
        val furni: RoomFurni,
        val id: Int,
        var revision: Int,
        var choices: List<Pair<String, String>>?,
    )

    private data class Subscription(
        val boardId: Int,
        val revision: Int,
        val variable: String,
        val chunks: List<Int>,
        val session: PacketSession,
    )

    private class Boards {
        val published = LinkedHashMap<Int, Published>()
        val subscriptions = HashMap<Int, Subscription>()
        val changed = HashSet<String>()
        val players = HashMap<Int, Int>()

        var sent: List<Pair<Int, Int>> = emptyList()
        var nextTick = 0L
        var loading = true
    }

    private val rooms = ConcurrentHashMap<Room, Boards>()

    private val marks = ConcurrentHashMap<Int, Pair<Int, Int>>()

    override fun load(room: Room) {
        val boards = Boards()
        rooms[room] = boards
        val stored = store.marks(room.info.id)
        room.furni.all().filter { Interactions.of(it.definition) == CODE }.forEach { furni ->
            stored[furni.id]?.takeIf { (id, _) -> id > 0 }?.let { (id, revision) ->
                boards.published[furni.id] = Published(furni, id, revision, null)
                marks[furni.id] = id to revision
            }
        }
        boards.sent = boards.published.values.map { it.id to it.revision }
    }

    override fun unload(scope: RoomScope) {
        rooms
            .remove(scope.room)
            ?.published
            ?.keys
            ?.forEach(marks::remove)
    }

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        val boards = boards(scope.room)
        when (event) {
            is RoomEvent.Entered -> {
                (scope.room.units[event.unitIndex]?.kind as? UnitKind.Player)?.let {
                    boards.players[event.unitIndex] =
                        it.playerId
                }
            }

            is RoomEvent.Left -> {
                boards.players.remove(event.unitIndex)?.let(boards.subscriptions::remove)
            }

            else -> {}
        }
    }

    fun heard(
        on: VariableOn,
        change: VariableChange,
    ) {
        boards(on.scope.room).changed += change.variableId
        store.changed()
    }

    fun publishedId(
        room: Room,
        boxId: Int,
    ): Int = boards(room).published[boxId]?.id ?: 0

    fun atLimit(
        room: Room,
        boxId: Int,
    ): Boolean {
        val published = boards(room).published
        return boxId !in published && published.size >= MAX_PER_ROOM
    }

    /** Published in the lowest free slot, a new revision either way. */
    fun publish(
        room: Room,
        box: RoomFurni,
    ) {
        val boards = boards(room)
        val known = boards.published[box.id]
        val board =
            when {
                known == null -> {
                    val free = (1..MAX_PER_ROOM).first { slot -> boards.published.values.none { it.id == slot } }
                    Published(box, free, 1, null).also { boards.published[box.id] = it }
                }

                known.furni === box -> {
                    known.also { it.revision++ }
                }

                // the same furni placed again before the tick dropped its old entry keeps that entry's slot
                else -> {
                    Published(box, known.id, known.revision + 1, null).also { boards.published[box.id] = it }
                }
            }
        board.choices = null
        marks[box.id] = board.id to board.revision
    }

    fun showPublished(scope: RoomScope) {
        val boards = boards(scope.room)
        started(scope, boards)
        sendSnapshot(scope, Audience.Room, boards)
    }

    fun subscribe(
        scope: RoomScope,
        session: PacketSession,
        playerId: Int,
        request: SubscribeRequest,
    ) {
        val boards = boards(scope.room)
        started(scope, boards)
        if (request.boardId == 0) {
            return sendSnapshot(scope, Audience.Player(playerId), boards)
        }
        val board = boards.published.values.firstOrNull { request.namesChunks && it.id == request.boardId } ?: return
        val choices = choices(scope.room, board)
        val subscription = Subscription(board.id, board.revision, request.variable, request.chunks, session)
        val stale = request.revision != board.revision || choices.none { it.first == request.variable }
        val same = boards.subscriptions[playerId]?.copy(session = session) == subscription
        if (stale || same) return
        boards.subscriptions[playerId] = subscription
        sendRows(scope, board, subscription)
    }

    fun entered(
        scope: RoomScope,
        playerId: Int,
    ) {
        val boards = boards(scope.room)
        val loading = boards.loading
        boards.loading = false
        if (loading && boards.published.isNotEmpty()) {
            started(scope, boards)
            sendSnapshot(scope, Audience.Player(playerId), boards)
        }
    }

    private fun started(
        scope: RoomScope,
        boards: Boards,
    ) {
        if (boards.nextTick == 0L) boards.nextTick = scope.now() + TICK_MILLIS
    }

    val tick: TickPart =
        TickPart { scope ->
            val boards = boards(scope.room)
            if (boards.nextTick != 0L && scope.now() >= boards.nextTick) {
                boards.nextTick = scope.now() + TICK_MILLIS
                onTick(scope, boards)
            }
        }

    private fun onTick(
        scope: RoomScope,
        boards: Boards,
    ) {
        boards.published.entries.removeAll { (id, board) ->
            val gone = scope.room.furni[id] !== board.furni
            if (gone) marks.remove(id)
            gone
        }
        boards.published.values.forEach { board ->
            val known = board.choices
            val now = choiceList(scope.room, board.furni)
            board.choices = now
            if (known != null && known != now) {
                board.revision++
                marks[board.furni.id] = board.id to board.revision
                saveMarks(scope.room.info.id, board)
            }
        }
        val current = boards.published.values.map { it.id to it.revision }
        if (current != boards.sent) sendSnapshot(scope, Audience.Room, boards)
        val changed = boards.changed.toSet()
        boards.changed.clear()
        boards.subscriptions.values.filter { it.variable in changed }.forEach { subscription ->
            boards.published.values.firstOrNull { it.id == subscription.boardId }?.let {
                sendRows(
                    scope,
                    it,
                    subscription,
                )
            }
        }
    }

    /** The mark lives in the box's extras row, so it goes in line with the room's other box writes. */
    private fun saveMarks(
        roomId: Int,
        board: Published,
    ) {
        val id = board.furni.id
        val published = board.id
        val revision = board.revision
        writer.write(roomId, Runnable { store.saveMark(id, published, revision) })
    }

    private fun sendSnapshot(
        scope: RoomScope,
        to: Audience,
        boards: Boards,
    ) {
        boards.sent = boards.published.values.map { it.id to it.revision }
        val list = boards.published.values.mapNotNull { snapshot(scope.room, it) }
        scope.out.send(
            Lane.Now,
            to,
            VariableLeaderboardSnapshotOutCodec,
            VariableLeaderboardSnapshotOut(list),
        )
    }

    private fun snapshot(
        room: Room,
        board: Published,
    ): VariableLeaderboardSnapshotOut.List1Item? {
        val settings = forms()?.settings(room, board.furni) ?: return null
        val form = LeaderboardForm(settings)
        val choices = choices(room, board)
        val columns =
            (0 until form.columnCount).map { i ->
                VariableLeaderboardSnapshotOut.List1Item.List1Item(
                    form.columnTypes[i],
                    form.label(i),
                    form.widths[i],
                )
            }
        return VariableLeaderboardSnapshotOut.List1Item(
            board.id,
            board.revision,
            form.title,
            form.description,
            form.tileX,
            form.tileY,
            form.offsetX,
            form.altitude,
            form.sort,
            columns,
            choices.map { VariableLeaderboardSnapshotOut.List1Item.List2Item(it.first, it.second) },
            choices.firstOrNull()?.first ?: NO_CHOICE,
            form.pageOption,
        )
    }

    private fun choices(
        room: Room,
        board: Published,
    ): List<Pair<String, String>> = board.choices ?: choiceList(room, board.furni).also { board.choices = it }

    /** Bottom first, at most ten. */
    private fun choiceList(
        room: Room,
        box: RoomFurni,
    ): List<Pair<String, String>> {
        val form = forms()?.settings(room, box)?.let(::LeaderboardForm)
        val overrides =
            form
                ?.overrides
                .orEmpty()
                .take(form?.overrideCount ?: 0)
                .toMap()
        return stacked(room, box).distinctBy { it.first }.take(MAX_CHOICES).map { (id, name) ->
            id to (overrides[id] ?: LeaderboardRows.displayName(name))
        }
    }

    /** How many variables the box's stack offers, repeats counted. */
    fun choiceCount(
        room: Room,
        boxId: Int,
    ): Int = room.furni[boxId]?.let { stacked(room, it).size } ?: 0

    /**
     * The user variables in the stack, bottom first: an echo stands for the user variable it echoes, through any
     * echoes in between, also when that variable is listed already.
     */
    fun stacked(
        room: Room,
        box: RoomFurni,
    ): List<Pair<String, String>> {
        val tile = box.anchor ?: return emptyList()
        return room.furni
            .at(tile)
            .furni
            .sortedWith(compareBy<RoomFurni> { it.z }.thenBy { it.id })
            .mapNotNull { furni ->
                when (Interactions.of(furni.definition)) {
                    in USER_BOXES -> {
                        val name = forms()?.settings(room, furni)?.text.orEmpty()
                        val id = variables()?.definition(room, furni)?.id ?: "${RoomVariables.USER_PREFIX}:${furni.id}"
                        id to name
                    }

                    RoomVariables.ECHO_BOX -> {
                        echoed(room, furni)
                    }

                    else -> {
                        null
                    }
                }
            }
    }

    private fun echoed(
        room: Room,
        echo: RoomFurni,
    ): Pair<String, String>? {
        val sourceId = forms()?.settings(room, echo)?.variableIds?.firstOrNull() ?: return null
        val source = variables()?.find(room, sourceId)?.let(::underEchoes)?.entry ?: return null
        return if (source.target == VariableTarget.USER) source.id to source.name else null
    }

    /** An echo of an echo stands for the variable at the bottom of the chain, the one whose values are kept. */
    private fun underEchoes(variable: Variable): Variable {
        var source = variable
        while (source is EchoVariable) source = source.source
        return source
    }

    /** Rows are read outside the lock on the source room's write lane, which first writes the room's held values. */
    private fun sendRows(
        scope: RoomScope,
        board: Published,
        subscription: Subscription,
    ) {
        val room = scope.room
        val settings = forms()?.settings(room, board.furni) ?: return
        val form = LeaderboardForm(settings)
        val variable = variables()?.find(room, subscription.variable) ?: return
        val query = query(scope, form, variable) ?: return
        writer.write(
            query.roomId,
            Runnable {
                val rows = rows(query)
                val shows = { column: Int -> column in query.columns }
                LeaderboardRows
                    .chunks(
                        subscription.boardId,
                        subscription.revision,
                        subscription.variable,
                        rows,
                        subscription.chunks,
                        shows,
                    ).forEach { subscription.session.send(VariableLeaderboardChunkUpdateOutCodec, it) }
            },
        )
    }

    private fun query(
        scope: RoomScope,
        form: LeaderboardForm,
        variable: Variable,
    ): RowQuery? {
        val room = scope.room
        val all = variables() ?: return null
        val entry = variable.entry
        val token = room.furni[variable.boxId]?.let { all.tokenOf(room, it) }?.let(SharedToken::parse)
        val filter = if (form.filterOn) all.find(room, form.filterVariable)?.boxId?.takeIf { it != 0 } else null
        val stored = entry.target == VariableTarget.USER && (Availability.isStored(entry.availability) || token != null)
        return RowQuery(
            stored,
            token?.roomId ?: room.info.id,
            token?.boxId ?: variable.boxId,
            filter,
            wantedValue(scope, form),
            form.sort,
            form.columnTypes.take(form.columnCount).toSet(),
            entry::textOf,
            all.levelsOf(room, variable),
        )
    }

    private fun wantedValue(
        scope: RoomScope,
        form: LeaderboardForm,
    ): Long? =
        when {
            !form.comparisonOn -> {
                null
            }

            form.comparisonGlobal -> {
                variables()
                    ?.find(
                        scope.room,
                        form.globalVariable,
                    )?.read(VariableOn(scope), Holder.Room)
                    ?.value
            }

            else -> {
                form.literal.toLongOrNull()
            }
        }

    private fun rows(query: RowQuery): List<LeaderRow> {
        if (!query.stored) return emptyList()
        var holders = store.holders(query.roomId, query.boxId)
        query.filterBox?.let { filter ->
            val kept = store.holders(query.roomId, filter).filter { query.wanted == null || it.value == query.wanted }
            val ids = kept.map { it.userId }.toSet()
            holders = holders.filter { it.userId in ids }
        }
        return LeaderboardRows.sorted(holders, query.sort).mapIndexed { i, holder ->
            val text = if (LeaderboardForm.TEXT in query.columns) holder.value?.let(query.text) else null
            val level =
                query.levels?.takeIf { LeaderboardForm.LEVEL in query.columns }?.let { levels ->
                    holder.value?.let { levels(it).toString() }
                }
            LeaderRow(i + 1, holder, text, level)
        }
    }

    private class RowQuery(
        val stored: Boolean,
        val roomId: Int,
        val boxId: Int,
        val filterBox: Int?,
        val wanted: Long?,
        val sort: Int,
        val columns: Set<Int>,
        val text: (Long) -> String,
        val levels: ((Long) -> Int)?,
    )

    private fun boards(room: Room): Boards = rooms.computeIfAbsent(room) { Boards() }

    fun keeping(next: WiredStore): WiredStore =
        object : WiredStore by next {
            override fun saveBox(
                furniId: Int,
                box: StoredBox,
            ) {
                val mark = marks[furniId]
                if (mark != null) {
                    val extras = box.extras?.deepCopy() ?: JsonObject()
                    extras.addProperty(LeaderboardStore.PUBLISHED_KEY, mark.first)
                    extras.addProperty(LeaderboardStore.REVISION_KEY, mark.second)
                    next.saveBox(furniId, box.copy(extras = extras))
                } else {
                    next.saveBox(furniId, box)
                }
            }
        }

    companion object {
        const val CODE = "wf_xtra_var_leaderboard"
        private const val TICK_MILLIS = 10_000L
        private const val MAX_PER_ROOM = 4
        private const val MAX_CHOICES = 10

        /** The default variable of a board with no choices; the client drops the board for anything else. */
        private const val NO_CHOICE = "0"
        private val USER_BOXES = setOf(RoomVariables.USER_BOX, RoomVariables.REFERENCE_BOX)
    }
}
