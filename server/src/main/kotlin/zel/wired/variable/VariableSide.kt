package zel.wired.variable

import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomRegistry
import zel.room.furni.behaviour.state.InvisibleFurni
import zel.room.furni.behaviour.state.StatePart
import zel.room.furni.place.UnitsOnTiles
import zel.wired.BoxSettingsSource
import zel.wired.WiredRooms
import zel.wired.box.BoxType
import zel.wired.box.WiredBox
import zel.wired.effect.furni.FurniMoves
import zel.wired.effect.furni.WallMoves
import zel.wired.form.BoxValidation
import zel.wired.form.DialogContexts
import zel.wired.form.WiredMasks
import zel.wired.protocol.BoxUse
import zel.wired.store.WiredWriter
import zel.wired.timing.RoomPoster
import zel.wired.usage.UsageAllowance
import zel.wired.usage.UsageLimit
import zel.wired.variable.box.VariableBoxes
import zel.wired.variable.box.VariableRules
import java.util.concurrent.atomic.AtomicReference

class VariableSide(
    store: VariableStore,
    forms: BoxSettingsSource,
    writer: WiredWriter,
    rooms: () -> WiredRooms?,
    poster: RoomPoster,
    units: UnitsOnTiles,
    state: StatePart,
    facts: (spaces: (Room) -> VariableSpace) -> VariableFacts,
    tables: ValueTables = ValueTables.None,
    addons: VariableAddons = VariableAddons.None,
    changeListeners: List<(VariableOn, VariableChange) -> Unit> = emptyList(),
    allowance: (Room) -> UsageAllowance = { UsageLimit.of(0) },
) {
    private val built = AtomicReference<RoomVariables>()
    private val spaces: (Room) -> VariableSpace = { room -> checkNotNull(built.get()).space(room) }
    private val services = facts(spaces)
    private val moves = FurniMoves(units, poster) { InvisibleFurni.inView(state, it) }
    private val values = InternalValues(services, { spaces(it.room) })
    val projectiles = ProjectileFlights(spaces, poster) { on, change -> built.get()?.raise(on, change) }
    private val writes =
        InternalWrites(services, { spaces(it.room) }, moves, WallMoves(moves), state, UnitSlides(poster, moves))

    val variables =
        RoomVariables(
            store,
            forms,
            writer,
            VariableChanges(rooms, changeListeners),
            InternalVariables(values, writes, tables),
            addons,
            poster,
        ).also(built::set)

    private val store = store
    private val rooms = rooms

    val types: List<BoxType<WiredBox>> = VariableBoxes(variables).types

    val validations: Map<String, BoxValidation> = VariableRules(variables, allowance).validations

    val part: RoomPart get() = variables

    val facts: VariableFacts get() = services

    val listener: RoomListener get() = variables

    val pickupConfirm = VariableBoxPickup(variables)

    fun contexts(next: DialogContexts): DialogContexts = VariableContexts(variables, next)

    fun hash(room: Room): Int = VariableEntry.listHash(variables.listed(room).map { it.entry })

    fun packets(
        registry: RoomRegistry,
        masks: (Room) -> WiredMasks,
        levels: (Room) -> Map<Int, Int>,
    ): VariablePackets = VariablePackets(registry, VariableMenu(variables, masks, rooms, store), levels)

    fun permanentCounts(room: Room): Triple<Int, Int, Int> {
        val stored = variables.customs(room).filter { Availability.isStored(it.entry.availability) }

        fun count(target: Int) = stored.count { it.entry.target == target }
        return Triple(count(VariableTarget.FURNI), count(VariableTarget.USER), count(VariableTarget.GLOBAL))
    }

    fun behaviours(use: BoxUse): List<Pair<String, VariableBoxFurni>> =
        VariableBoxes.DEFINITIONS.map { it to VariableBoxFurni(use, variables) }
}
