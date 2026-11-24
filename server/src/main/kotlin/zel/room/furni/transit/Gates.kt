package zel.room.furni.transit

import zel.protocol.generated.CustomPlayerNotificationOut
import zel.protocol.generated.CustomPlayerNotificationOutCodec
import zel.protocol.generated.ObjectsDataUpdateOut
import zel.protocol.generated.ObjectsDataUpdateOutCodec
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.RoomFurni
import zel.room.furni.place.FurniAccess
import zel.room.furni.place.FurniWire
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

/** Under the room lock. */
class Gates(
    private val hooks: TransitHooks,
) {
    fun use(
        scope: RoomScope,
        furni: RoomFurni,
        unit: RoomUnit,
        access: FurniAccess,
    ) {
        if (!(access.isRoomOwner || access.hasRights) || occupied(scope, furni)) return
        val closed = (furni.state.trim().toIntOrNull() ?: 0) == 0
        StateOut.data(scope, furni, if (closed) Rides.OPEN else Rides.CLOSED, keep = true)
        scope.raise(RoomEvent.StateChanged(furni.id, unit.index))
    }

    fun lets(
        furni: RoomFurni,
        unit: RoomUnit,
        rule: GateRule,
    ): Boolean {
        if (unit.kind !is UnitKind.Player) return false
        return when (rule) {
            GateRule.Club -> hooks.clubMember(unit)
            GateRule.Group -> furni.groupId > 0 && hooks.groupMember(unit, furni.groupId)
            GateRule.Effect -> wearsCostume(furni, hooks.effect(unit))
        }
    }

    fun refused(
        scope: RoomScope,
        unit: RoomUnit,
        rule: GateRule,
    ) {
        val player = unit.kind as? UnitKind.Player ?: return
        if (rule != GateRule.Club) return
        scope.out.send(
            Lane.Now,
            Audience.Player(player.playerId),
            CustomPlayerNotificationOutCodec,
            CustomPlayerNotificationOut(CLUB_GATE_NOTICE),
        )
    }

    fun stepOn(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        rule: GateRule,
    ) {
        state.gateCloses.remove(furni.id)
        if (furni.state == Rides.OPEN) return
        state.gateOpened[furni.id] = scope.now()
        if (rule.plural) {
            StateOut.plural(scope, furni, Rides.OPEN)
        } else {
            StateOut.data(scope, furni, Rides.OPEN)
        }
    }

    fun stepOff(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        unit: RoomUnit,
        rule: GateRule,
    ) = vacate(scope, state, furni, rule, leaving = unit)

    fun tick(
        scope: RoomScope,
        state: RoomTransit,
    ) {
        state.gateCloses.entries.toList().forEach { (id, close) ->
            if (scope.now() + Rides.SLACK_MILLIS < close.due) return@forEach
            state.gateCloses.remove(id)
            val furni = scope.room.furni[id] ?: return@forEach
            if (!occupied(scope, furni)) close(scope, state, furni, close.rule)
        }
        abandoned(scope, state)
    }

    private fun abandoned(
        scope: RoomScope,
        state: RoomTransit,
    ) {
        state.gateOpened.keys.toList().forEach { id ->
            if (id in state.gateCloses) return@forEach
            val furni = scope.room.furni[id]
            val rule = (furni?.behaviour as? MemberGate)?.rule
            if (furni == null || rule == null) {
                state.gateOpened.remove(id)
            } else if (!occupied(scope, furni)) {
                vacate(scope, state, furni, rule, leaving = null)
            }
        }
    }

    private fun vacate(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        rule: GateRule,
        leaving: RoomUnit?,
    ) {
        rule.closeAfterMillis?.let { delay ->
            state.gateCloses[furni.id] = GateClose(scope.now() + delay, rule)
            return
        }
        if (occupied(scope, furni, except = leaving)) return
        val opened = state.gateOpened[furni.id] ?: scope.now()
        val due = maxOf(scope.now(), opened + GateRule.OPEN_MILLIS)
        if (scope.now() + Rides.SLACK_MILLIS >= due) {
            close(scope, state, furni, rule)
        } else {
            state.gateCloses[furni.id] = GateClose(due, rule)
        }
    }

    /** An entrant's room view ends with the room's club gates in one plural update, after its first unit update. */
    fun entered(
        scope: RoomScope,
        playerId: Int,
    ) {
        val items =
            scope.room.furni
                .all()
                .filter { (it.behaviour as? MemberGate)?.rule == GateRule.Club }
                .map { ObjectsDataUpdateOut.List1Item(it.id, FurniWire.stuff(it)) }
        if (items.isEmpty()) return
        scope.out.send(
            Lane.NextTickAfterUnitUpdate,
            Audience.Player(playerId),
            ObjectsDataUpdateOutCodec,
            ObjectsDataUpdateOut(items),
        )
    }

    private fun close(
        scope: RoomScope,
        state: RoomTransit,
        furni: RoomFurni,
        rule: GateRule,
    ) {
        state.gateOpened.remove(furni.id)
        if (furni.state == Rides.CLOSED) return
        if (rule.plural) {
            StateOut.plural(scope, furni, Rides.CLOSED)
        } else {
            StateOut.data(scope, furni, Rides.CLOSED)
        }
    }

    private fun occupied(
        scope: RoomScope,
        furni: RoomFurni,
        except: RoomUnit? = null,
    ): Boolean {
        val tiles = furni.tiles().toSet()
        return scope.room.units
            .all()
            .any { it !== except && it.tile in tiles }
    }

    /** The effects the gate's definition names; a gate that names none takes the standard costumes. */
    private fun wearsCostume(
        furni: RoomFurni,
        effect: Int,
    ): Boolean {
        val named =
            furni.definition.params
                .split(',', ';')
                .mapNotNull { it.trim().toIntOrNull() }
        return effect in named.ifEmpty { STANDARD_COSTUMES }
    }

    private companion object {
        const val CLUB_GATE_NOTICE = 3

        val STANDARD_COSTUMES = listOf(114, 115, 116, 117, 118, 135)
    }
}
