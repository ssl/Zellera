package zel.wired.selector

import zel.room.event.PlayerAction
import zel.room.event.RoomEvent
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import zel.wired.box.BoxSettings
import zel.wired.condition.RecentActions
import zel.wired.firing.Firing

class UserByAction(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: SelectorFacts,
) : UnitSelector() {
    private val action = settings.intAt(0).takeIf(::isAction) ?: PlayerAction.WAVE
    private val narrowing = settings.text.trim()

    override fun matches(firing: Firing): Collection<Int> =
        firing.scope.room.units
            .all()
            .filter { doing(firing, it) }
            .map { it.index }

    private fun doing(
        firing: Firing,
        unit: RoomUnit,
    ): Boolean =
        when (action) {
            PlayerAction.AWAKE -> !facts.asleep(unit)
            PlayerAction.IDLE -> facts.asleep(unit)
            PlayerAction.SIT -> unit.showsStatus(UnitStatus.SIT)
            PlayerAction.STAND -> !unit.showsStatus(UnitStatus.SIT) && !unit.showsStatus(UnitStatus.LAY)
            PlayerAction.LAY -> unit.showsStatus(UnitStatus.LAY)
            PlayerAction.SWIM -> unit.showsStatus(UnitStatus.SWIM)
            PlayerAction.DANCE -> facts.dance(unit).let { it > 0 && narrows(it.toString()) }
            PlayerAction.SIGN -> unit.statuses[UnitStatus.SIGN]?.let(::narrows) == true || happening(firing, unit)
            else -> momentary(firing, unit)
        }

    private fun momentary(
        firing: Firing,
        unit: RoomUnit,
    ): Boolean {
        val event = firing.event
        if (event is RoomEvent.Action && event.unitIndex == unit.index && event.action == action) {
            return narrows(event.param)
        }
        val note = facts.recentActions(firing.scope, unit)[action] ?: return false
        return firing.scope.now() - note.at <= RecentActions.WINDOW_MILLIS && narrows(note.param)
    }

    private fun happening(
        firing: Firing,
        unit: RoomUnit,
    ): Boolean {
        val event = firing.event
        return event is RoomEvent.Action &&
            event.unitIndex == unit.index &&
            event.action == action &&
            narrows(event.param)
    }

    private fun narrows(param: String): Boolean = narrowing.isEmpty() || action !in NARROWED || param == narrowing

    companion object {
        fun isAction(value: Int): Boolean = value in ACTIONS

        private val NARROWED = setOf(PlayerAction.SIGN, PlayerAction.DANCE)
        private val ACTIONS = (PlayerAction.WAVE..PlayerAction.DANCE).toSet() + PlayerAction.SIXTY_SEVEN
    }
}
