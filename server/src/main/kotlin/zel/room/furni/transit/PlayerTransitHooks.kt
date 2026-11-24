package zel.room.furni.transit

import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.room.RoomScope
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind

class PlayerTransitHooks(
    private val players: (Int) -> Player?,
    private val effects: (RoomUnit) -> Int = { 0 },
    private val travel: (RoomScope, RoomUnit, Int, Int) -> Boolean = { _, _, _, _ -> false },
) : TransitHooks {
    override fun clubMember(unit: RoomUnit): Boolean = playerOf(unit)?.stats?.club?.isMember == true

    override fun groupMember(
        unit: RoomUnit,
        groupId: Int,
    ): Boolean {
        val player = playerOf(unit) ?: return false
        return groupId in player.stats.guildIds || player.hasPermission(Privilege.GROUP_PASS_ANY_GATE)
    }

    override fun effect(unit: RoomUnit): Int = effects(unit)

    override fun teleported(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        val player = unit.kind as? UnitKind.Player ?: return
        PlayerActions.progress(player.playerId, CountedAction.TELEPORT, 1)
    }

    override fun travel(
        scope: RoomScope,
        unit: RoomUnit,
        roomId: Int,
        furniId: Int,
    ): Boolean = travel.invoke(scope, unit, roomId, furniId)

    private fun playerOf(unit: RoomUnit): Player? = (unit.kind as? UnitKind.Player)?.let { players(it.playerId) }
}
