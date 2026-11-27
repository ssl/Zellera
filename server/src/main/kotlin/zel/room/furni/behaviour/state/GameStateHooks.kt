package zel.room.furni.behaviour.state

import zel.game.players.Gender
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.room.Room
import zel.room.RoomScope
import zel.room.furni.transit.TransitHolds
import zel.room.life.RoomLife
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.path.PathResult
import zel.room.unit.walk.Walking

class GameStateHooks(
    private val walking: Walking,
    private val life: RoomLife,
    private val rights: (RoomScope, Int) -> Boolean,
    private val gender: (Int) -> Gender?,
    private val progress: (Int, String, Int) -> Unit,
    private val save: (Room) -> Unit = {},
    private val holds: TransitHolds = TransitHolds(),
    private val unkickable: (Int) -> Boolean = { false },
    private val blocksEffect: (Int, Int) -> Boolean = { _, _ -> false },
) : StateHooks {
    override fun holdStill(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        holds.hold(unit)
        unit.path = emptyList()
    }

    override fun stopWalk(unit: RoomUnit) {
        unit.path = emptyList()
    }

    override fun letGo(unit: RoomUnit) = holds.release(unit)

    override fun sendOut(
        scope: RoomScope,
        unit: RoomUnit,
    ) = walking.sendOut(scope, unit)

    override fun unkickable(playerId: Int): Boolean = unkickable.invoke(playerId)

    override fun saveNow(room: Room) = save(room)

    override fun hasRights(
        scope: RoomScope,
        playerId: Int,
    ): Boolean = rights(scope, playerId)

    override fun male(unit: RoomUnit): Boolean =
        when (val kind = unit.kind) {
            is UnitKind.Player -> gender(kind.playerId) != Gender.F
            is UnitKind.Bot, is UnitKind.Pet -> true
        }

    override fun effect(unit: RoomUnit): Int = life.effect(unit)

    override fun giveEffect(
        scope: RoomScope,
        unit: RoomUnit,
        effect: Int,
        endsAt: Long?,
    ) = life.giveEffect(scope, unit, effect, endsAt)

    override fun blocksEffect(
        playerId: Int,
        effect: Int,
    ): Boolean = blocksEffect.invoke(playerId, effect)

    override fun holdItem(
        scope: RoomScope,
        unit: RoomUnit,
        item: Int,
    ) = life.holdItem(scope, unit, item)

    override fun handItemFromFurni(
        scope: RoomScope,
        unit: RoomUnit,
        item: Int,
    ) {
        life.holdItem(scope, unit, item)
        val player = unit.kind as? UnitKind.Player ?: return
        if (item > 0) PlayerActions.progress(player.playerId, CountedAction.FIND_HAND_ITEM, 1)
    }

    override fun walkTo(
        scope: RoomScope,
        unit: RoomUnit,
        goal: Point,
    ): Boolean = walking.walkTo(scope, unit, goal) is PathResult.Found

    override fun heightsChanged(
        scope: RoomScope,
        tiles: Collection<Point>,
    ) = walking.furniChanged(scope, tiles)

    override fun progress(
        playerId: Int,
        achievement: String,
        amount: Int,
    ) = progress.invoke(playerId, achievement, amount)
}
