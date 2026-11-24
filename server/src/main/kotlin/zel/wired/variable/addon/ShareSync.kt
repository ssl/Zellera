package zel.wired.variable.addon

import zel.protocol.generated.WiredSaveSuccessOut
import zel.protocol.generated.WiredSaveSuccessOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.wired.BoxSettingsSource
import zel.wired.form.WiredMasks
import zel.wired.store.WiredWriter
import zel.wired.timing.RoomPoster
import zel.wired.variable.Availability
import zel.wired.variable.RoomVariables

class ShareSync(
    private val forms: BoxSettingsSource,
    private val rows: ImportRows,
    private val stack: StackAddons,
    private val copies: ImportCopies,
    private val fx: VariableFx,
    private val variables: () -> RoomVariables,
    private val store: ImportStore,
    private val persist: (Runnable) -> Unit,
    private val writer: WiredWriter,
    private val poster: RoomPoster,
    private val isLoaded: (Int) -> Boolean,
    private val masks: (Room) -> WiredMasks,
) {
    fun synchronize(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        boxId: Int,
    ) {
        val room = scope.room
        if (!masks(room).mayModify(level)) return
        val share = room.furni[boxId]?.takeIf { Interactions.of(it.definition) == AddonCodes.SHARE } ?: return
        scope.out.send(Lane.Now, Audience.Player(playerId), WiredSaveSuccessOutCodec, WiredSaveSuccessOut)
        val shared = sharedVariable(room, share) ?: return
        val addons = shared(room, share)
        val roomId = room.info.id
        persist(Runnable { deliver(roomId, shared.id, addons) })
    }

    private fun sharedVariable(
        room: Room,
        share: RoomFurni,
    ): RoomFurni? {
        val tile = share.anchor ?: return null
        return room.furni.at(tile).furni.firstOrNull {
            Interactions.of(it.definition).startsWith(RoomVariables.BOX_PREFIX) &&
                variables().definition(room, it)?.entry?.availability == Availability.SHARED
        }
    }

    private fun shared(
        room: Room,
        share: RoomFurni,
    ): ImportedAddons {
        val flags = forms.settings(room, share)?.ints.orEmpty()

        fun on(category: Int) = flags.getOrElse(category) { 1 } != 0

        fun copied(box: RoomFurni) =
            forms.settings(room, box)?.let { CopiedBox(box.definition.id, box.id, it.ints, it.text) }
        val tile =
            share.anchor
                ?.let { room.furni.at(it).furni }
                .orEmpty()
                .sortedWith(compareBy({ it.z }, { it.id }))
        return ImportedAddons(
            if (on(StackAddons.SHARED_CONNECTORS)) {
                stack.onTile(room, share, AddonCodes.TEXT_CONNECTOR).mapNotNull { forms.settings(room, it)?.text }
            } else {
                emptyList()
            },
            if (on(StackAddons.SHARED_LEVELS)) {
                stack.onTile(room, share, AddonCodes.LEVEL_UP).mapNotNull(::copied)
            } else {
                emptyList()
            },
            if (on(StackAddons.SHARED_FX)) {
                tile.filter { FxKind.fromCode(Interactions.of(it.definition)) != null }.mapNotNull(::copied)
            } else {
                emptyList()
            },
        )
    }

    /** Stores each reached row behind its room's wired writes, loaded or not; a loaded room also takes it. */
    private fun deliver(
        roomId: Int,
        boxId: Int,
        addons: ImportedAddons,
    ) {
        val token = ImportStore.tokenOf(roomId, boxId)
        store.importsOf(roomId, boxId).groupBy { it.roomId }.forEach { (targetRoom, reached) ->
            writer.write(targetRoom, Runnable { reached.forEach { store.save(it, addons) } })
            if (isLoaded(targetRoom)) {
                val others =
                    store
                        .importsIn(targetRoom)
                        .filterNot { row -> reached.any { it.importId == row.importId } }
                        .associateWith { row -> if (row.token == token) addons else store.sharedBy(row.token) }
                poster.post(targetRoom, 0) { scope -> arrive(scope, reached, addons, others) }
            }
        }
    }

    private fun arrive(
        scope: RoomScope,
        reached: List<ImportRow>,
        addons: ImportedAddons,
        others: Map<ImportRow, ImportedAddons?>,
    ) {
        val room = scope.room
        val first = fx.firstSynchronize(room)
        if (first) others.forEach { (row, shared) -> shared?.let { put(room, row, it) } }
        val boxes = reached.mapNotNull { row -> put(room, row, addons) }
        fx.synchronized(scope, boxes)
    }

    private fun put(
        room: Room,
        row: ImportRow,
        addons: ImportedAddons,
    ): RoomFurni? {
        val box = room.furni[row.importId] ?: return null
        rows.updateExtras(room, box) { addons.writeTo(it, row.token) }
        copies.replaced(room, box, ImportedAddons.read(rows.extras(room, box)))
        return box
    }
}
