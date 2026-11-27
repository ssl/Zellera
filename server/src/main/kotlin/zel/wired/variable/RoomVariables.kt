package zel.wired.variable

import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.place.Interactions
import zel.wired.BoxSettingsSource
import zel.wired.box.BoxSettings
import zel.wired.box.WiredBox
import zel.wired.firing.Firing
import zel.wired.firing.HeldValue
import zel.wired.firing.Targets
import zel.wired.store.WiredWriter
import zel.wired.timing.RoomPoster
import java.util.concurrent.ConcurrentHashMap

class RoomVariables(
    private val store: VariableStore,
    private val forms: BoxSettingsSource,
    private val writer: WiredWriter,
    private val changes: VariableChanges,
    val internal: InternalVariables,
    private val addons: VariableAddons = VariableAddons.None,
    private val poster: RoomPoster = RoomPoster.None,
    val clock: () -> Long = System::currentTimeMillis,
) : RoomPart,
    RoomListener {
    private data class SharedKey(
        val roomId: Int,
        val boxId: Int,
    )

    private val spaces = ConcurrentHashMap<Room, VariableSpace>()

    private val shared = ConcurrentHashMap<SharedKey, MutableMap<Holder, HeldValue>>()

    private val sources = ConcurrentHashMap<SharedKey, Boolean>()

    /** Whether a shared box holds a value, as [hasValueOf] reads its settings; a reference takes its flags from it. */
    private val valued = ConcurrentHashMap<SharedKey, Boolean>()

    private val pinned = ThreadLocal<Long>()

    private val stamp: () -> Long = { pinned.get() ?: clock() }

    /** Runs [body] with every value written on this thread stamped with one time. */
    fun <T> atOneTime(body: () -> T): T {
        val previous = pinned.get()
        pinned.set(previous ?: clock())
        try {
            return body()
        } finally {
            if (previous == null) pinned.remove() else pinned.set(previous)
        }
    }

    override fun load(room: Room) {
        spaces[room] = VariableSpace(store.roomValues(room.info.id), clock())
    }

    override fun unload(scope: RoomScope) {
        spaces.remove(scope.room)
        writer.flush(scope.room.info.id)
    }

    override fun on(
        scope: RoomScope,
        event: RoomEvent,
    ) {
        val space = space(scope.room)
        if (event is RoomEvent.Entered) {
            scope.room.units[event.unitIndex]?.let { space.players[event.unitIndex] = Holder.of(it) }
        } else if (event is RoomEvent.Left) {
            space.players.remove(event.unitIndex)?.let { left(scope.room, space, it) }
            space.icons.remove(event.unitIndex)
        }
    }

    private fun left(
        room: Room,
        space: VariableSpace,
        holder: Holder,
    ) {
        customs(room)
            .filter { it.entry.availability == Availability.WHILE_IN_ROOM }
            .forEach { space.homes[it.boxId]?.values?.remove(holder) }
    }

    fun space(room: Room): VariableSpace =
        spaces.getOrPut(room) { VariableSpace(store.roomValues(room.info.id), clock()) }

    fun find(
        room: Room,
        id: String,
    ): Variable? =
        when {
            id.isEmpty() || id == NONE -> null
            id.startsWith('-') || id.startsWith('~') -> internal.byId(room, id)
            id.startsWith(SUB_PREFIX) && !id.startsWith(SHARED_PREFIX) -> subvariable(room, id)
            else -> custom(room, id)
        }

    private fun custom(
        room: Room,
        id: String,
    ): Variable? {
        val boxId = id.substringAfter(':', "").toIntOrNull() ?: return null
        val furni = room.furni[boxId] ?: return null
        return definition(room, furni)?.takeIf { it.id == id }
    }

    /** An echo has no subvariables of its own, and building one here would loop when it echoes a subvariable. */
    private fun subvariable(
        room: Room,
        id: String,
    ): Variable? =
        room.furni
            .all()
            .filter { Interactions.of(it.definition).let { type -> type.startsWith(BOX_PREFIX) && type != ECHO_BOX } }
            .sortedBy { it.id }
            .mapNotNull { definition(room, it) }
            .firstNotNullOfOrNull { variable ->
                room.furni[variable.boxId]?.let { box ->
                    addons.subvariables(room, variable, box).firstOrNull { it.id == id }
                }
            }

    fun customs(room: Room): List<Variable> =
        room.furni
            .all()
            .filter { Interactions.of(it.definition).startsWith(BOX_PREFIX) }
            .sortedBy { it.id }
            .mapNotNull { definition(room, it) }

    /** The wired boxes of the room that have the furni picked, by id. */
    fun pickedBy(
        room: Room,
        furniId: Int,
    ): List<Int> =
        room.furni
            .all()
            .filter { Interactions.isWiredBox(Interactions.of(it.definition)) }
            .filter { forms.settings(room, it)?.picks?.contains(furniId) == true }
            .map { it.id }
            .sorted()

    fun listed(room: Room): List<Variable> {
        val customs = customs(room)
        val subvariables =
            customs.flatMap { variable ->
                room.furni[variable.boxId]?.let { addons.subvariables(room, variable, it) }.orEmpty()
            }
        val byTarget = compareBy<Variable> { TARGET_ORDER.indexOf(it.entry.target) }.thenBy { it.entry.name }
        return (customs + subvariables).sortedWith(byTarget) + internal.listed(room)
    }

    /** Null without a level-up system on its box's tile. */
    fun levelsOf(
        room: Room,
        variable: Variable,
    ): ((Long) -> Int)? = room.furni[variable.boxId]?.let { addons.levels(room, it) }

    fun definition(
        room: Room,
        furni: RoomFurni,
    ): Variable? {
        val settings = forms.settings(room, furni) ?: return null
        if (settings.text.isEmpty()) return null
        return when (Interactions.of(furni.definition)) {
            USER_BOX -> user(room, furni, settings)
            FURNI_BOX -> furni(room, furni, settings)
            ROOM_BOX -> global(room, furni, settings)
            CONTEXT_BOX -> context(room, furni, settings)
            POINTER_BOX -> pointer(furni, settings)
            REFERENCE_BOX -> reference(room, furni, settings)
            ECHO_BOX -> echo(room, furni, settings)
            else -> null
        }
    }

    private fun user(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): Variable {
        val availability = settings.ints.getOrElse(0) { 0 }
        val hasValue = hasValueOf(USER_BOX, settings)
        val entry =
            VariableEntry(
                "$USER_PREFIX:${box.id}",
                CUSTOM,
                settings.text,
                availability,
                VariableTarget.USER,
                VariableFlags.of(if (hasValue) HELD_VALUE else HELD_FLAG),
                addons.texts(room, box),
            )
        return plain(room, box, entry)
    }

    private fun furni(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): Variable {
        val hasValue = hasValueOf(FURNI_BOX, settings)
        val availability = settings.ints.getOrElse(1) { 1 }
        val entry =
            VariableEntry(
                "$FURNI_PREFIX:${box.id}",
                CUSTOM,
                settings.text,
                availability,
                VariableTarget.FURNI,
                VariableFlags.of(if (hasValue) HELD_VALUE else HELD_FLAG),
                addons.texts(room, box),
            )
        return plain(room, box, entry)
    }

    private fun global(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): Variable {
        val entry =
            VariableEntry(
                "$ROOM_PREFIX:${box.id}",
                CUSTOM,
                settings.text,
                settings.ints.getOrElse(0) { 1 },
                VariableTarget.GLOBAL,
                VariableFlags.of(GLOBAL_FLAGS),
                addons.texts(room, box),
            )
        return plain(room, box, entry)
    }

    private fun context(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): Variable {
        val hasValue = settings.ints.getOrElse(0) { 0 } != 0
        val entry =
            VariableEntry(
                "$CONTEXT_PREFIX:${box.id}",
                CUSTOM,
                settings.text,
                Availability.EXECUTION,
                VariableTarget.CONTEXT,
                VariableFlags.of(if (hasValue) CONTEXT_VALUE else CONTEXT_FLAG),
                addons.texts(room, box),
            )
        return ContextVariable(entry, box.id, stamp)
    }

    private fun pointer(
        box: RoomFurni,
        settings: BoxSettings,
    ): Variable {
        val furni = settings.ints.getOrElse(0) { 1 } == VariableTarget.FURNI
        val prefix = if (furni) FURNI_PREFIX else USER_PREFIX
        val entry =
            VariableEntry(
                "$prefix:${box.id}",
                CUSTOM,
                settings.text,
                Availability.EXECUTION,
                if (furni) VariableTarget.FURNI else VariableTarget.USER,
                VariableFlags.of(POINTER_FLAGS),
                ValueTexts(emptyList()),
            )
        return PointerVariable(entry, box.id)
    }

    private fun reference(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): Variable? {
        val source = SharedToken.parse(settings.variableIds.firstOrNull().orEmpty()) ?: return null
        val readOnly = settings.ints.getOrElse(0) { 1 } != 0
        val user = source.target == VariableTarget.USER
        val stands = sourceStands(source)
        val hasValue = !user || !stands || sourceHasValue(source)
        val bits =
            when {
                user && readOnly -> if (hasValue) USER_REFERENCE_READ else USER_FLAG_REFERENCE_READ
                user -> if (hasValue) HELD_VALUE else HELD_FLAG
                readOnly -> GLOBAL_REFERENCE_READ
                else -> GLOBAL_REFERENCE
            }
        val entry =
            VariableEntry(
                "${if (user) USER_PREFIX else ROOM_PREFIX}:${box.id}",
                CUSTOM,
                settings.text,
                Availability.REFERENCE,
                source.target,
                VariableFlags.of(bits),
                addons.texts(room, box),
            )
        if (!stands) return PointerVariable(entry, box.id)
        val home =
            ValueHome(
                source.roomId,
                source.boxId,
                stored = true,
                sharedValues(source.roomId, source.boxId, source.target),
                store,
                writer,
            )
        val inner = PlainVariable(entry, box.id, home, changes, stamp)
        return if (readOnly) inner else ReferenceVariable(inner, source, poster, this)
    }

    private fun echo(
        room: Room,
        box: RoomFurni,
        settings: BoxSettings,
    ): Variable? {
        val source = find(room, settings.variableIds.firstOrNull().orEmpty()) ?: return null
        val target = source.entry.target
        val prefix =
            when (target) {
                VariableTarget.USER -> USER_PREFIX
                VariableTarget.GLOBAL -> ROOM_PREFIX
                else -> FURNI_PREFIX
            }
        val entry =
            VariableEntry(
                "$prefix:${box.id}",
                CUSTOM,
                settings.text,
                if (target == VariableTarget.USER) Availability.WHILE_IN_ROOM else Availability.WHILE_ROOM_ACTIVE,
                target,
                VariableFlags.of(if (target == VariableTarget.GLOBAL) GLOBAL_FLAGS else HELD_VALUE),
                addons.texts(room, box),
            )
        return EchoVariable(entry, box.id, source)
    }

    private fun plain(
        room: Room,
        box: RoomFurni,
        entry: VariableEntry,
    ): Variable {
        val inner = PlainVariable(entry, box.id, home(room, box.id, entry), changes, stamp)
        val shared = entry.availability == Availability.SHARED && entry.target != VariableTarget.FURNI
        return if (shared) SharedSourceVariable(inner, room.info.id, this) else inner
    }

    private fun home(
        room: Room,
        boxId: Int,
        entry: VariableEntry,
    ): ValueHome {
        val space = space(room)
        val stored = Availability.isStored(entry.availability)
        val isShared = entry.availability == Availability.SHARED
        space.homes[boxId]?.takeIf { it.stored == stored && (it.values is ConcurrentHashMap<*, *>) == isShared }?.let {
            return it
        }
        val roomId = room.info.id
        val held = space.homes[boxId]?.values
        val loaded = held ?: if (stored) space.loaded(boxId, entry.target) else emptyMap()
        val values: MutableMap<Holder, HeldValue> =
            if (isShared) {
                sources[SharedKey(roomId, boxId)] = true
                shared.getOrPut(SharedKey(roomId, boxId)) { ConcurrentHashMap(loaded) }
            } else {
                HashMap(loaded)
            }
        return ValueHome(roomId, boxId, stored, values, store, writer).also { space.homes[boxId] = it }
    }

    private fun sourceStands(source: SharedToken): Boolean =
        sources.getOrPut(SharedKey(source.roomId, source.boxId)) {
            store.sharedBoxInts(source.roomId, source.boxId)?.getOrNull(0) == Availability.SHARED
        }

    /** A source box without settings counts as holding a value. */
    private fun sourceHasValue(source: SharedToken): Boolean =
        valued.getOrPut(SharedKey(source.roomId, source.boxId)) {
            val code = if (source.target == VariableTarget.USER) USER_BOX else ROOM_BOX
            store.sharedBoxInts(source.roomId, source.boxId)?.let { hasValueOf(code, it) } ?: true
        }

    private fun sharedValues(
        roomId: Int,
        boxId: Int,
        target: Int,
    ): MutableMap<Holder, HeldValue> =
        shared.getOrPut(SharedKey(roomId, boxId)) {
            val rows = store.boxValues(roomId, boxId)
            val loaded: Map<Holder, HeldValue> =
                when (target) {
                    VariableTarget.USER -> rows.users[boxId].orEmpty().mapKeys { Holder.Player(it.key) }
                    else -> rows.rooms[boxId]?.let { mapOf<Holder, HeldValue>(Holder.Room to it) }.orEmpty()
                }
            ConcurrentHashMap(loaded)
        }

    fun redefined(
        room: Room,
        box: RoomFurni,
        next: BoxSettings,
    ) {
        val space = space(room)
        val code = Interactions.of(box.definition)
        val availability = availabilityOf(code, next) ?: return
        val roomId = room.info.id
        val key = SharedKey(roomId, box.id)
        if (availability == Availability.SHARED) valued[key] = hasValueOf(code, next) else valued.remove(key)
        val home = space.homes[box.id]
        // references elsewhere follow a switch of sharing; the next home starts from this room's current values
        if (availability != Availability.SHARED) {
            shared.remove(key)
            sources.computeIfPresent(key) { _, _ -> false }
        } else {
            if (home != null && home.values !is ConcurrentHashMap<*, *>) shared.remove(key)
            sources[key] = true
        }
        val old = home ?: return
        val storedNext = Availability.isStored(availability)
        if (old.stored == storedNext) return
        if (old.stored) {
            writer.write(roomId, Runnable { store.deleteBox(roomId, box.id) })
        } else {
            val hasValue = hasValueOf(code, next)
            old.values.forEach { (holder, held) -> saved(roomId, box.id, holder, held, hasValue) }
        }
    }

    private fun saved(
        roomId: Int,
        boxId: Int,
        holder: Holder,
        held: HeldValue,
        hasValue: Boolean,
    ) {
        val value = held.value.takeIf { hasValue }
        when (holder) {
            is Holder.Player -> {
                writer.write(roomId, Runnable { store.saveUser(roomId, holder.id, boxId, value, held) })
            }

            is Holder.Furni -> {
                writer.write(roomId, Runnable { store.saveFurni(roomId, holder.id, boxId, value, held) })
            }

            Holder.Room -> {
                writer.write(roomId, Runnable { store.saveRoom(roomId, boxId, held) })
            }

            else -> {}
        }
    }

    /** A flag variable (no value) stores its holders without a value, as [ValueHome] does. */
    private fun hasValueOf(
        code: String,
        settings: BoxSettings,
    ): Boolean = hasValueOf(code, settings.ints)

    private fun hasValueOf(
        code: String,
        ints: List<Int>,
    ): Boolean =
        when (code) {
            USER_BOX -> ints.getOrElse(1) { 0 } != 0
            FURNI_BOX -> ints.getOrElse(0) { 0 } != 0
            else -> true
        }

    private fun availabilityOf(
        code: String,
        settings: BoxSettings,
    ): Int? =
        when (code) {
            USER_BOX, ROOM_BOX -> settings.ints.getOrNull(0)
            FURNI_BOX -> settings.ints.getOrNull(1)
            else -> null
        }

    fun boxGone(
        room: Room,
        box: RoomFurni,
    ) {
        val roomId = room.info.id
        val boxId = box.id
        space(room).homes.remove(boxId)
        shared.remove(SharedKey(roomId, boxId))
        sources[SharedKey(roomId, boxId)] = false
        valued.remove(SharedKey(roomId, boxId))
        writer.write(roomId, Runnable { store.deleteBox(roomId, boxId) })
    }

    fun referencedTokens(room: Room): Set<String> =
        room.furni
            .all()
            .filter { Interactions.of(it.definition) == REFERENCE_BOX }
            .mapNotNullTo(HashSet()) { tokenOf(room, it) }

    fun tokenOf(
        room: Room,
        box: RoomFurni,
    ): String? =
        forms
            .settings(room, box)
            ?.variableIds
            ?.firstOrNull()
            ?.takeIf { SharedToken.parse(it) != null }

    fun raise(
        on: VariableOn,
        change: VariableChange,
    ) = changes.raise(on, change)

    /**
     * A change of the shared variable of [sourceBoxId] in [sourceRoomId], raised in every other loaded room on each of
     * its references (the source room raised its own already).
     */
    fun sharedChanged(
        sourceRoomId: Int,
        sourceBoxId: Int,
        holder: Holder,
        origin: Int,
        type: Int,
        old: Long?,
        new: Long?,
    ) {
        val reached = spaces.keys.mapTo(HashSet()) { it.info.id } - sourceRoomId
        reached.forEach { roomId ->
            poster.post(roomId, 0) { scope ->
                val room = scope.room
                room.furni
                    .all()
                    .filter { Interactions.of(it.definition) == REFERENCE_BOX }
                    .sortedBy { it.id }
                    .filter { box ->
                        val token = tokenOf(room, box)?.let(SharedToken::parse)
                        token?.roomId == sourceRoomId && token.boxId == sourceBoxId
                    }.forEach { box ->
                        val reference = definition(room, box) ?: return@forEach
                        raise(VariableOn(scope), VariableChange(reference.id, box.id, holder, origin, type, old, new))
                    }
            }
        }
    }

    fun readFor(
        firing: Firing,
        box: WiredBox,
        variableSlot: Int,
        target: Int,
        furniSlot: Int,
        userSlot: Int,
    ): Long? {
        val variable =
            find(
                firing.scope.room,
                box.settings.variableIds
                    .getOrNull(variableSlot)
                    .orEmpty(),
            ) ?: return null
        val holder =
            when (target) {
                VariableTarget.USER -> Targets.units(firing, box, userSlot).firstOrNull()?.let { Holder.of(it) }
                VariableTarget.FURNI -> Targets.furni(firing, box, furniSlot).firstOrNull()?.let { Holder.of(it) }
                VariableTarget.GLOBAL -> Holder.Room
                VariableTarget.CONTEXT -> Holder.Context
                else -> null
            } ?: return null
        return variable.read(VariableOn(firing.scope, firing), holder)?.value
    }

    fun iconShown(
        room: Room,
        unitIndex: Int,
        iconId: Int,
    ) {
        space(room).icons[unitIndex] = iconId to clock() + ICON_MILLIS
    }

    fun names(ids: Collection<Int>): Map<Int, String> = store.userNames(ids)

    fun sharedOffers(room: Room): List<SharedOffer> = store.sharedOffers(room.info.ownerId, room.info.id)

    fun storedOf(
        room: Room,
        playerId: Int,
    ): List<Pair<Variable, HeldValue>> {
        val holder = Holder.Player(playerId)
        return customs(room)
            .filter { Availability.isStored(it.entry.availability) && it.entry.target == VariableTarget.USER }
            .mapNotNull { variable ->
                space(room)
                    .homes[variable.boxId]
                    ?.values
                    ?.get(holder)
                    ?.let { variable to it }
            }
    }

    fun holds(
        room: Room,
        variable: Variable,
        holder: Holder,
    ): Boolean = space(room).homes[variable.boxId]?.values?.containsKey(holder) == true

    fun allHolders(
        room: Room,
        variable: Variable,
    ): Map<Holder, HeldValue> = space(room).homes[variable.boxId]?.values.orEmpty()

    companion object {
        const val NONE = "0"
        private const val ICON_MILLIS = 3000L
        const val BOX_PREFIX = "wf_var_"
        const val USER_BOX = "wf_var_user"
        const val FURNI_BOX = "wf_var_furni"
        const val ROOM_BOX = "wf_var_room"
        const val CONTEXT_BOX = "wf_var_context"
        const val POINTER_BOX = "wf_var_pointer"
        const val REFERENCE_BOX = "wf_var_reference"
        const val ECHO_BOX = "wf_var_echo"
        const val USER_PREFIX = "user"
        const val FURNI_PREFIX = "furni"
        const val ROOM_PREFIX = "room"
        const val CONTEXT_PREFIX = "ctx"
        const val SHARED_PREFIX = "shared:"
        private const val SUB_PREFIX = "s"
        private const val CUSTOM = 0
        const val HELD_VALUE = "011110011"
        const val HELD_FLAG = "010010010"
        const val GLOBAL_FLAGS = "101110001"
        const val CONTEXT_VALUE = "011100011"
        const val CONTEXT_FLAG = "010000010"
        const val POINTER_FLAGS = "011100111"
        const val USER_REFERENCE_READ = "001010011"
        const val USER_FLAG_REFERENCE_READ = "000010010"
        const val GLOBAL_REFERENCE_READ = "001010001"
        const val GLOBAL_REFERENCE = "001110001"
        private val TARGET_ORDER =
            listOf(VariableTarget.GLOBAL, VariableTarget.USER, VariableTarget.FURNI, VariableTarget.CONTEXT)
    }
}
