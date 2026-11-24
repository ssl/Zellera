package zel.wired.selector

import zel.room.Room
import zel.room.RoomScope
import zel.room.chat.Dances
import zel.room.entry.RoomDoor
import zel.room.event.PlayerAction
import zel.room.life.RoomLife
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.wired.condition.RecentActions

class GameSelectorFacts(
    private val doors: (Room) -> RoomDoor,
    private val life: RoomLife,
    private val dances: Dances,
    /** Owner, admin or member of the group; a pending request does not count. */
    private val member: (groupId: Int, playerId: Int) -> Boolean,
    private val recent: RecentActions,
    /** A bot's or pet's name. */
    private val otherName: (RoomScope, RoomUnit) -> String? = { _, _ -> null },
    private val team: (Room, Int) -> Int = { _, _ -> 0 },
) : SelectorFacts {
    override fun playerName(
        scope: RoomScope,
        unit: RoomUnit,
    ): String? = (unit.kind as? UnitKind.Player)?.let { doors(scope.room).present[it.playerId]?.card?.name }

    override fun unitName(
        scope: RoomScope,
        unit: RoomUnit,
    ): String? = if (unit.kind is UnitKind.Player) playerName(scope, unit) else otherName(scope, unit)

    override fun handItem(unit: RoomUnit): Int = life.handItem(unit)

    override fun dance(unit: RoomUnit): Int = dances.danceOf(unit)

    override fun asleep(unit: RoomUnit): Boolean = life.asleep(unit)

    override fun team(
        scope: RoomScope,
        unit: RoomUnit,
    ): Int = (unit.kind as? UnitKind.Player)?.let { team(scope.room, it.playerId) } ?: 0

    override fun inGroup(
        scope: RoomScope,
        unit: RoomUnit,
        groupId: Int,
    ): Boolean {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return false
        return member(groupId, playerId)
    }

    override fun recentActions(
        scope: RoomScope,
        unit: RoomUnit,
    ): Map<Int, ActionNote> = recent.notes(scope, unit).filterKeys { it in MOMENTARY }

    private companion object {
        val MOMENTARY =
            setOf(
                PlayerAction.WAVE,
                PlayerAction.BLOW_KISS,
                PlayerAction.LAUGH,
                PlayerAction.THUMBS_UP,
                PlayerAction.SIGN,
                PlayerAction.SIXTY_SEVEN,
            )
    }
}
