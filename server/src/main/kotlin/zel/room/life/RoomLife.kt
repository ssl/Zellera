package zel.room.life

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import java.util.concurrent.ConcurrentHashMap

@Suppress("TooManyFunctions") // one per room life event
class RoomLife(
    private val hooks: LifeHooks,
    times: () -> LifeTimes = { LifeTimes() },
    unload: (Int) -> Unit,
) : RoomPart {
    /** Rooms' life state; each entry is read and written only under its room's lock. */
    private val rooms = ConcurrentHashMap<Room, RoomLifeState>()

    private val byUnit = ConcurrentHashMap<RoomUnit, UnitLife>()

    private val idle = Idle(hooks, times)
    private val carried = Carried()
    private val decoHosting = DecoHosting(hooks)
    private val emptyRooms = EmptyRooms(hooks, unload)

    val decoHostingPart: TickPart =
        TickPart { scope ->
            val state = stateOf(scope.room)
            state.ticks++
            if (times().decoHosting) decoHosting.tick(scope, state)
        }

    /** A room that never unloads keeps ticking empty. */
    val emptyRoomPart: TickPart =
        TickPart { scope ->
            val state = stateOf(scope.room)
            prune(scope.room, state)
            emptyRooms.tick(scope, state)
        }

    fun playersPart(steps: TickPart): TickPart =
        TickPart { scope ->
            val state = stateOf(scope.room)
            emptyRooms.occupied(state)
            prune(scope.room, state)
            val leaving = checkPlayers(scope)
            steps.run(scope)
            leaving.forEach { unit ->
                if (scope.room.units[unit.index] === unit) {
                    hooks.restoreLook(scope, unit)
                    hooks.leave(scope, unit)
                }
            }
        }

    override fun load(room: Room) {
        rooms[room] = RoomLifeState()
    }

    override fun unload(scope: RoomScope) {
        scope.room.units
            .all()
            .filter { it.kind is UnitKind.Player }
            .forEach { hooks.leave(scope, it) }
        rooms
            .remove(scope.room)
            ?.units
            ?.values
            ?.forEach { byUnit.remove(it.unit) }
    }

    fun active(
        scope: RoomScope,
        unit: RoomUnit,
    ) = idle.wake(scope, unit, lifeOf(scope, unit))

    fun sleep(
        scope: RoomScope,
        unit: RoomUnit,
    ) = idle.sleep(scope, unit, lifeOf(scope, unit))

    fun asleep(unit: RoomUnit): Boolean = byUnit[unit]?.asleep == true

    internal val followed: Int get() = byUnit.size

    fun effect(unit: RoomUnit): Int = byUnit[unit]?.effect ?: 0

    fun handItem(unit: RoomUnit): Int = byUnit[unit]?.handItem ?: 0

    /** Whether the unit holds `@handitem`: something in its hand, or the variable given with an empty hand. */
    fun handItemHeld(unit: RoomUnit): Boolean = byUnit[unit]?.let { it.handItem != 0 || it.handVariable } == true

    fun markHandItem(
        scope: RoomScope,
        unit: RoomUnit,
        held: Boolean,
    ) {
        lifeOf(scope, unit).handVariable = held
    }

    fun holdItem(
        scope: RoomScope,
        unit: RoomUnit,
        item: Int,
    ) {
        carried.holdItem(scope, unit, lifeOf(scope, unit), item)
        val player = unit.kind as? UnitKind.Player
        if (item > 0 && player != null) hooks.handItemReceived(player.playerId, item)
    }

    fun dropItem(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        holdItem(scope, unit, 0)
        active(scope, unit)
    }

    fun giveEffect(
        scope: RoomScope,
        unit: RoomUnit,
        effect: Int,
        endsAt: Long? = null,
    ) = carried.giveEffect(scope, unit, lifeOf(scope, unit), effect, endsAt)

    private fun checkPlayers(scope: RoomScope): List<RoomUnit> =
        scope.room.units
            .all()
            .filter { it.kind is UnitKind.Player }
            .filter { unit ->
                val life = lifeOf(scope, unit)
                carried.expire(scope, unit, life)
                idle.tick(scope, unit, life)
            }

    private fun prune(
        room: Room,
        state: RoomLifeState,
    ) {
        val gone = state.units.values.filter { room.units[it.unit.index] !== it.unit }
        gone.forEach {
            state.units.remove(it.unit.index)
            byUnit.remove(it.unit)
        }
    }

    private fun lifeOf(
        scope: RoomScope,
        unit: RoomUnit,
    ): UnitLife {
        val state = stateOf(scope.room)
        val track = state.units[unit.index]?.takeIf { it.unit === unit }
        if (track != null) return track.life
        state.units.remove(unit.index)?.let { byUnit.remove(it.unit) }
        val fresh = UnitTrack(unit)
        state.units[unit.index] = fresh
        byUnit[unit] = fresh.life
        return fresh.life
    }

    private fun stateOf(room: Room): RoomLifeState = rooms.getOrPut(room) { RoomLifeState() }
}
