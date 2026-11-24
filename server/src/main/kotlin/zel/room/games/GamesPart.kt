package zel.room.games

import zel.room.Room
import zel.room.RoomListener
import zel.room.RoomPart
import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.furni.FurniBehaviour
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.games.football.Football
import zel.room.games.football.FootballGate
import zel.room.games.football.FootballGoal
import zel.room.games.football.FootballScoreboard
import zel.room.games.football.Puck
import zel.room.games.freeze.FreezeBlock
import zel.room.games.freeze.FreezeExit
import zel.room.games.freeze.FreezeTile
import zel.room.games.tag.TagField
import zel.room.games.tag.TagGame
import zel.room.games.tag.TagKind
import zel.room.games.tag.TagPole
import zel.room.games.tileclaim.TileClaimSphere
import zel.room.games.tileclaim.TileClaimTeleporter
import zel.room.games.tileclaim.TileClaimTile
import zel.room.map.Point
import zel.room.tick.TickPart
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

@Suppress("TooManyFunctions") // the room side of every game in one place
class GamesPart(
    val context: GameContext,
) : RoomPart {
    /** Shared by all rooms; a room's entry is only touched under that room's lock. */
    private val rooms = ConcurrentHashMap<Room, RoomGames>()
    private val held: MutableSet<RoomUnit> = Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))

    val clocks = Clocks(this)

    val behaviours: List<Pair<String, FurniBehaviour>> = clockBehaviours() + freeze() + tileclaim() + football() + tag()

    val listener =
        RoomListener { scope, event ->
            if (event is RoomEvent.Left) left(scope, event.unitIndex)
        }

    /** The games' part of the unload that comes before the room's furni are saved. */
    val beforeFurniSave =
        object : RoomPart {
            override fun unload(scope: RoomScope) {
                val games = of(scope.room)
                clocks.unload(scope, scope.room.furni.all())
                games.unload(scope)
            }
        }

    fun of(room: Room): RoomGames = rooms.getOrPut(room) { RoomGames(context, this) }

    /** First in the room's tick: what the games held for it goes out. */
    val tick = TickPart { scope -> rooms[scope.room]?.tick(scope) }

    override fun load(room: Room) {
        rooms[room] = RoomGames(context, this)
        room.furni
            .all()
            .filter { it.behaviour is FieldFurni }
            .forEach { it.state = FieldFurni.RESET_STATE }
    }

    override fun unload(scope: RoomScope) {
        scope.room.units.all().forEach { unit ->
            release(unit)
            waitingEffects.remove(unit)
        }
        rooms.remove(scope.room)
    }

    fun hold(unit: RoomUnit) {
        held += unit
    }

    fun release(unit: RoomUnit) {
        held -= unit
    }

    fun holds(unit: RoomUnit): Boolean = unit in held

    /** A team effect a frozen unit gets waits until its unfreeze; the freeze effect stays on view meanwhile. */
    private val waitingEffects: MutableMap<RoomUnit, Int> = Collections.synchronizedMap(IdentityHashMap())

    fun teamEffect(
        scope: RoomScope,
        unit: RoomUnit,
        effect: Int,
    ) {
        if (unit in held) waitingEffects[unit] = effect else context.hooks.giveEffect(scope, unit, effect, null)
    }

    /** The team effect that waited for this unit's unfreeze, taken out. */
    fun takeWaitingEffect(unit: RoomUnit): Int? = waitingEffects.remove(unit)

    fun stepped(
        scope: RoomScope,
        unit: RoomUnit,
        to: Point,
    ) {
        val games = of(scope.room)
        games.steps.merge(unit.index, 1, Int::plus)
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val game = games.playing[playerId] as? TagGame ?: return
        game.memberOf(playerId)?.let { game.stepped(StepScope(scope), it, to) }
    }

    fun walkEnded(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        of(scope.room).steps.remove(unit.index)
    }

    fun lookedAt(
        scope: RoomScope,
        unit: RoomUnit,
        point: Point,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val game = of(scope.room).playing[playerId] as? TagGame ?: return
        game.memberOf(playerId)?.let { game.lookedAt(scope, it, point) }
    }

    fun endRunningClocks(scope: RoomScope): Boolean = clocks.endRunning(scope, scope.room.furni.all())

    fun joinTeam(
        scope: RoomScope,
        unit: RoomUnit,
        color: TeamColor,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val games = of(scope.room)
        val wired = games.wiredOrNew()
        if (wired.memberOf(playerId)?.color != color) wired.join(scope, unit, color)
    }

    fun leaveTeam(
        scope: RoomScope,
        unit: RoomUnit,
    ) {
        if (botTeams.remove(unit) != null) return teamEffect(scope, unit, 0)
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        of(scope.room).leave(scope, playerId)
    }

    /** Bots joined to a wired team: they show the team's effect but play in no game. */
    private val botTeams: MutableMap<RoomUnit, TeamColor> = Collections.synchronizedMap(IdentityHashMap())

    fun botJoinsTeam(
        scope: RoomScope,
        unit: RoomUnit,
        color: TeamColor,
    ) {
        if (unit.kind !is UnitKind.Bot || botTeams[unit] == color) return
        botTeams[unit] = color
        teamEffect(scope, unit, WiredTeams.effectOf(color))
    }

    fun giveTeamScore(
        scope: RoomScope,
        color: TeamColor,
        points: Int,
    ) {
        val games = of(scope.room)
        val game = games.roomGame() ?: games.wiredOrNew()
        val team = game.teamOrNew(color)
        val before = team.totalScore
        team.addTeamScore(points)
        if (team.totalScore != before) {
            scope.raise(RoomEvent.ScoreChanged(NO_UNIT, color.code, before, team.totalScore))
            showScores(scope, game, color)
        }
    }

    fun giveUserScore(
        scope: RoomScope,
        unit: RoomUnit,
        points: Int,
    ) {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return
        val game = of(scope.room).playing[playerId] ?: return
        val member = game.memberOf(playerId) ?: return
        game.addScore(scope, member, points)
        showScores(scope, game, member.color)
    }

    /** A score a wired box gave shows on the team's score furni with the room's next tick. */
    private fun showScores(
        scope: RoomScope,
        game: RoomGame,
        color: TeamColor,
    ) {
        val points = game.score(color).toString()
        val boards =
            scope.room.furni
                .all()
                .filter { (it.behaviour as? TeamScoreboard)?.color == color && it.state != points }
                .onEach { StateShow.set(it, points) }
        of(scope.room).showAtTick(boards)
    }

    fun clockRun(scope: RoomScope): ClockRun = clocks.runOf(scope.room.furni.all())

    fun clockOf(furni: RoomFurni): GameClock? = (furni.behaviour as? ClockFurni)?.clock(furni)

    private fun left(
        scope: RoomScope,
        unitIndex: Int,
    ) {
        val games = of(scope.room)
        synchronized(waitingEffects) { waitingEffects.keys.removeIf { it.index == unitIndex } }
        games.steps.remove(unitIndex)
        games.gateLooks.remove(unitIndex)?.let { context.host.wear(scope, it.playerId, it.look) }
        val entry = games.playing.entries.firstOrNull { (id, game) -> game.memberOf(id)?.unit?.index == unitIndex }
        val playerId = entry?.key
        if (playerId != null) {
            val member = entry.value.memberOf(playerId)
            member?.let { release(it.unit) }
            games.leave(scope, playerId)
        }
    }

    fun saveGateOutfit(
        scope: RoomScope,
        playerId: Int,
        furniId: Int,
        gender: String,
        look: String,
    ) {
        val furni = scope.room.furni[furniId] ?: return
        val gate = furni.behaviour as? FootballGate ?: return
        if (context.hooks.hasRights(scope, playerId)) gate.save(scope, furni, gender, look)
    }

    private fun clockBehaviours(): List<Pair<String, FurniBehaviour>> =
        listOf(
            "clock.timer" to ClockFurni(clocks, ClockKind.Timer),
            "clock.counter.game" to ClockFurni(clocks, ClockKind.GameCounter),
            "clock.counter.plain" to ClockFurni(clocks, ClockKind.PlainCounter),
            "clock.hourglass" to ClockFurni(clocks, ClockKind.Hourglass),
        )

    private fun freeze(): List<Pair<String, FurniBehaviour>> =
        listOf(
            "game.freeze.tile" to FreezeTile(this),
            "game.freeze.block" to FreezeBlock(this),
            "game.freeze.exit" to FreezeExit(),
        ) + teamFurni("game.freeze", FieldFamily.Freeze)

    private fun tileclaim(): List<Pair<String, FurniBehaviour>> =
        listOf(
            "game.tileclaim.tile" to TileClaimTile(this),
            "game.tileclaim.sphere" to TileClaimSphere(),
            "game.tileclaim.teleport" to TileClaimTeleporter(this),
            "game.tileclaim.puck" to Puck(this),
        ) + teamFurni("game.tileclaim", FieldFamily.TileClaim)

    private fun teamFurni(
        prefix: String,
        family: FieldFamily,
    ): List<Pair<String, FurniBehaviour>> =
        TeamColor.entries.flatMap { color ->
            val name = color.name.lowercase()
            listOf(
                "$prefix.gate.$name" to FieldGate(this, family, color),
                "$prefix.score.$name" to TeamScoreboard(family, color),
            )
        }

    private fun football(): List<Pair<String, FurniBehaviour>> =
        listOf(
            "game.football.ball" to Football(this),
            "game.football.gate" to FootballGate(this),
        ) +
            TeamColor.entries.flatMap { color ->
                val name = color.name.lowercase()
                listOf(
                    "game.football.goal.$name" to FootballGoal(color),
                    "game.football.score.$name" to FootballScoreboard(this, color),
                )
            }

    private fun tag(): List<Pair<String, FurniBehaviour>> =
        listOf(
            "game.tag.ice.field" to TagField(this, TagKind.IceTag),
            "game.tag.ice.pole" to TagPole(TagKind.IceTag),
            "game.tag.bunny.field" to TagField(this, TagKind.Bunnyrun),
            "game.tag.bunny.pole" to TagPole(TagKind.Bunnyrun),
            "game.tag.skate.field" to TagField(this, TagKind.Rollerskate),
        )

    companion object {
        const val NO_UNIT = -1
    }
}
