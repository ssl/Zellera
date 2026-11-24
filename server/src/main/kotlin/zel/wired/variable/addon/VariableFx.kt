package zel.wired.variable.addon

import zel.protocol.generated.PacketWriter
import zel.protocol.generated.VariableFxConfigsOut
import zel.protocol.generated.VariableFxConfigsOutCodec
import zel.protocol.generated.VariableFxConfigsRemovedOut
import zel.protocol.generated.VariableFxConfigsRemovedOutCodec
import zel.protocol.generated.VariableFxStatusOut
import zel.protocol.generated.VariableFxStatusOutCodec
import zel.protocol.generated.VariableFxStatusRemovedOut
import zel.protocol.generated.VariableFxStatusRemovedOutCodec
import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.wired.BoxSettingsSource
import zel.wired.form.DialogSpecs
import zel.wired.store.RoomLog
import zel.wired.timing.RoomPoster
import zel.wired.usage.WiredError
import zel.wired.variable.Availability
import zel.wired.variable.Holder
import zel.wired.variable.RoomVariables
import zel.wired.variable.Variable
import zel.wired.variable.VariableChange
import zel.wired.variable.VariableOn
import zel.wired.variable.VariableTarget
import zel.wired.variable.entityIn
import java.util.concurrent.ConcurrentHashMap

class VariableFx(
    private val forms: BoxSettingsSource,
    private val stack: StackAddons,
    private val copies: ImportCopies,
    private val variables: () -> RoomVariables,
    private val poster: RoomPoster,
    private val team: (RoomScope, Int) -> Int?,
    private val codeOfBase: (Int) -> String?,
    private val roomLog: (Room, RoomLog.Line) -> Unit = { _, _ -> },
    private val wallClock: () -> Long = System::currentTimeMillis,
) : RoomPart {
    private class ActiveFx(
        val config: FxConfig,
        val form: FxForm,
        val variableIds: List<String>,
        val box: RoomFurni,
        val copied: Boolean,
    )

    private data class StatusKey(
        val configId: Int,
        val variableId: String,
        val holder: Holder,
    )

    private class RoomFx {
        val lastSent = HashMap<Int, FxConfig>()
        val dirty = LinkedHashSet<StatusKey>()
        var flushPosted = false

        val importIds = HashMap<Pair<Int, Int>, Int>()
        var lowestImportId = 0
        var numbered = false
        var synchronized = false
    }

    private val rooms = ConcurrentHashMap<Room, RoomFx>()

    private fun state(room: Room): RoomFx = rooms.getOrPut(room) { RoomFx() }

    override fun unload(scope: RoomScope) {
        rooms.remove(scope.room)
        copies.forget(scope.room)
    }

    override fun load(room: Room) {
        room.furni
            .all()
            .filter { FxKind.fromCode(Interactions.of(it.definition)) != null }
            .groupBy { it.anchor }
            .filterValues { it.size > MAX_PER_STACK }
            .values
            .forEach { _ -> overLimit(room) }
    }

    /** An FX past the fifth of its stack, placed or saved: the room log says so, the box itself still saves. */
    private fun pastLimit(
        room: Room,
        box: RoomFurni,
    ) {
        val tile = box.anchor ?: return
        val stacked =
            room.furni
                .at(tile)
                .furni
                .filter { FxKind.fromCode(Interactions.of(it.definition)) != null }
                .sortedWith(compareBy({ it.z }, { it.id }))
        if (stacked.indexOfFirst { it.id == box.id } >= MAX_PER_STACK) overLimit(room)
    }

    private fun overLimit(room: Room) {
        val now = wallClock()
        val users = room.units.playerIds().size
        val error = "Wired error: ${WiredError.VARIABLE_FX_LIMIT}. (usage=0, users=$users)"
        roomLog(room, RoomLog.Line(ERROR_LEVEL, 0, error, now))
        roomLog(room, RoomLog.Line(LIMIT_LEVEL, 0, "$LIMIT_TEXT $MAX_PER_STACK", now))
    }

    private fun active(room: Room): List<ActiveFx> {
        val own =
            room.furni.all().mapNotNull { box ->
                FxKind.fromCode(Interactions.of(box.definition))?.let { kind ->
                    val settings = forms.settings(room, box) ?: fresh(kind.code)
                    val ints = settings.ints
                    ActiveFx(FxConfig.of(box.id, kind, ints), FxForm(ints), settings.variableIds, box, false)
                }
            }
        return own + imported(room)
    }

    private fun fresh(code: String) =
        DialogSpecs.TABLE
            .getValue(code)
            .form.fresh

    private fun imported(room: Room): List<ActiveFx> {
        val fx = state(room)
        if (!fx.numbered) numberAll(room, fx)
        return importBoxes(room).flatMap { import ->
            if (forms.settings(room, import)?.ints?.getOrElse(StackAddons.SHARED_FX) { 1 } == 0) {
                emptyList()
            } else {
                copies.of(room, import).fx.mapNotNull { copy -> copiedFx(fx, room, import, copy) }
            }
        }
    }

    private fun copiedFx(
        fx: RoomFx,
        room: Room,
        import: RoomFurni,
        copy: CopiedBox,
    ): ActiveFx? {
        val id = fx.importIds[import.id to copy.boxId] ?: return null
        val code = codeOfBase(copy.baseItemId) ?: return null
        val kind = FxKind.fromCode(code) ?: return null
        val settings = fresh(code).copy(ints = copy.ints, text = copy.text)
        val ints = FxConfig.filled(kind, settings).ints
        return ActiveFx(FxConfig.of(id, kind, ints), FxForm(ints), settings.variableIds, import, true)
            .takeIf { room.furni[import.id] != null }
    }

    private fun importBoxes(room: Room): List<RoomFurni> =
        room.furni
            .all()
            .filter { Interactions.of(it.definition) == AddonCodes.IMPORT }
            .sortedByDescending { it.id }

    /** Every import box from -1, highest box first, each box's FX from the source's lowest furni id up. */
    private fun numberAll(
        room: Room,
        fx: RoomFx,
    ) {
        fx.numbered = true
        fx.lowestImportId = 0
        fx.importIds.clear()
        importBoxes(room).forEach { number(room, fx, it) }
    }

    private fun number(
        room: Room,
        fx: RoomFx,
        import: RoomFurni,
    ) {
        copies.of(room, import).fx.sortedBy { it.boxId }.forEach { copy ->
            fx.lowestImportId -= 1
            fx.importIds[import.id to copy.boxId] = fx.lowestImportId
        }
    }

    fun synchronized(
        scope: RoomScope,
        reached: List<RoomFurni>,
    ) {
        val fx = state(scope.room)
        if (fx.synchronized) {
            reached.sortedByDescending { it.id }.forEach { number(scope.room, fx, it) }
        } else {
            fx.synchronized = true
            numberAll(scope.room, fx)
        }
        sendConfigs(scope, Audience.Room, imported(scope.room))
    }

    fun firstSynchronize(room: Room): Boolean = !state(room).synchronized

    private fun sendConfigs(
        scope: RoomScope,
        to: Audience,
        list: List<ActiveFx>,
    ) {
        val state = state(scope.room)
        val sorted = list.sortedByDescending { it.config.id }
        sorted.forEach { state.lastSent[it.config.id] = it.config }
        if (sorted.isEmpty()) return
        scope.out.send(
            Lane.Now,
            to,
            VariableFxConfigsOutCodec,
            VariableFxConfigsOut(sorted.map { it.config.wire() }),
        )
    }

    fun placed(
        scope: RoomScope,
        box: RoomFurni,
    ) {
        val kind = FxKind.fromCode(Interactions.of(box.definition)) ?: return
        val config = FxConfig.of(box.id, kind, fresh(kind.code).ints)
        state(scope.room).lastSent[box.id] = config
        send(scope, config)
        pastLimit(scope.room, box)
    }

    private fun send(
        scope: RoomScope,
        config: FxConfig,
    ) = scope.out.send(
        Lane.Now,
        Audience.Room,
        VariableFxConfigsOutCodec,
        VariableFxConfigsOut(listOf(config.wire())),
    )

    fun saved(
        room: Room,
        box: RoomFurni,
        config: FxConfig,
        accepted: Boolean,
    ) {
        val state = state(room)
        pastLimit(room, box)
        val before = state.lastSent[config.id] ?: currentConfig(room, box)
        if (before == config) return
        state.lastSent[config.id] = config
        poster.post(room.info.id, 0) {
            send(it, config)
            if (accepted) pushToRoomActiveHolders(it, box.id)
        }
    }

    private fun pushToRoomActiveHolders(
        scope: RoomScope,
        boxId: Int,
    ) {
        val room = scope.room
        val all = active(room)
        val fx = all.firstOrNull { it.config.id == boxId && !it.config.forUsers } ?: return
        val on = VariableOn(scope)
        val statuses =
            variablesOf(room, fx)
                .filter { it.entry.availability == Availability.WHILE_ROOM_ACTIVE && fx in shownFor(room, all, it) }
                .flatMap { variable ->
                    variable.holders(on).mapNotNull { holder ->
                        holder.entityIn(room)?.let { entity ->
                            status(on, fx, variable, holder, entity, initialize = true)?.let { fx to it }
                        }
                    }
                }
        sendStatuses(scope, statuses, initializeAll = true, viewers = room.units.playerIds())
    }

    private fun currentConfig(
        room: Room,
        box: RoomFurni,
    ): FxConfig? =
        FxKind.fromCode(Interactions.of(box.definition))?.let { kind ->
            FxConfig.of(box.id, kind, (forms.settings(room, box) ?: fresh(kind.code)).ints)
        }

    fun pickedUp(
        scope: RoomScope,
        box: RoomFurni,
    ) {
        val state = state(scope.room)
        val ids =
            when (Interactions.of(box.definition)) {
                AddonCodes.IMPORT -> {
                    state.importIds
                        .filterKeys { it.first == box.id }
                        .values
                        .toList()
                }

                else -> {
                    listOf(box.id)
                }
            }
        ids.forEach { state.lastSent.remove(it) }
        if (ids.isEmpty()) return
        scope.out.send(
            Lane.Now,
            Audience.Room,
            VariableFxConfigsRemovedOutCodec,
            VariableFxConfigsRemovedOut(ids),
        )
    }

    fun changed(
        on: VariableOn,
        change: VariableChange,
    ) {
        if (change.holder == Holder.Room || change.holder == Holder.Context) return
        val room = on.scope.room
        val fx = active(room).filter { active -> variablesOf(room, active).any { it.id == change.variableId } }
        if (fx.isEmpty()) return
        val state = state(room)
        fx.forEach { state.dirty += StatusKey(it.config.id, change.variableId, change.holder) }
        if (state.flushPosted) return
        state.flushPosted = true
        if (!poster.post(room.info.id, FLUSH_MILLIS, ::flush)) flush(on.scope)
    }

    private fun variablesOf(
        room: Room,
        fx: ActiveFx,
    ): List<Variable> {
        val tile = fx.box.anchor ?: return emptyList()
        val target = if (fx.config.forUsers) VariableTarget.USER else VariableTarget.FURNI
        return room.furni
            .at(tile)
            .furni
            .filter { Interactions.of(it.definition).startsWith(RoomVariables.BOX_PREFIX) }
            .filter { !fx.copied || Interactions.of(it.definition) == RoomVariables.REFERENCE_BOX }
            .mapNotNull { variables().definition(room, it) }
            .filter { it.entry.target == target }
    }

    /** The five lowest config ids. */
    private fun shownFor(
        room: Room,
        all: List<ActiveFx>,
        variable: Variable,
    ): List<ActiveFx> =
        all
            .filter { fx -> variablesOf(room, fx).any { it.id == variable.id } }
            .sortedBy { it.config.id }
            .take(MAX_PER_VARIABLE)

    private fun flush(scope: RoomScope) {
        val state = state(scope.room)
        state.flushPosted = false
        val dirty = state.dirty.toList()
        state.dirty.clear()
        val room = scope.room
        val all = active(room)
        val on = VariableOn(scope)
        val statuses = ArrayList<Pair<ActiveFx, Written>>()
        val removed = ArrayList<String>()
        dirty.forEach { key ->
            val variable = variables().find(room, key.variableId) ?: return@forEach
            val fx = all.firstOrNull { it.config.id == key.configId } ?: return@forEach
            if (fx !in shownFor(room, all, variable)) return@forEach
            if (!showsStatus(variable, key.holder)) return@forEach
            val entity = key.holder.entityIn(room) ?: return@forEach
            val written = status(on, fx, variable, key.holder, entity, initialize = false)
            if (written == null) removed += removedKey(fx, variable, key.holder, entity) else statuses += fx to written
        }
        sendStatuses(scope, statuses, initializeAll = false, viewers = room.units.playerIds())
        if (removed.isNotEmpty()) {
            scope.out.send(
                Lane.Now,
                Audience.Room,
                VariableFxStatusRemovedOutCodec,
                VariableFxStatusRemovedOut(removed),
            )
        }
    }

    fun entered(
        scope: RoomScope,
        unit: RoomUnit,
        playerId: Int,
    ) {
        val room = scope.room
        val all = active(room)
        sendConfigs(scope, Audience.Player(playerId), all)
        if (all.isEmpty()) return
        val on = VariableOn(scope)
        val holders =
            room.units
                .all()
                .map { Holder.of(it) to it.index }
                .plus(room.furni.all().map { Holder.of(it) to it.id })
        val everyone = statusesOf(on, all, holders, initialize = true)
        sendStatuses(scope, everyone, initializeAll = true, viewers = listOf(playerId))
        val own = statusesOf(on, all, listOf(Holder.of(unit) to unit.index), initialize = false)
        sendStatuses(scope, own, initializeAll = false, viewers = room.units.playerIds() - playerId)
    }

    private fun statusesOf(
        on: VariableOn,
        all: List<ActiveFx>,
        holders: List<Pair<Holder, Int>>,
        initialize: Boolean,
    ): List<Pair<ActiveFx, Written>> {
        val room = on.scope.room
        val byVariable =
            all
                .flatMap { fx -> variablesOf(room, fx) }
                .distinctBy { it.id }
        return byVariable
            .flatMap { variable ->
                shownFor(room, all, variable).flatMap { fx ->
                    holders.mapNotNull { (holder, entity) ->
                        status(on, fx, variable, holder, entity, initialize)?.let { fx to it }
                    }
                }
            }.sortedBy { it.first.config.id }
    }

    private fun sendStatuses(
        scope: RoomScope,
        statuses: List<Pair<ActiveFx, Written>>,
        initializeAll: Boolean,
        viewers: Collection<Int>,
    ) {
        if (statuses.isEmpty()) return
        viewers.forEach { viewer ->
            val seen = statuses.filter { (fx, written) -> mayOnView(scope, fx, written.holder, viewer) }
            if (seen.isEmpty()) return@forEach
            val body = PacketWriter().int(seen.size)
            seen.forEach { (_, written) -> body.bytes(written.bytes) }
            scope.out.send(
                Lane.Now,
                Audience.Player(viewer),
                VariableFxStatusOutCodec,
                VariableFxStatusOut(initializeAll, body.toByteArray()),
            )
        }
    }

    private fun mayOnView(
        scope: RoomScope,
        fx: ActiveFx,
        holder: Holder,
        viewer: Int,
    ): Boolean =
        when (fx.form.visibility) {
            ONLY_HOLDER -> {
                holder == Holder.Player(viewer)
            }

            GAME_TEAM -> {
                val own = team(scope, viewer)
                own != null && holder is Holder.Player && team(scope, holder.id) == own
            }

            AUDIENCE_HOLDS -> {
                audienceValue(scope, fx, viewer) != null
            }

            AUDIENCE_VALUE -> {
                audienceValue(scope, fx, viewer) == fx.form.audienceValue
            }

            else -> {
                true
            }
        }

    private fun audienceValue(
        scope: RoomScope,
        fx: ActiveFx,
        viewer: Int,
    ): Long? =
        variables()
            .find(scope.room, fx.variableIds.getOrElse(AUDIENCE_VARIABLE) { "" })
            ?.read(VariableOn(scope), Holder.Player(viewer))
            ?.value

    private class Written(
        val holder: Holder,
        val bytes: ByteArray,
    )

    private fun status(
        on: VariableOn,
        fx: ActiveFx,
        variable: Variable,
        holder: Holder,
        entity: Int,
        initialize: Boolean,
    ): Written? {
        if (!showsStatus(variable, holder)) return null
        val value = variable.read(on, holder)?.value ?: return null
        val out = PacketWriter()
        out.string("${fx.config.id}|${wireId(variable)}")
        out.boolean(initialize)
        out.boolean(fx.config.forUsers)
        out.int(entity)
        out.long(value)
        val table = if (fx.config.kind == FxKind.LEVELLING) tableOf(on.scope.room, fx) else null
        val level = table?.read(value)
        val range = if (table != null && level != null) levelRange(table, level, value) else overrides(on, fx, holder)
        out.boolean(range != null)
        if (range != null) out.long(range.first).long(range.second)
        if (level == null) {
            out.int(0)
        } else {
            out.int(LEVEL_EXTRAS)
            out.string("current_level").string(level.level.toString())
            out.string("is_maxed").string(level.maxed.toString())
            out.string("max_level").string(level.maxLevel.toString())
        }
        return Written(holder, out.toByteArray())
    }

    private fun showsStatus(
        variable: Variable,
        holder: Holder,
    ): Boolean = holder !is Holder.Furni || variable.entry.flags.hasValue

    private fun tableOf(
        room: Room,
        fx: ActiveFx,
    ): LevelTable? =
        if (fx.copied) {
            copies.of(room, fx.box).levels.firstNotNullOfOrNull { copy -> LevelTable.of(copy.ints, copy.text) }
        } else {
            stack.levelTable(room, fx.box)
        }

    private fun levelRange(
        table: LevelTable,
        level: LevelProgress,
        value: Long,
    ): Pair<Long, Long> =
        if (level.maxed) value to value else table.xpForLevel(level.level) to table.xpForLevel(level.level + 1)

    private fun overrides(
        on: VariableOn,
        fx: ActiveFx,
        holder: Holder,
    ): Pair<Long, Long>? {
        val form = fx.form
        if (!form.minFromVariable && !form.maxFromVariable) return null
        val min = if (form.minFromVariable) readFor(on, fx, MIN_VARIABLE, form.minTarget, holder) else null
        val max = if (form.maxFromVariable) readFor(on, fx, MAX_VARIABLE, form.maxTarget, holder) else null
        return (min ?: fx.config.min) to (max ?: fx.config.max)
    }

    private fun readFor(
        on: VariableOn,
        fx: ActiveFx,
        slot: Int,
        target: Int,
        holder: Holder,
    ): Long? {
        val variable = variables().find(on.scope.room, fx.variableIds.getOrElse(slot) { "" }) ?: return null
        return variable.read(on, if (target == VariableTarget.GLOBAL) Holder.Room else holder)?.value
    }

    private fun removedKey(
        fx: ActiveFx,
        variable: Variable,
        holder: Holder,
        entity: Int,
    ): String = "${fx.config.id}|${wireId(variable)}|${if (holder is Holder.Furni) "f" else "u"}|$entity"

    private fun wireId(variable: Variable): String = variable.id.substringAfter(':')

    private companion object {
        /** The statuses go out once the room's current work is done, one per change, not gathered for longer. */
        const val FLUSH_MILLIS = 0L
        const val MAX_PER_VARIABLE = 5
        const val MAX_PER_STACK = 5
        const val ERROR_LEVEL = 2
        const val LIMIT_LEVEL = 3
        const val LIMIT_TEXT = "Exceeded maximum amount of Variable FX configurations per Wired stack:"
        const val ONLY_HOLDER = 0
        const val GAME_TEAM = 1
        const val AUDIENCE_HOLDS = 3
        const val AUDIENCE_VALUE = 4
        const val MIN_VARIABLE = 0
        const val MAX_VARIABLE = 1
        const val AUDIENCE_VARIABLE = 2
        const val LEVEL_EXTRAS = 3
    }
}
