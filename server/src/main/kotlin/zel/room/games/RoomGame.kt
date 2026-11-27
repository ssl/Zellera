package zel.room.games

import zel.room.RoomScope
import zel.room.event.RoomEvent
import zel.room.unit.RoomUnit
import zel.room.unit.UnitKind
import zel.util.MILLIS_PER_SECOND
import java.util.EnumMap

abstract class RoomGame(
    protected val context: GameContext,
    protected val games: RoomGames,
) {
    private val byColor = EnumMap<TeamColor, GameTeam>(TeamColor::class.java)

    val teams: Collection<GameTeam> get() = byColor.values

    var run: GameRun = GameRun.Idle
        protected set

    var round: Int = 0
        protected set

    private var runningMillis = 0L
    private var runningSince: Long? = null
    private var startedAt = 0
    private var endedAt = 0

    protected abstract val scoreName: String

    protected abstract fun member(
        unit: RoomUnit,
        playerId: Int,
        color: TeamColor,
    ): TeamMember

    fun team(color: TeamColor): GameTeam? = byColor[color]

    fun teamOrNew(color: TeamColor): GameTeam = byColor.getOrPut(color) { GameTeam(color) }

    fun memberOf(playerId: Int): TeamMember? =
        teams.firstNotNullOfOrNull { t ->
            t.members.find {
                it.playerId ==
                    playerId
            }
        }

    fun score(color: TeamColor): Int = team(color)?.totalScore ?: 0

    fun place(color: TeamColor): Int {
        val own = score(color)
        return 1 + teams.count { it.color != color && it.totalScore > own }
    }

    fun winningTeam(): GameTeam? =
        teams.fold(null as GameTeam?) { best, t ->
            if (best == null ||
                t.totalScore > best.totalScore
            ) {
                t
            } else {
                best
            }
        }

    open fun join(
        scope: RoomScope,
        unit: RoomUnit,
        color: TeamColor,
    ): TeamMember? {
        val playerId = (unit.kind as? UnitKind.Player)?.playerId ?: return null
        games.switchAway(scope, playerId)
        val member = member(unit, playerId, color)
        teamOrNew(color).join(member)
        games.playing[playerId] = this
        GameOut.playing(playingScope(scope), playerId, true)
        return member
    }

    open fun leave(
        scope: RoomScope,
        member: TeamMember,
    ) {
        team(member.color)?.leave(member)
        games.playing.remove(member.playerId)
        GameOut.playing(playingScope(scope), member.playerId, false)
    }

    /** Out of this game because the player joins another one; by default a plain [leave]. */
    open fun switchAway(
        scope: RoomScope,
        member: TeamMember,
    ) = leave(scope, member)

    protected open fun playingScope(scope: RoomScope): RoomScope = scope

    fun addScore(
        scope: RoomScope,
        member: TeamMember,
        amount: Int,
    ) {
        val team = team(member.color) ?: return
        val before = team.totalScore
        val added = member.add(amount)
        if (added !=
            0
        ) {
            scope.raise(RoomEvent.ScoreChanged(member.unit.index, member.color.code, before, before + added))
        }
    }

    protected fun startRound(scope: RoomScope) {
        round++
        run = GameRun.Running
        startedAt = context.host.nowSeconds()
        runningMillis = 0
        runningSince = scope.now()
        teams.forEach(GameTeam::resetScores)
    }

    fun pause(scope: RoomScope) {
        if (run != GameRun.Running) return
        stopRunningTime(scope)
        run = GameRun.Paused
    }

    fun resume(scope: RoomScope) {
        if (run != GameRun.Paused) return
        runningSince = scope.now()
        run = GameRun.Running
    }

    protected fun runningMillis(scope: RoomScope): Long = runningMillis + (runningSince?.let { scope.now() - it } ?: 0)

    protected fun stopRunningTime(scope: RoomScope) {
        runningSince?.let { runningMillis += scope.now() - it }
        runningSince = null
    }

    protected fun recordEnd(scope: RoomScope) {
        endedAt = context.host.nowSeconds()
        val seconds = ((runningMillis(scope) + GameClock.HALF_SECOND) / MILLIS_PER_SECOND).toInt()
        val played =
            teams
                .filter { it.members.isNotEmpty() }
                .map { team ->
                    val players = team.members.map { GameRecord.Player(it.playerId, it.score) }
                    GameRecord.Team(team.color.code, team.teamScore, players)
                }
        if (played.isNotEmpty() && runningMillis(scope) >= MIN_RESULT_MILLIS) {
            context.host.record(GameRecord(scope.room.info.id, scoreName, startedAt, endedAt, played))
        }
        val total = teams.sumOf { it.totalScore }
        val winner = if (total > 0) winningTeam() else null
        val results =
            teams
                .filter { it.members.isNotEmpty() && (it === winner || it.totalScore > 0) }
                .map { t -> TeamResult(t.color, t.members.map { it.playerId }, t.totalScore, t === winner) }
        if (results.isNotEmpty()) context.host.finished(scope, GameResult(scoreName, results, seconds))
    }

    abstract fun endRound(scope: RoomScope)

    fun clear(scope: RoomScope) {
        teams.toList().forEach { team -> team.members.toList().forEach { leave(scope, it) } }
    }

    companion object {
        private const val MIN_RESULT_MILLIS = 1000L

        fun experience(seconds: List<Int>): Int {
            val played = seconds.filter { it > 0 }
            return if (played.isEmpty()) 0 else played.sum() - (played.size - 1)
        }
    }
}
