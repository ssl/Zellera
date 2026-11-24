package zel.room.furni.transit

import zel.room.Room
import zel.room.RoomPart
import zel.room.RoomReads
import zel.room.RoomScope
import zel.room.entry.EntryViews
import zel.room.furni.FurniBehaviour
import zel.room.furni.place.FurniAccess
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.walk.Walking
import kotlin.random.Random

/** Every member runs under the room's lock. */
class TransitPart(
    walking: Walking,
    val holds: TransitHolds,
    hooks: TransitHooks,
    private val data: TransitData,
    random: Random = Random.Default,
    guildFields: (Int) -> List<String>? = { null },
    reads: RoomReads = RoomReads.Inline,
) : RoomPart {
    private val rooms = TransitRooms()
    private val rides = Rides(walking, holds, hooks, data, reads)
    private val gates = Gates(hooks)
    private val oneWay = OneWayGates(walking, holds)
    private val dice = DiceRolls(random)

    val behaviours: List<Pair<String, FurniBehaviour>> =
        listOf(
            "transit.door" to DoorTeleport(rooms, rides, data),
            "transit.tile" to TileTeleport(rooms, rides, hooks, data, clubOnly = false),
            "transit.tile.club" to TileTeleport(rooms, rides, hooks, data, clubOnly = true),
            "transit.hopper" to Hopper(rooms, rides, gates, rule = null),
            "transit.hopper.club" to Hopper(rooms, rides, gates, GateRule.Club),
            "transit.hopper.effect" to Hopper(rooms, rides, gates, GateRule.Effect),
            "gate.plain" to PlainGate(gates),
            "gate.club" to MemberGate(rooms, gates, GateRule.Club),
            "gate.group" to MemberGate(rooms, gates, GateRule.Group, guildFields),
            "gate.effect" to MemberGate(rooms, gates, GateRule.Effect),
            "gate.oneway" to OneWayGate(rooms, oneWay),
            "chance.dice" to Dice(rooms, dice),
        )

    val tick: TickPart =
        TickPart { scope ->
            val state = rooms.of(scope.room)
            rides.tick(scope, state)
            resets(scope, state)
            gates.tick(scope, state)
            oneWay.tick(scope, state)
            dice.tick(scope, state)
        }

    fun entryViews(next: EntryViews): EntryViews =
        object : EntryViews by next {
            override fun afterBurst(
                scope: RoomScope,
                unit: RoomUnit,
                playerId: Int,
            ) {
                next.afterBurst(scope, unit, playerId)
                gates.entered(scope, playerId)
            }

            override fun arrivedThrough(
                scope: RoomScope,
                unit: RoomUnit,
                furniId: Int,
            ) {
                next.arrivedThrough(scope, unit, furniId)
                arriveThrough(scope, unit, furniId)
            }
        }

    override fun load(room: Room) {
        rooms.add(room)
        data.loadTargets(room.info.id)
    }

    override fun unload(scope: RoomScope) {
        val teleports =
            scope.room.furni
                .all()
                .filter { it.behaviour is DoorTeleport || it.behaviour is TileTeleport }
        data.forgetTargets(teleports.map { it.id })
        val state = rooms.remove(scope.room) ?: return
        state.rides.values.forEach { holds.release(it.unit) }
        state.passages.values.forEach { holds.release(it.unit) }
    }

    fun use(
        scope: RoomScope,
        unit: RoomUnit,
        furniId: Int,
        access: FurniAccess,
    ): Boolean {
        val furni = scope.room.furni[furniId] ?: return false
        val behaviour = furni.behaviour as? TransitUse ?: return false
        behaviour.use(scope, furni, unit, access)
        return true
    }

    fun enterOneWayGate(
        scope: RoomScope,
        unit: RoomUnit,
        furniId: Int,
    ) {
        val furni = scope.room.furni[furniId] ?: return
        (furni.behaviour as? OneWayGate)?.enter(scope, furni, unit)
    }

    fun rollDice(
        scope: RoomScope,
        unit: RoomUnit,
        furniId: Int,
    ) {
        val furni = scope.room.furni[furniId] ?: return
        (furni.behaviour as? Dice)?.roll(scope, furni, unit)
    }

    fun closeDice(
        scope: RoomScope,
        unit: RoomUnit,
        furniId: Int,
    ) {
        val furni = scope.room.furni[furniId] ?: return
        (furni.behaviour as? Dice)?.close(scope, furni, unit)
    }

    fun arriveThrough(
        scope: RoomScope,
        unit: RoomUnit,
        furniId: Int,
    ) {
        val furni = scope.room.furni[furniId] ?: return
        if (furni.behaviour !is DoorTeleport && furni.behaviour !is TileTeleport) return
        rides.arrive(scope, rooms.of(scope.room), furni, unit)
    }

    private fun resets(
        scope: RoomScope,
        state: RoomTransit,
    ) {
        val due = state.resets.filter { scope.now() + Rides.SLACK_MILLIS >= it.due }
        state.resets.removeAll(due)
        due.forEach { reset ->
            scope.room.furni[reset.furniId]?.let { StateOut.data(scope, it, reset.state) }
        }
    }
}
