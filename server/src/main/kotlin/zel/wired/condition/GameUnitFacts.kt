package zel.wired.condition

import zel.game.players.Player
import zel.room.RoomScope
import zel.room.chat.Dances
import zel.room.life.RoomLife
import zel.room.map.Point
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.bot.RoomBots
import zel.room.unit.pet.RoomPets

class GameUnitFacts(
    private val life: RoomLife,
    private val dances: Dances,
    private val bots: RoomBots,
    private val pets: RoomPets,
    private val players: (Int) -> Player?,
    private val steppingFrom: (RoomScope, RoomUnit) -> Point? = { _, _ -> null },
) : UnitFacts {
    override fun arrivedAt(
        scope: RoomScope,
        unit: RoomUnit,
    ): Point = steppingFrom(scope, unit) ?: unit.tile

    override fun effect(unit: RoomUnit): Int = life.effect(unit)

    override fun handItem(unit: RoomUnit): Int = life.handItem(unit)

    override fun asleep(unit: RoomUnit): Boolean = life.asleep(unit)

    override fun dance(unit: RoomUnit): Int = dances.danceOf(unit)

    override fun name(
        scope: RoomScope,
        unit: RoomUnit,
    ): String? =
        when (val kind = unit.kind) {
            is UnitKind.Player -> players(kind.playerId)?.profile?.name
            is UnitKind.Bot -> bots.byIndex(scope, unit.index)?.bot?.name
            is UnitKind.Pet -> pets.byIndex(scope, unit.index)?.pet?.name
        }

    override fun wornBadges(playerId: Int): List<String> =
        players(playerId)
            ?.inventory
            ?.badges
            ?.worn
            ?.map { it.code }
            .orEmpty()

    override fun inGroup(
        groupId: Int,
        playerId: Int,
    ): Boolean = players(playerId)?.stats?.guildIds?.contains(groupId) == true
}
