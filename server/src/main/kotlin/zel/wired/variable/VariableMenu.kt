package zel.wired.variable

import zel.protocol.generated.PacketWriter
import zel.protocol.generated.WiredAllVariableHoldersOut
import zel.protocol.generated.WiredAllVariableHoldersOutCodec
import zel.protocol.generated.WiredAllVariablesDiffsOut
import zel.protocol.generated.WiredAllVariablesDiffsOutCodec
import zel.protocol.generated.WiredAllVariablesHashOut
import zel.protocol.generated.WiredAllVariablesHashOutCodec
import zel.protocol.generated.WiredGetAllVariablesDiffsIn
import zel.protocol.generated.WiredMenuErrorOut
import zel.protocol.generated.WiredMenuErrorOutCodec
import zel.protocol.generated.WiredPlayerPermanentVariablesOut
import zel.protocol.generated.WiredPlayerPermanentVariablesOutCodec
import zel.protocol.generated.WiredPlayerVariablesListOut
import zel.protocol.generated.WiredPlayerVariablesListOutCodec
import zel.protocol.generated.WiredSetPlayerPermanentVariableResultOut
import zel.protocol.generated.WiredSetPlayerPermanentVariableResultOutCodec
import zel.protocol.generated.WiredVariablesForObjectOut
import zel.protocol.generated.WiredVariablesForObjectOutCodec
import zel.room.Room
import zel.room.RoomScope
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.GameZone
import zel.wired.WiredRooms
import zel.wired.firing.Firing
import zel.wired.firing.FiringStart
import zel.wired.firing.HeldValue
import zel.wired.firing.Selection
import zel.wired.form.WiredMasks
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class VariableMenu(
    private val variables: RoomVariables,
    private val masks: (Room) -> WiredMasks,
    private val rooms: () -> WiredRooms?,
    private val store: VariableStore,
) {
    fun hash(
        scope: RoomScope,
        playerId: Int,
        level: Int,
    ) {
        if (!masks(scope.room).mayRead(level)) return
        val hash = VariableEntry.listHash(variables.listed(scope.room).map { it.entry })
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            WiredAllVariablesHashOutCodec,
            WiredAllVariablesHashOut(hash),
        )
    }

    fun diffs(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        known: List<WiredGetAllVariablesDiffsIn.HashesItem>,
    ) {
        if (!masks(scope.room).mayRead(level)) return
        val listed = variables.listed(scope.room).map { it.entry }
        val cached = known.associate { it.variableId to it.hash }
        val ids = listed.mapTo(HashSet()) { it.id }
        val removed = cached.keys.filterNot { it in ids }
        val changed = listed.filter { cached[it.id] != it.hash }
        val hash = VariableEntry.listHash(listed)
        val chunks = changed.chunked(CHUNK).ifEmpty { listOf(emptyList()) }
        chunks.forEachIndexed { index, chunk ->
            val body = PacketWriter()
            body.int(chunk.size)
            chunk.forEach { entry ->
                body.int(entry.hash)
                VariableWire.block(body, entry)
            }
            scope.out.send(
                Lane.Now,
                Audience.Player(playerId),
                WiredAllVariablesDiffsOutCodec,
                WiredAllVariablesDiffsOut(
                    hash,
                    index == chunks.lastIndex,
                    if (index == 0) removed else emptyList(),
                    body.toByteArray(),
                ),
            )
        }
    }

    fun inspect(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        type: Int,
        id: Int,
    ) {
        if (!masks(scope.room).mayRead(level)) return
        answer(scope, playerId, type, id, holderOf(scope.room, type, id))
    }

    private fun answer(
        scope: RoomScope,
        playerId: Int,
        type: Int,
        id: Int,
        holder: Holder?,
    ) {
        val to = Audience.Player(playerId)
        if (holder == null) {
            scope.out.send(Lane.Now, to, WiredMenuErrorOutCodec, WiredMenuErrorOut(NOT_FOUND))
            return
        }
        val on = VariableOn(scope)
        val values = heldBy(on, holder)
        val body = PacketWriter()
        if (type != VariableTarget.GLOBAL) body.int(id)
        body.int(values.size)
        values.forEach { (variableId, value) -> body.string(variableId).int(value) }
        if (type == VariableTarget.FURNI) {
            val picking = variables.pickedBy(scope.room, abs(id))
            body.int(picking.size)
            picking.forEach(body::int)
        }
        scope.out.send(
            Lane.Now,
            to,
            WiredVariablesForObjectOutCodec,
            WiredVariablesForObjectOut(type, body.toByteArray()),
        )
    }

    private fun heldBy(
        on: VariableOn,
        holder: Holder,
    ): List<Pair<String, Int>> {
        val target =
            when (holder) {
                is Holder.Furni -> VariableTarget.FURNI
                Holder.Room -> VariableTarget.GLOBAL
                else -> VariableTarget.USER
            }
        val room = on.scope.room
        val customs =
            variables.listed(room).filter { it.entry.target == target && !isInternal(it.entry) }.sortedBy { it.boxId }
        val own =
            customs.mapNotNull { variable ->
                variable.read(on, holder)?.let { variable.id to VariableWire.shown(variable.entry, it.value) }
            }
        val internal =
            variables.internal.values.valuesOf(on, holder).map { (id, value) ->
                val entry = variables.find(room, id)?.entry
                id to if (entry != null) VariableWire.shown(entry, value) else VariableWire.shown(value)
            }
        return own + internal
    }

    private fun isInternal(entry: VariableEntry): Boolean = entry.id.startsWith('-') || entry.id.startsWith('~')

    fun set(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        request: ObjectWrite,
    ) {
        if (!masks(scope.room).mayModify(level)) return
        val wired = rooms() ?: return
        val holder = holderOf(scope.room, request.target, request.objectId)
        val held = HeldScope(scope)
        wired.within(held) { flash, chain ->
            val firing =
                Firing(held, null, FiringStart(null, null), Selection(), chain, flash, emptyList())
            if (holder != null) write(VariableOn(held, firing), holder, request)
            answer(scope, playerId, request.target, request.objectId, holder)
        }
        held.flush()
    }

    private fun write(
        on: VariableOn,
        holder: Holder,
        request: ObjectWrite,
    ) {
        val variable = variables.find(on.scope.room, request.variableId) ?: return
        val flags = variable.entry.flags
        val value = request.value.toLong()
        when (request.action) {
            ACTION_SET -> {
                if (flags.canWriteValue) variable.write(on, holder, value, ChangeOrigin.INSPECTION)
            }

            ACTION_CREATE -> {
                if (flags.canCreateAndDelete) variable.give(on, holder, value, true, ChangeOrigin.INSPECTION)
            }

            ACTION_DELETE -> {
                if (flags.canCreateAndDelete) variable.remove(on, holder, ChangeOrigin.INSPECTION)
            }
        }
    }

    private fun holderOf(
        room: Room,
        type: Int,
        id: Int,
    ): Holder? =
        when (type) {
            VariableTarget.FURNI -> {
                room.furni[abs(id)]
                    ?.takeIf { it.isWall == (id < 0) }
                    ?.let { Holder.of(it) }
            }

            VariableTarget.USER -> {
                room.units[id]?.let { Holder.of(it) }
            }

            VariableTarget.GLOBAL -> {
                Holder.Room
            }

            else -> {
                null
            }
        }

    fun holders(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        variableId: String,
    ) {
        if (!masks(scope.room).mayRead(level)) return
        val variable = variables.find(scope.room, variableId) ?: return
        val entry = variable.entry
        if (entry.target != VariableTarget.USER && entry.target != VariableTarget.FURNI) return
        if (entry.flags.requiresExecutionContext) return
        val on = VariableOn(scope)
        val rows =
            variable.holders(on).mapNotNull { holder ->
                val shownId = objectId(scope.room, holder) ?: return@mapNotNull null
                variable.read(on, holder)?.let { shownId to VariableWire.shown(entry, it.value) }
            }
        val body = PacketWriter()
        VariableWire.texts(body, entry.texts)
        body.int(rows.size)
        rows.forEach { (id, value) -> body.int(id).int(value) }
        val flags = entry.flags
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            WiredAllVariableHoldersOutCodec,
            WiredAllVariableHoldersOut(
                if (entry.target == VariableTarget.USER) USER_LIST else FURNI_LIST,
                entry.id,
                entry.type,
                entry.name,
                entry.availability,
                entry.target,
                flags.alwaysAvailable,
                flags.canCreateAndDelete,
                flags.hasValue,
                flags.canWriteValue,
                flags.canInterceptChanges,
                flags.isInvisible,
                flags.requiresExecutionContext,
                flags.canReadCreationTime,
                flags.canReadLastUpdateTime,
                body.toByteArray().let { it[0] != 0.toByte() },
                body.toByteArray().let { it.copyOfRange(1, it.size) },
            ),
        )
    }

    private fun objectId(
        room: Room,
        holder: Holder,
    ): Int? =
        when (holder) {
            is Holder.Furni -> {
                holder.id
            }

            Holder.Room, Holder.Context -> {
                null
            }

            else -> {
                holder.unitIn(room)?.index
            }
        }

    fun page(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        request: HoldersPage,
    ) {
        if (!masks(scope.room).mayRead(level)) return
        if (request.pageSize !in 1..MAX_PAGE_SIZE) {
            val refused = WiredMenuErrorOut(BAD_PAGE_SIZE)
            scope.out.send(Lane.Now, Audience.Player(playerId), WiredMenuErrorOutCodec, refused)
            return
        }
        val room = scope.room
        val variable = variables.find(room, request.variableId)
        val all = variable?.let { variables.allHolders(room, it).entries.toList() }.orEmpty()
        val page = request.page.coerceAtLeast(1)
        val size = request.pageSize
        val skipped = ((page - 1).toLong() * size).coerceAtMost(all.size.toLong()).toInt()
        val shown = all.drop(skipped).take(size)
        val names = variables.names(shown.mapNotNull { (it.key as? Holder.Player)?.id })
        val body = PacketWriter()
        body.int(shown.size)
        shown.forEach { (holder, held) -> element(room, body, holder, held, variable, names) }
        body.int(request.userTypeFilter).int(request.sortTypeFilter)
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            WiredPlayerVariablesListOutCodec,
            WiredPlayerVariablesListOut(request.variableId, all.size, page, size, body.toByteArray()),
        )
    }

    private fun element(
        room: Room,
        body: PacketWriter,
        holder: Holder,
        held: HeldValue,
        variable: Variable?,
        names: Map<Int, String>,
    ) {
        val (type, id, name) =
            when (holder) {
                is Holder.Player -> {
                    Triple(ENTITY_USER, holder.id, names[holder.id].orEmpty())
                }

                is Holder.Furni -> {
                    Triple(
                        ENTITY_FURNI,
                        holder.id,
                        room.furni[holder.id]
                            ?.definition
                            ?.name
                            .orEmpty(),
                    )
                }

                is Holder.Bot -> {
                    Triple(ENTITY_USER, holder.id, "")
                }

                is Holder.Pet -> {
                    Triple(ENTITY_USER, holder.id, "")
                }

                Holder.Room, Holder.Context -> {
                    Triple(0, 0, "")
                }
            }
        body.int(type).int(id).string(name)
        val entry = variable?.entry
        storage(body, if (entry != null) VariableWire.shown(entry, held.value) else 0, held)
    }

    private fun storage(
        body: PacketWriter,
        value: Int,
        held: HeldValue,
    ) {
        body.int(value)
        body.long(held.created).string(time(held.created))
        body.long(held.updated).string(time(held.updated))
    }

    private fun time(millis: Long): String = stamp(millis, GameZone.current)

    fun deleteAll(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        variableId: String,
    ) {
        val room = scope.room
        val variable = variables.find(room, variableId) ?: return
        val box = room.furni[variable.boxId] ?: return
        if (level < STAFF && !(box.ownerId == playerId && masks(room).mayModify(level))) return
        val target = variable.entry.target
        if (target != VariableTarget.USER && target != VariableTarget.FURNI) return
        val on = VariableOn(scope)
        variables.allHolders(room, variable).keys.toList().forEach {
            variable.remove(on, it, ChangeOrigin.EXTERNAL)
        }
    }

    fun permanent(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        type: Int,
        id: Int,
    ) {
        if (!masks(scope.room).mayRead(level) || type != ENTITY_USER) return
        val card = store.userCard(id) ?: return
        val values = variables.storedOf(scope.room, id)
        val body = PacketWriter()
        body.int(values.size)
        values.forEach { (variable, held) ->
            body.string(variable.id)
            storage(body, VariableWire.shown(variable.entry, held.value), held)
        }
        scope.out.send(
            Lane.Now,
            Audience.Player(playerId),
            WiredPlayerPermanentVariablesOutCodec,
            WiredPlayerPermanentVariablesOut(type, id, card.first, card.second, body.toByteArray()),
        )
    }

    fun setPermanent(
        scope: RoomScope,
        playerId: Int,
        level: Int,
        request: PermanentWrite,
    ) {
        if (!masks(scope.room).mayModify(level)) return
        val to = Audience.Player(playerId)
        val variable = variables.find(scope.room, request.variableId)
        val done = variable != null && applyPermanent(scope, variable, request)
        if (!done) {
            scope.out.send(Lane.Now, to, WiredMenuErrorOutCodec, WiredMenuErrorOut(REFUSED))
        }
        scope.out.send(
            Lane.Now,
            to,
            WiredSetPlayerPermanentVariableResultOutCodec,
            WiredSetPlayerPermanentVariableResultOut(done),
        )
        if (done) permanent(scope, playerId, level, request.entityType, request.userId)
    }

    private fun applyPermanent(
        scope: RoomScope,
        variable: Variable,
        request: PermanentWrite,
    ): Boolean {
        val entry = variable.entry
        val permanentUser = entry.target == VariableTarget.USER && Availability.isStored(entry.availability)
        if (!permanentUser || store.userCard(request.userId) == null) return false
        val on = VariableOn(scope)
        val holder = Holder.Player(request.userId)
        val value = request.value.toLong()
        val createsOrDeletes = entry.flags.canCreateAndDelete
        return when (request.action) {
            ACTION_SET -> variable.write(on, holder, value, ChangeOrigin.EXTERNAL)
            ACTION_CREATE -> createsOrDeletes && variable.give(on, holder, value, true, ChangeOrigin.EXTERNAL)
            ACTION_DELETE -> createsOrDeletes && variable.remove(on, holder, ChangeOrigin.EXTERNAL)
            else -> false
        }
    }

    companion object {
        private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")

        /** A held value's time as the menu's windows show it. */
        internal fun stamp(
            millis: Long,
            zone: ZoneId,
        ): String = STAMP.withZone(zone).format(Instant.ofEpochMilli(millis))

        private const val CHUNK = 100
        private const val MAX_PAGE_SIZE = 100
        private const val NOT_FOUND: Short = 0
        private const val REFUSED: Short = 1
        private const val BAD_PAGE_SIZE: Short = 3
        private const val ACTION_SET = 0
        private const val ACTION_CREATE = 1
        private const val ACTION_DELETE = 2
        private const val USER_LIST = 2
        private const val FURNI_LIST = 1
        private const val ENTITY_USER = 1
        private const val ENTITY_FURNI = 2
        private const val STAFF = 5
    }
}
