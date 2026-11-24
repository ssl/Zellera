package zel.wired.condition

import zel.room.event.PlayerAction
import zel.room.event.RoomEvent
import zel.room.unit.RoomUnit
import zel.room.unit.UnitStatus
import zel.wired.box.BoxSettings
import zel.wired.box.ConditionBox
import zel.wired.firing.Firing
import zel.wired.firing.Targets

class UserPerformsAction(
    override val furniId: Int,
    override val settings: BoxSettings,
    private val facts: UnitFacts,
    private val recent: RecentActions,
    private val negative: Boolean,
) : ConditionBox {
    override fun passes(firing: Firing): Boolean {
        val users = Targets.units(firing, this)
        if (users.isEmpty()) return Quantified.noUsers(this, negative)
        val action = settings.intAt(0)
        return Quantified.holds(users, settings) { performs(firing, it, action) } != negative
    }

    private fun performs(
        firing: Firing,
        unit: RoomUnit,
        action: Int,
    ): Boolean {
        val event = firing.event
        return when {
            event is RoomEvent.Action && event.unitIndex == unit.index && event.action == action -> {
                filtered(event.param)
            }

            inState(unit, action) -> {
                true
            }

            else -> {
                action in MOMENTARY && recent.did(firing.scope, unit, action) && signFits(firing, unit, action)
            }
        }
    }

    private fun inState(
        unit: RoomUnit,
        action: Int,
    ): Boolean =
        when (action) {
            PlayerAction.SIT -> unit.showsStatus(UnitStatus.SIT)
            PlayerAction.LAY -> unit.showsStatus(UnitStatus.LAY)
            PlayerAction.IDLE -> facts.asleep(unit)
            PlayerAction.SWIM -> unit.showsStatus(UnitStatus.SWIM)
            PlayerAction.SIGN -> unit.statuses[UnitStatus.SIGN]?.let(::filtered) == true
            PlayerAction.DANCE -> facts.dance(unit).let { it != 0 && filtered(it.toString()) }
            else -> false
        }

    private fun filtered(param: String): Boolean = settings.text.isEmpty() || settings.text == param

    private fun signFits(
        firing: Firing,
        unit: RoomUnit,
        action: Int,
    ): Boolean = action != PlayerAction.SIGN || recent.notes(firing.scope, unit)[action]?.param?.let(::filtered) == true

    companion object {
        val ACTIONS: Set<Int> =
            setOf(
                PlayerAction.WAVE,
                PlayerAction.BLOW_KISS,
                PlayerAction.LAUGH,
                PlayerAction.THUMBS_UP,
                PlayerAction.AWAKE,
                PlayerAction.IDLE,
                PlayerAction.SIT,
                PlayerAction.STAND,
                PlayerAction.LAY,
                PlayerAction.SWIM,
                PlayerAction.SIGN,
                PlayerAction.DANCE,
                PlayerAction.SIXTY_SEVEN,
            )

        private val MOMENTARY =
            setOf(
                PlayerAction.WAVE,
                PlayerAction.BLOW_KISS,
                PlayerAction.LAUGH,
                PlayerAction.THUMBS_UP,
                PlayerAction.AWAKE,
                PlayerAction.STAND,
                PlayerAction.SIGN,
                PlayerAction.SIXTY_SEVEN,
            )
    }
}
