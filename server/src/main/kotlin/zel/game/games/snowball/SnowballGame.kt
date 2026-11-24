@file:Suppress("MagicNumber") // percent and the lobby size

package zel.game.games.snowball

import zel.game.games.snowball.arena.ArenaDefinition
import zel.protocol.snowball.Game2LobbyData
import zel.protocol.snowball.Game2PlayerData
import java.util.concurrent.ScheduledFuture
import kotlin.math.abs

/** Changed only under the lobby's lock. */
class SnowballGame internal constructor(
    val id: Int,
    val mode: SnowballMode,
    val arena: ArenaDefinition,
) {
    enum class State(
        /** Loading and the stage take a game slot; the results window does not. */
        val holdsSlot: Boolean = false,
        val allowsChat: Boolean = false,
    ) {
        LOBBY,
        WAITING_FOR_SLOT,
        LOADING(holdsSlot = true, allowsChat = true),
        STAGE_STARTING(holdsSlot = true, allowsChat = true),
        RUNNING(holdsSlot = true, allowsChat = true),
        RESULTS,
        FINISHED,
        ;

        val isLobby: Boolean get() = this == LOBBY || this == WAITING_FOR_SLOT
    }

    /** One pending step of the game flow; starting it again replaces the earlier one. */
    internal class Timer {
        private var pending: ScheduledFuture<*>? = null

        var endsAt: Long = 0
            private set

        val isRunning: Boolean get() = pending != null

        fun start(
            task: ScheduledFuture<*>?,
            endsAt: Long = 0,
        ) {
            cancel()
            pending = task
            this.endsAt = endsAt
        }

        fun cancel() {
            pending?.cancel(false)
            pending = null
            endsAt = 0
        }
    }

    private class RateLimit(
        private val intervalMs: Long,
    ) {
        private val last = HashMap<Int, Long>()

        fun admit(
            userId: Int,
            now: Long,
        ): Boolean {
            val previous = last[userId]
            if (previous != null && now - previous < intervalMs) return false
            last[userId] = now
            return true
        }
    }

    private val byUser = LinkedHashMap<Int, SnowballGamePlayer>()
    private val fullStatusLimit = RateLimit(SnowballLobby.FULL_STATUS_INTERVAL_MS)
    private val chatLimit = RateLimit(SnowballLobby.CHAT_INTERVAL_MS)

    var state: State = State.LOBBY
        internal set

    internal val countdown = Timer()
    internal val loadTimeout = Timer()
    internal val stageTimer = Timer()

    internal var match: SnowballMatch? = null

    var runningSinceMillis: Long = 0
        private set

    var ticksDone: Int = 0
        private set

    internal val fullStatusRequests = HashSet<Int>()

    internal val rematchers = LinkedHashSet<Int>()

    val isLobby: Boolean get() = state.isLobby

    val players: List<SnowballGamePlayer> get() = byUser.values.toList()

    val playerCount: Int get() = byUser.size

    val isFull: Boolean get() = byUser.size >= MAX_PLAYERS

    fun player(userId: Int): SnowballGamePlayer? = byUser[userId]

    internal fun teamForNextPlayer(): Int =
        when {
            mode.isDeathmatch -> DEATHMATCH_TEAM
            teamSize(2) < teamSize(1) -> 2
            else -> 1
        }

    private fun teamSize(teamId: Int): Int = byUser.values.count { it.teamId == teamId }

    /** Moves the bigger team's latest joiner over while the teams are two apart. */
    internal fun balanceTeams(): List<SnowballGamePlayer> {
        if (mode.isDeathmatch) return emptyList()
        val moved = mutableListOf<SnowballGamePlayer>()
        while (abs(teamSize(1) - teamSize(2)) > 1) {
            val bigger = if (teamSize(1) > teamSize(2)) 1 else 2
            val latest = byUser.values.last { it.teamId == bigger }
            latest.teamId = 3 - bigger
            moved += latest
        }
        return moved
    }

    internal fun hasOpponents(): Boolean =
        if (mode.isDeathmatch) byUser.size >= 2 else teamSize(1) > 0 && teamSize(2) > 0

    /** The winner when the others have nobody left (the team, or the last player in deathmatch), else 0. */
    internal fun forfeitWinner(): Int {
        if (mode.isDeathmatch) return if (byUser.size == 1) byUser.keys.first() else 0
        val blue = teamSize(1) > 0
        val red = teamSize(2) > 0
        return when {
            blue == red -> 0
            blue -> 1
            else -> 2
        }
    }

    internal fun addPlayer(
        user: SnowballUser,
        skill: SnowballSkill,
        teamId: Int = teamForNextPlayer(),
    ): SnowballGamePlayer = SnowballGamePlayer(user, skill, teamId).also { byUser[user.userId] = it }

    internal fun removePlayer(userId: Int): SnowballGamePlayer? = byUser.remove(userId)

    internal fun startRunning(now: Long) {
        runningSinceMillis = now
        ticksDone = 0
    }

    internal fun tickDone() {
        ticksDone++
    }

    internal fun requestFullStatus(
        userId: Int,
        now: Long,
    ): Boolean = fullStatusLimit.admit(userId, now).also { if (it) fullStatusRequests += userId }

    internal fun allowChat(
        userId: Int,
        now: Long,
    ): Boolean = chatLimit.admit(userId, now)

    internal fun assignHumanObjectIds() {
        val first = arena.trees.size + arena.piles.size + arena.machines.size + 1
        byUser.values.forEachIndexed { index, player -> player.humanObjectId = first + index }
    }

    fun lobbyData(): Game2LobbyData =
        Game2LobbyData(
            id,
            arena.name,
            GAME_TYPE_SNOWBALL,
            arena.fieldType,
            mode.numberOfTeams,
            MAX_PLAYERS,
            byUser.values
                .firstOrNull()
                ?.user
                ?.name
                .orEmpty(),
            arena.fieldType,
            byUser.values.map(::lobbyPlayer),
            mode,
        )

    fun arenaPlayers(): List<Game2PlayerData> = byUser.values.map(::arenaPlayer)

    internal fun loadedPercent(): Int =
        if (byUser.isEmpty()) 100 else byUser.values.count { it.stageLoaded } * 100 / byUser.size

    internal fun loadedUserIds(): List<Int> = byUser.values.filter { it.stageLoaded }.map { it.userId }

    internal fun allLoaded(): Boolean = byUser.values.all { it.stageLoaded }

    companion object {
        const val GAME_TYPE_SNOWBALL = 0

        /** Lobby slots (maximumPlayers), 5 per team; deathmatch has the same 10. */
        const val MAX_PLAYERS = 10

        /** Deathmatch's one team: every player is on it; team 1 draws blue. */
        const val DEATHMATCH_TEAM = 1

        internal fun lobbyPlayer(player: SnowballGamePlayer): Game2LobbyData.Player {
            val user = player.user
            val skill = player.skill
            return Game2LobbyData.Player(
                user.userId,
                user.name,
                user.figure,
                user.gender,
                player.teamId,
                skill.skillLevel,
                skill.totalScore,
                skill.scoreToNextLevel,
            )
        }

        internal fun arenaPlayer(player: SnowballGamePlayer): Game2PlayerData {
            val user = player.user
            return Game2PlayerData(player.humanObjectId, user.name, user.figure, user.gender, player.teamId)
        }
    }
}
