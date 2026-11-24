package zel.room.games.tileclaim

import zel.game.achievements.GameEvents
import zel.game.effects.EffectRoles
import zel.game.quests.CountedAction
import zel.room.RoomScope
import zel.room.furni.RoomFurni
import zel.room.furni.behaviour.state.StateShow
import zel.room.games.FieldFamily
import zel.room.games.FieldGame
import zel.room.games.GameContext
import zel.room.games.GameOut
import zel.room.games.GameRun
import zel.room.games.GamesPart
import zel.room.games.RoomGames
import zel.room.games.TeamColor
import zel.room.games.TeamMember
import zel.room.games.TeamScoreboard
import zel.room.games.TileClaimRules
import zel.room.map.Point
import zel.room.unit.RoomUnit
import java.util.EnumMap

class TileClaimGame(
    context: GameContext,
    games: RoomGames,
    private val part: GamesPart,
) : FieldGame(context, games, FieldFamily.TileClaim) {
    private val rules: TileClaimRules get() = context.rules.tileclaim
    private val patches = HashMap<Point, RoomFurni>()
    private val locked = EnumMap<TeamColor, MutableSet<Point>>(TeamColor::class.java)

    override val scoreName = "TileClaimGame"

    override fun member(
        unit: RoomUnit,
        playerId: Int,
        color: TeamColor,
    ): TeamMember = TeamMember(unit, playerId, color)

    override fun effectOf(
        scope: RoomScope,
        member: TeamMember,
    ): Int =
        gateEffect(scope, member.color) ?: EffectRoles.of(EffectRoles.TILE_CLAIM_TEAM + member.color.name.lowercase())

    override fun joined(
        scope: RoomScope,
        member: TeamMember,
    ) {
        member.joinedAt(runningMillis(scope))
        context.host.counted(member.playerId, CountedAction.GAME_JOIN_GAME, 1)
    }

    fun begin(scope: RoomScope) {
        if (run != GameRun.Idle) return
        startRound(scope)
        locked.clear()
        patches.clear()
        val all = scope.room.furni.all()
        val field = all.filter { it.behaviour is TileClaimTile }
        field.forEach { furni -> furni.anchor?.let { patches[it] = furni } }
        // Only the patches that change go out: a start while a round runs resends no "1". They go out with the
        // room's next tick, the patches before the score furni, in one ObjectsDataUpdate.
        val reset = field.filter { it.state != FIRST_TOUCH }.onEach { it.state = FIRST_TOUCH }
        val boards =
            all.filter {
                (it.behaviour as? TeamScoreboard)?.family == FieldFamily.TileClaim && it.state != "0"
            }
        boards.forEach { it.state = "0" }
        games.showAtTick(reset + boards)
        spheres(scope).forEach { StateShow.data(scope, it, FIRST_TOUCH) }
        watch(scope, round, FIRST_LOOK_MS)
        members().forEach { it.joinedAt(0) }
    }

    private fun spheres(scope: RoomScope) =
        scope.room.furni
            .all()
            .filter { it.behaviour is TileClaimSphere }

    /** Once a second the round is looked at: who leads, and whether every patch is locked. */
    private fun watch(
        scope: RoomScope,
        ofRound: Int,
        delay: Long,
    ) {
        context.jobs.after(scope.room, delay) { later ->
            if (run == GameRun.Idle || round != ofRound) return@after
            if (run == GameRun.Running) {
                showLeader(later)
                endWhenAllLocked(later)
            }
            if (run != GameRun.Idle) watch(later, ofRound, LOOK_MS)
        }
    }

    /** The sphere shows the team with the most points; of level teams the first in team order. */
    private fun showLeader(scope: RoomScope) {
        val ahead = teams.maxByOrNull { it.totalScore } ?: return
        val state = (ahead.color.code + LEADER_STATE_OFFSET).toString()
        spheres(scope).filter { it.state != state }.forEach { StateShow.data(scope, it, state) }
    }

    /** A step takes a patch one stage further for the stepper's team, or takes it from the team that held it. */
    fun mark(
        scope: RoomScope,
        member: TeamMember,
        tile: RoomFurni,
    ) {
        val at = tile.anchor ?: return
        if (run != GameRun.Running || patches[at] !== tile) return
        val color = member.color
        val first = color.code * STAGES_PER_TEAM
        val state = tile.state.toIntOrNull() ?: 0
        if (state >= STAGES_PER_TEAM && state % STAGES_PER_TEAM == LOCKED_STAGE) return
        val stage = state - first
        val changed = mutableListOf(tile)
        when (stage) {
            LOCKED_STAGE - 1 -> {
                tile.state = (first + LOCKED_STAGE).toString()
                addScore(scope, member, rules.lockPoints)
                changed += lock(scope, member, at)
            }

            0 -> {
                tile.state = (first + 1).toString()
                addScore(scope, member, rules.fillPoints)
            }

            else -> {
                tile.state = first.toString()
                addScore(scope, member, rules.hijackPoints)
            }
        }
        GameOut.states(scope, changed + changedScoreboards(scope, color))
    }

    /** Returns the patches the lock closed in. */
    private fun lock(
        scope: RoomScope,
        member: TeamMember,
        at: Point,
    ): List<RoomFurni> {
        val color = member.color
        val own = locked.getOrPut(color) { HashSet() }
        own += at
        val state = (LOCKED_STAGE + color.code * STAGES_PER_TEAM).toString()
        val filled = closedIn(at, color, own).mapNotNull { patches[it] }.onEach { it.state = state }
        filled.forEach { patch -> patch.anchor?.let(own::add) }
        if (filled.isNotEmpty()) addScore(scope, member, rules.lockPoints * filled.size)
        context.host.counted(member.playerId, CountedAction.TILECLAIM_LOCK_TILE, 1 + filled.size)
        context.hooks.progress(member.playerId, GameEvents.GAME_TILECLAIM_TILE_LOCKED, 1 + filled.size)
        if (filled.isNotEmpty()) context.host.reached(member.playerId, CountedAction.TILECLAIM_LOCK_AREA, filled.size)
        return filled
    }

    /** The largest pocket beside [at] that [own] locked patches shut off from the field's edge. */
    private fun closedIn(
        at: Point,
        color: TeamColor,
        own: Set<Point>,
    ): Set<Point> = SIDES.map { pocket(at.step(it), color, own) }.maxByOrNull { it.size }.orEmpty()

    /** The patches reachable from [start] inside [walls]; none when the way out is open or another team holds one. */
    private fun pocket(
        start: Point,
        color: TeamColor,
        walls: Set<Point>,
    ): Set<Point> {
        val reached = LinkedHashSet<Point>()
        val open = ArrayDeque(listOf(start))
        while (open.isNotEmpty()) {
            val tile = open.removeFirst()
            if (tile in walls || tile in reached) continue
            if (tile !in patches || locked.any { (other, tiles) -> other != color && tile in tiles }) return emptySet()
            reached += tile
            SIDES.forEach { open.addLast(tile.step(it)) }
        }
        return reached
    }

    private fun endWhenAllLocked(scope: RoomScope) {
        if (patches.isEmpty() || locked.values.sumOf { it.size } < patches.size) return
        if (!part.endRunningClocks(scope)) endRound(scope)
    }

    override fun endRound(scope: RoomScope) {
        recordEnd(scope)
        stopRunningTime(scope)
        val millis = runningMillis(scope)
        run = GameRun.Idle
        val winner = winningTeam()
        val singleTeam = teams.count { it.members.isNotEmpty() } == 1
        if (winner != null && !singleTeam) winner.members.forEach { GameOut.wave(scope, it.unit.index) }
        if (winner != null) {
            val state = (WINNER_STATE_OFFSET + winner.color.code).toString()
            spheres(scope).forEach { StateShow.data(scope, it, state) }
        }
        scheduleEndField(winner?.color)
        if (!singleTeam) endAchievements(scope, winner?.color, millis)
    }

    /**
     * The untouched patches go back to "0" with the room's next tick; a start before that tick keeps them (no patch
     * update for a start on a paused or running game).
     */
    private fun scheduleEndField(winner: TeamColor?) {
        val ofRound = round
        val tiles = patches.values.toList()
        games.atNextTick { later ->
            if (run == GameRun.Idle && round == ofRound) {
                GameOut.states(later, tiles.filter { it.state == FIRST_TOUCH }.onEach { it.state = "0" })
                val won = winner?.let { color -> locked[color]?.mapNotNull { patches[it] } }.orEmpty()
                if (won.isNotEmpty()) flicker(later, won, ofRound, FLICKERS)
            }
        }
    }

    private fun flicker(
        scope: RoomScope,
        tiles: List<RoomFurni>,
        ofRound: Int,
        left: Int,
    ) {
        context.jobs.after(scope.room, FLICKER_MS) { later ->
            if (run == GameRun.Idle && round == ofRound) {
                val state = if (left % 2 == 0) "0" else lockedStateOf(tiles.first())
                GameOut.states(later, tiles.onEach { it.state = state })
                if (left > 1) flicker(later, tiles, ofRound, left - 1)
            }
        }
    }

    private fun lockedStateOf(tile: RoomFurni): String {
        val holder = tile.anchor?.let { at -> locked.entries.find { at in it.value }?.key } ?: return "0"
        return (LOCKED_STAGE + holder.code * STAGES_PER_TEAM).toString()
    }

    private fun endAchievements(
        scope: RoomScope,
        winner: TeamColor?,
        millis: Long,
    ) {
        val hooks = context.hooks
        val players = members()
        val seconds = players.map { it.secondsPlayed(millis) }
        val total = experience(seconds)
        players.forEachIndexed { k, member ->
            if (member.color == winner && total > 0) {
                hooks.progress(member.playerId, GameEvents.GAME_PLAYER_XP, total)
                hooks.progress(member.playerId, GameEvents.GAME_TILECLAIM_WIN, total)
                context.host.counted(member.playerId, CountedAction.TILECLAIM_WIN_GAME, 1)
            }
            if (seconds[k] > 0) {
                hooks.progress(member.playerId, GameEvents.GAME_TILECLAIM_PLAY_SECONDS, seconds[k])
                context.host.counted(member.playerId, CountedAction.TILECLAIM_PLAY_GAME, 1)
            }
        }
        if (total > 0) hooks.progress(scope.room.info.ownerId, GameEvents.GAME_AUTHOR_XP, total)
    }

    private fun members(): List<TeamMember> = teams.flatMap { it.members }

    private companion object {
        const val FIRST_TOUCH = "1"
        const val STAGES_PER_TEAM = 3
        const val LOCKED_STAGE = 2
        const val LEADER_STATE_OFFSET = 2
        const val WINNER_STATE_OFFSET = 6
        const val FIRST_LOOK_MS = 1500L
        const val LOOK_MS = 1000L
        const val FLICKER_MS = 500L
        const val FLICKERS = 10
        val SIDES = listOf(0, 4, 6, 2)
    }
}
