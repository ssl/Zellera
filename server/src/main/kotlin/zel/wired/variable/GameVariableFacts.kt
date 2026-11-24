package zel.wired.variable

import zel.game.pets.Pet
import zel.game.players.inventory.BadgeLeaderboard
import zel.game.players.level.AccountLevelTable
import zel.net.clients.ClientConnections
import zel.room.Room
import zel.room.RoomScope
import zel.room.chat.Dances
import zel.room.entry.RoomDoor
import zel.room.furni.RoomFurni
import zel.room.furni.transit.TransitData
import zel.room.games.GameClock
import zel.room.games.GamesPart
import zel.room.games.TeamColor
import zel.room.games.freeze.FreezeGame
import zel.room.games.tag.TagGame
import zel.room.games.tileclaim.TileClaimGame
import zel.room.life.RoomLife
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.room.unit.pet.RoomPets
import zel.util.MILLIS_PER_SECOND
import zel.wired.store.WiredWriter

@Suppress("TooManyFunctions") // one per server fact the variables read or write
class GameVariableFacts(
    private val doors: (Room) -> RoomDoor,
    private val life: RoomLife,
    private val dances: Dances,
    private val games: () -> GamesPart?,
    private val pets: RoomPets,
    private val clients: ClientConnections,
    private val leaderboard: () -> BadgeLeaderboard?,
    private val space: (Room) -> VariableSpace,
    private val timezones: (Room) -> String,
    private val transit: TransitData,
    private val isFrozen: (Room, RoomUnit) -> Boolean = { _, _ -> false },
    private val writer: WiredWriter = WiredWriter.Inline,
    private val clock: () -> Long = System::currentTimeMillis,
) : VariableFacts {
    override fun frozen(
        scope: RoomScope,
        unit: RoomUnit,
    ): Boolean = isFrozen(scope.room, unit)

    override fun accountLevel(
        scope: RoomScope,
        playerId: Int,
    ): Int =
        doors(scope.room)
            .present[playerId]
            ?.card
            ?.accountLevel
            ?.takeIf { it > 0 }
            ?: AccountLevelTable.levelOf(playerId, clients.onlinePlayer(playerId), leaderboard())

    override fun achievementScore(
        scope: RoomScope,
        playerId: Int,
    ): Int =
        doors(scope.room)
            .present[playerId]
            ?.card
            ?.achievementScore ?: 0

    override fun gender(
        scope: RoomScope,
        playerId: Int,
    ): Int =
        if (doors(scope.room)
                .present[playerId]
                ?.card
                ?.gender
                .equals("F", ignoreCase = true)
        ) {
            FEMALE
        } else {
            MALE
        }

    override fun favouriteGroup(
        scope: RoomScope,
        playerId: Int,
    ): Int? =
        doors(scope.room)
            .present[playerId]
            ?.card
            ?.group
            ?.id

    override fun hasClub(playerId: Int): Boolean =
        clients
            .onlinePlayer(playerId)
            ?.stats
            ?.club
            ?.isMember == true

    override fun rightsLevel(
        scope: RoomScope,
        playerId: Int,
    ): Int = doors(scope.room).levelOf(playerId)

    override fun handItem(unit: RoomUnit): Int = life.handItem(unit)

    override fun holdItem(
        scope: RoomScope,
        unit: RoomUnit,
        item: Int,
    ) = life.holdItem(scope, unit, item)

    override fun handItemHeld(unit: RoomUnit): Boolean = life.handItemHeld(unit)

    override fun markHandItem(
        scope: RoomScope,
        unit: RoomUnit,
        held: Boolean,
    ) = life.markHandItem(scope, unit, held)

    override fun effect(unit: RoomUnit): Int = life.effect(unit)

    override fun dance(unit: RoomUnit): Int = dances.danceOf(unit)

    override fun asleep(unit: RoomUnit): Boolean = life.asleep(unit)

    override fun muted(
        scope: RoomScope,
        playerId: Int,
    ): Boolean {
        val until =
            clients
                .onlinePlayer(playerId)
                ?.stats
                ?.standing
                ?.mutedUntil ?: 0
        return until > clock() / MILLIS_PER_SECOND
    }

    override fun team(
        scope: RoomScope,
        playerId: Int,
    ): TeamFacts? {
        val room = games()?.of(scope.room) ?: return null
        val game = room.playing[playerId] ?: return null
        val member = game.memberOf(playerId) ?: return null
        val type =
            when (game) {
                is TileClaimGame -> TYPE_TILECLAIM
                is FreezeGame -> TYPE_FREEZE
                is TagGame -> TYPE_TAG
                else -> TYPE_WIRED
            }
        return TeamFacts(member.color.code, member.score, type)
    }

    override fun teamScore(
        room: Room,
        color: Int,
    ): Int {
        val team = TeamColor.fromCode(color) ?: return 0
        val rooms = games()?.of(room) ?: return 0
        return (rooms.roomGame() ?: rooms.wiredOrNew()).score(team)
    }

    override fun teamSize(
        room: Room,
        color: Int,
    ): Int {
        val team = TeamColor.fromCode(color) ?: return 0
        val rooms = games()?.of(room) ?: return 0
        return (rooms.roomGame() ?: rooms.wiredOrNew())
            .team(team)
            ?.members
            ?.size ?: 0
    }

    override fun setUserScore(
        scope: RoomScope,
        unit: RoomUnit,
        score: Int,
    ): Boolean {
        val part = games() ?: return false
        val before = (unit.kind as? UnitKind.Player)?.let { team(scope, it.playerId)?.score } ?: return false
        part.giveUserScore(scope, unit, score - before)
        return true
    }

    override fun setTeamScore(
        scope: RoomScope,
        color: Int,
        score: Int,
    ): Boolean {
        val part = games() ?: return false
        val team = TeamColor.fromCode(color) ?: return false
        part.giveTeamScore(scope, team, score - teamScore(scope.room, color))
        return true
    }

    override fun pet(
        room: Room,
        unit: RoomUnit,
    ): Pet? = pets.byIndex(room, unit.index)?.pet.takeIf { unit.kind is UnitKind.Pet }

    override fun horseRider(
        room: Room,
        unit: RoomUnit,
    ): Int? {
        val rider = pets.byIndex(room, unit.index)?.rider?.let { room.units[it] } ?: return null
        return (rider.kind as? UnitKind.Player)?.playerId
    }

    override fun clock(furni: RoomFurni): GameClock? = games()?.clockOf(furni)

    override fun wiredTimer(scope: RoomScope): Long = (clock() - space(scope.room).startedAt) / HALF_SECOND

    override fun timezone(room: Room): String = timezones(room)

    override fun teleportTarget(furni: RoomFurni): Int? = transit.shownTarget(furni.id).takeIf { it > 0 }

    /** Shown at once; stored behind the room's other wired writes, never on the room's thread. */
    override fun aimTeleport(
        roomId: Int,
        furni: RoomFurni,
        target: RoomFurni,
    ) {
        transit.showTarget(furni.id, target.id)
        writer.write(roomId, Runnable { transit.aim(furni.id, target.id) })
    }

    private companion object {
        const val TYPE_TILECLAIM = 0
        const val TYPE_FREEZE = 1
        const val TYPE_TAG = 2
        const val TYPE_WIRED = 4
        const val MALE = 0
        const val FEMALE = 1
        const val HALF_SECOND = 500L
    }
}
