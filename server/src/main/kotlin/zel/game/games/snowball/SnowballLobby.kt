@file:Suppress("MagicNumber") // millisecond and second conversions

package zel.game.games.snowball

import zel.core.rules.FreeGames
import zel.core.rules.rules
import zel.game.games.snowball.SnowballGame.State
import zel.game.games.snowball.arena.ArenaRepository
import zel.game.games.snowball.sim.SyncArena
import zel.game.messenger.FriendNotification
import zel.protocol.gamecenter.GameCenterMessages
import zel.protocol.snowball.Game2Messages
import zel.protocol.snowball.SnowballPacket
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/** Snowball lobbies through rematch; all state changes under this object's lock and timers re-enter through it. */
@Suppress("LargeClass") // the whole game flow shares one lock
class SnowballLobby internal constructor(
    private val runtime: SnowballRuntime,
    @Volatile private var arenas: ArenaRepository?,
    val leaderboards: SnowballLeaderboards,
) {
    /** Requests of one user in a fixed window; guarded by itself. */
    private class FloodWindow(
        private var start: Long,
    ) {
        private var count = 0L

        @Synchronized
        fun admit(now: Long): Boolean {
            if (now - start > PACKET_FLOOD_WINDOW_MS) {
                start = now
                count = 0
            }
            return ++count <= PACKET_FLOOD_MAX_PER_WINDOW
        }
    }

    private val byId = LinkedHashMap<Int, SnowballGame>()
    private val byUser = HashMap<Int, SnowballGame>()
    private val leaveBlocks = HashMap<Int, Long>()
    private val slotQueue = ArrayDeque<SnowballGame>()
    private var nextGameId = 1
    private val floodWindows = ConcurrentHashMap<Int, FloodWindow>()

    constructor(runtime: SnowballRuntime, leaderboards: SnowballLeaderboards) : this(runtime, null, leaderboards)

    val games: List<SnowballGame>
        @Synchronized get() = byId.values.toList()

    @Synchronized
    fun gameOf(userId: Int): SnowballGame? = byUser[userId]

    fun load() {
        val folder = runtime.settings().arenasFolder
        val repository = ArenaRepository.load(folder.takeIf { it.isNotBlank() }?.let(Path::of))
        arenas = repository
        if (repository.all().isEmpty()) {
            LOGGER.warn("Snowball: the content pack brings no arenas ({}), so no game can start.", folder)
        } else {
            LOGGER.info("Snowball: {} arenas loaded.", repository.all().size)
        }
        runtime.schedule({ settleLastWeek() }, WEEK_SETTLE_DELAY_MILLIS)
    }

    fun settleLastWeek() {
        runtime.settleLastWeek()
    }

    fun allowPacket(userId: Int): Boolean {
        val now = runtime.nowMillis()
        return floodWindows.computeIfAbsent(userId) { FloodWindow(now) }.admit(now)
    }

    fun checkDirectoryStatus(userId: Int) {
        settleLastWeek()
        val status =
            if (runtime.settings().enabled) GameCenterMessages.STATUS_OK else GameCenterMessages.STATUS_UNAVAILABLE
        val blockLeft = synchronized(this) { leaveBlockSecondsLeft(userId) }
        send(
            userId,
            GameCenterMessages.gameDirectoryStatus(
                status,
                blockLeft,
                runtime.gamesPlayed(userId),
                freeGamesLeft(userId),
            ),
        )
    }

    private fun freeGamesLeft(userId: Int): Int =
        when (rules.snowballFreeGames) {
            FreeGames.UNLIMITED -> GameCenterMessages.UNLIMITED_FREE_GAMES
            FreeGames.NONE -> 0
            FreeGames.DAILY -> runtime.allowance(userId).gamesLeft(rules)
        }

    fun accountGameStatus(
        userId: Int,
        gameTypeId: Int,
    ) {
        settleLastWeek()
        send(
            userId,
            GameCenterMessages.accountGameStatus(gameTypeId, freeGamesLeft(userId), runtime.gamesPlayed(userId)),
        )
    }

    /** The allowance and skill are read before the lobby lock, so their database reads never hold up a match. */
    fun quickJoin(
        user: SnowballUser,
        modeId: Int,
    ) {
        val gamesLeft = freeGamesLeft(user.userId)
        val skill = runtime.skill(user.userId)
        synchronized(this) { join(user, modeId, gamesLeft, skill) }
    }

    private fun join(
        user: SnowballUser,
        modeId: Int,
        gamesLeft: Int,
        skill: SnowballSkill,
    ) {
        val userId = user.userId
        val settings = runtime.settings()

        val blockLeft = leaveBlockSecondsLeft(userId)
        if (blockLeft > 0) {
            send(userId, GameCenterMessages.userBlocked(blockLeft))
            return
        }
        if (userId in byUser) {
            send(userId, Game2Messages.joiningGameFailed(Game2Messages.JOINING_FAILED_HAS_ACTIVE_INSTANCE))
            return
        }
        val mode = SnowballMode.of(modeId)
        if (!settings.enabled || mode == null || rules.snowballFreeGames == FreeGames.NONE) {
            send(userId, Game2Messages.joiningGameFailed(Game2Messages.JOINING_FAILED_GENERIC))
            return
        }
        if (gamesLeft == 0) {
            send(userId, Game2Messages.joiningGameFailed(Game2Messages.JOINING_FAILED_NO_FREE_GAMES))
            return
        }

        val open = openLobby(mode)
        val game = open ?: newGame(mode, settings)
        if (game == null) {
            send(userId, Game2Messages.startingGameFailed(Game2Messages.STARTING_FAILED_GAME_NOT_BUILT))
            return
        }
        val player = game.addPlayer(user, skill)
        byUser[userId] = game

        if (open == null) {
            send(userId, Game2Messages.gameCreated(game.lobbyData()))
        } else {
            send(userId, Game2Messages.gameLongData(game.lobbyData()))
            broadcast(game, Game2Messages.userJoinedGame(SnowballGame.lobbyPlayer(player), false), except = userId)
        }

        if (game.state == State.WAITING_FOR_SLOT) {
            send(userId, Game2Messages.inArenaQueue(queuePosition(game)))
        } else if (game.countdown.isRunning) {
            // The countdown is not restarted; the joiner gets what is left of it.
            val left = game.countdown.endsAt - runtime.nowMillis()
            send(userId, Game2Messages.startCounter(maxOf(1L, (left + 999) / 1000).toInt()))
        }

        if (game.state == State.LOBBY) {
            if (game.isFull) {
                startOrQueue(game, settings)
            } else if (!game.countdown.isRunning && game.playerCount >= settings.minPlayers) {
                startCountdown(game, settings)
            }
        }
    }

    private fun openLobby(mode: SnowballMode): SnowballGame? =
        byId.values.filter { it.mode == mode && it.isLobby && !it.isFull }.maxByOrNull { it.playerCount }

    private fun newGame(
        mode: SnowballMode,
        settings: SnowballSettings,
    ): SnowballGame? {
        val repository = arenas
        val arena = repository?.pick(settings.arenas, runtime.random())
        if (arena == null) {
            LOGGER.warn(
                "Snowball: no arena to open a lobby on (loaded: {}, snowball.arenas: {}).",
                repository != null,
                settings.arenas,
            )
            return null
        }
        return SnowballGame(nextGameId++, mode, arena).also { byId[it.id] = it }
    }

    private fun startCountdown(
        game: SnowballGame,
        settings: SnowballSettings,
    ) {
        val seconds = settings.lobbyCountdownSeconds
        broadcast(game, Game2Messages.startCounter(seconds))
        val delay = seconds * 1000L
        game.countdown.start(runtime.schedule({ countdownEnded(game) }, delay), runtime.nowMillis() + delay)
    }

    @Synchronized
    internal fun countdownEnded(game: SnowballGame) {
        if (!game.countdown.isRunning || game.state != State.LOBBY) return
        game.countdown.cancel()
        startOrQueue(game, runtime.settings())
    }

    private fun startOrQueue(
        game: SnowballGame,
        settings: SnowballSettings,
    ) {
        game.countdown.cancel()
        if (gamesInPlay() < settings.maxConcurrentGames) {
            startGame(game, settings)
            return
        }
        game.state = State.WAITING_FOR_SLOT
        slotQueue += game
        sendQueuePositions()
    }

    private fun gamesInPlay(): Int = byId.values.count { it.state.holdsSlot }

    private fun queuePosition(game: SnowballGame): Int =
        slotQueue.indexOf(game).let { if (it < 0) slotQueue.size else it } + 1

    private fun sendQueuePositions() {
        slotQueue.forEachIndexed { index, queued -> broadcast(queued, Game2Messages.inArenaQueue(index + 1)) }
    }

    @Synchronized
    fun leaveGame(userId: Int) {
        val game = byUser[userId]
        if (game != null && game.isLobby) leaveLobby(game, userId)
    }

    private fun leaveLobby(
        game: SnowballGame,
        userId: Int,
    ) {
        game.removePlayer(userId) ?: return
        byUser.remove(userId)
        broadcast(game, Game2Messages.userLeftGame(userId))

        if (game.playerCount == 0) {
            finishGame(game)
            return
        }
        // a leave can leave one team two players short (both on one team: the start would be cancelled)
        for (moved in game.balanceTeams()) {
            broadcast(game, Game2Messages.userJoinedGame(SnowballGame.lobbyPlayer(moved), true))
        }
        if (game.playerCount >= runtime.settings().minPlayers) return
        if (game.countdown.isRunning) {
            game.countdown.cancel()
            broadcast(game, Game2Messages.stopCounter())
        } else if (game.state == State.WAITING_FOR_SLOT) {
            slotQueue.remove(game)
            game.state = State.LOBBY
            broadcast(game, Game2Messages.stopCounter())
            sendQueuePositions()
        }
    }

    private fun startGame(
        game: SnowballGame,
        settings: SnowballSettings,
    ) {
        game.state = State.LOADING
        game.assignHumanObjectIds()
        broadcast(game, Game2Messages.gameStarted(game.lobbyData()))

        val arena = game.arena
        val enterArena =
            Game2Messages.enterArena(
                SnowballGame.GAME_TYPE_SNOWBALL,
                arena.fieldType,
                game.mode,
                game.arenaPlayers(),
                arena.width,
                arena.height,
                arena.heightmapString(),
                arena.fuse,
                settings.assetsUrl,
                runtime.siteUrl(),
            )
        for (player in game.players) {
            runtime.notifyFriends(player.userId, FriendNotification.IS_PLAYING_GAME)
            runtime.send(player.userId, enterArena)
            val room = runtime.leaveRoomForGame(player.userId)
            // a rematcher comes from the results window, in no room: he keeps the room of his first game
            if (room >= 0) player.roomBeforeGame = room
        }
        for (player in game.players) {
            broadcast(game, Game2Messages.arenaEntered(SnowballGame.arenaPlayer(player)))
        }
        broadcast(game, Game2Messages.stageLoad(SnowballGame.GAME_TYPE_SNOWBALL))
        game.loadTimeout.start(runtime.schedule({ loadTimedOut(game) }, settings.loadTimeoutSeconds * 1000L))
        LOGGER.info(
            "Snowball {} game {} ({}) started with {} players.",
            game.mode,
            game.id,
            arena.name,
            game.playerCount,
        )
    }

    @Synchronized
    fun loadStageReady(userId: Int) {
        val game = byUser[userId]?.takeIf { it.state == State.LOADING } ?: return
        val player = game.player(userId)?.takeIf { !it.stageLoaded } ?: return
        player.stageLoaded = true
        broadcast(game, Game2Messages.stageStillLoading(game.loadedPercent(), game.loadedUserIds()))
        if (game.allLoaded()) {
            game.loadTimeout.cancel()
            beginStage(game)
        }
    }

    @Synchronized
    internal fun loadTimedOut(game: SnowballGame) {
        if (game.state != State.LOADING) return
        game.loadTimeout.cancel()
        val late = game.players.filter { !it.stageLoaded }
        LOGGER.warn(
            "Snowball game {}: {} of {} players did not load the stage within {} s and are dropped: {}.",
            game.id,
            late.size,
            game.playerCount,
            runtime.settings().loadTimeoutSeconds,
            late.joinToString { describeLate(it.userId) },
        )
        for (player in late) {
            game.removePlayer(player.userId)
            byUser.remove(player.userId)
            runtime.notifyFriends(player.userId, FriendNotification.FINISHED_GAME)
            send(player.userId, GameCenterMessages.gameCancelled())
        }
        afterLoadingLeave(game)
    }

    private fun describeLate(userId: Int): String =
        if (runtime.isInBackground(userId)) "user $userId (window in background)" else "user $userId"

    private fun afterLoadingLeave(game: SnowballGame) {
        when {
            game.playerCount == 0 -> {
                closeEmptyGame(game, "while the stage was loading")
            }

            game.playerCount < runtime.settings().minPlayers || !game.hasOpponents() -> {
                cancelShortGame(game)
            }

            game.allLoaded() -> {
                game.loadTimeout.cancel()
                beginStage(game)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught") // any failure to build the stage cancels the game
    private fun beginStage(game: SnowballGame) {
        if (!game.hasOpponents()) {
            cancelShortGame(game)
            return
        }
        if (rules.snowballFreeGames == FreeGames.DAILY) game.players.forEach { runtime.recordPlay(it.userId) }
        val players =
            game.players.map {
                val user = it.user
                SnowballMatch.Player(
                    it.userId,
                    it.humanObjectId,
                    it.teamId,
                    user.name,
                    user.motto,
                    user.figure,
                    user.gender,
                )
            }
        val match =
            try {
                SnowballMatch(game.arena, game.mode, players, runtime.settings().stageSeconds)
            } catch (exception: RuntimeException) {
                LOGGER.error(
                    "Snowball game {}: the stage could not be built on {}.",
                    game.id,
                    game.arena.name,
                    exception,
                )
                cancelGame(game)
                return
            }
        game.match = match
        game.state = State.STAGE_STARTING
        broadcast(game, Game2Messages.stageStarting(SnowballGame.GAME_TYPE_SNOWBALL, match.startObjects))
        game.stageTimer.start(runtime.schedule({ stageRunning(game) }, STAGE_COUNTDOWN_MILLIS))
    }

    @Synchronized
    internal fun stageRunning(game: SnowballGame) {
        if (game.state != State.STAGE_STARTING) return
        val match = checkNotNull(game.match) { "a running game has a stage" }
        game.state = State.RUNNING
        game.startRunning(runtime.nowMillis())
        broadcast(game, Game2Messages.stageRunning(match.stageSeconds))
        scheduleTick(game)
    }

    private fun scheduleTick(game: SnowballGame) {
        val due = game.runningSinceMillis + (game.ticksDone + 1).toLong() * SyncArena.TURN_MILLIS
        val delay = maxOf(0L, due - runtime.nowMillis())
        game.stageTimer.start(runtime.schedule({ tick(game) }, delay))
    }

    @Synchronized
    internal fun tick(game: SnowballGame) {
        if (game.state != State.RUNNING) return
        val due = (runtime.nowMillis() - game.runningSinceMillis) / SyncArena.TURN_MILLIS
        var ran = 0
        try {
            while (game.state == State.RUNNING && game.ticksDone < due && ran < MAX_CATCH_UP_TURNS) {
                runTurn(game)
                ran++
            }
        } catch (
            @Suppress("TooGenericExceptionCaught") failure: RuntimeException, // see the comment below
        ) {
            // a turn that cannot run would leave the game running without a timer, holding its slot and its players
            LOGGER.error("Snowball game {}: turn {} failed; the game is cancelled.", game.id, game.ticksDone, failure)
            cancelGame(game)
            return
        }
        if (game.state == State.RUNNING) scheduleTick(game)
    }

    private fun runTurn(game: SnowballGame) {
        val match = checkNotNull(game.match) { "a running game has a stage" }
        val tick = match.tick()
        game.tickDone()
        if (tick.checksum !in Int.MIN_VALUE..Int.MAX_VALUE) {
            // the receiver sums the checksum as an int without wrapping: a wider value would desync every turn
            LOGGER.error(
                "Snowball game {}: checksum {} of turn {} leaves the int range.",
                game.id,
                tick.checksum,
                tick.turn,
            )
        }
        val checksum = tick.checksum.toInt()
        val status = compose { Game2Messages.gameStatus(tick.turn, checksum, tick.subturns()) }
        val full =
            if (game.fullStatusRequests.isNotEmpty() && match.snapshotRebuildsExactly()) {
                compose {
                    Game2Messages.fullGameStatus(
                        match.secondsLeft,
                        match.stageSeconds,
                        match.snapshot(),
                        game.mode,
                        tick.turn,
                        checksum,
                        tick.subturns(),
                    )
                }
            } else {
                null
            }
        for (player in game.players) {
            val message = if (full != null && player.userId in game.fullStatusRequests) full else status
            if (message != null) runtime.send(player.userId, message)
        }
        if (full != null) game.fullStatusRequests.clear()

        when {
            game.playerCount == 0 -> closeEmptyGame(game, "in the stage")
            match.isTimeUp -> endStage(game, 0)
            else -> game.forfeitWinner().takeIf { it > 0 }?.let { endStage(game, it) }
        }
    }

    private fun endStage(
        game: SnowballGame,
        forfeitWinner: Int,
    ) {
        val match = checkNotNull(game.match) { "an ending game has a stage" }
        game.stageTimer.cancel()
        game.state = State.RESULTS
        val settings = runtime.settings()
        val results = SnowballResults.of(match, forfeitWinner)

        broadcast(game, Game2Messages.stageEnding(Game2Messages.STAGE_END_NOW))
        broadcast(game, Game2Messages.gameEnding(results.ending(settings.rematchSeconds)))

        val finishers = game.players.map { it.userId }
        for (userId in finishers) runtime.notifyFriends(userId, FriendNotification.FINISHED_GAME)
        runtime.recordScores(finishers.associateWith { match.score(it) })
        runtime.progressAchievements(results.achievements(match, finishers))
        LOGGER.info(
            "Snowball {} game {} ({}) ended at turn {}: result {}, winner {}.",
            game.mode,
            game.id,
            game.arena.name,
            match.turn,
            results.resultType,
            results.winner,
        )

        game.stageTimer.start(runtime.schedule({ rematchWindowEnded(game) }, settings.rematchSeconds * 1000L))
        startQueuedGames()
    }

    private fun runningMatch(userId: Int): SnowballMatch? = byUser[userId]?.takeIf { it.state == State.RUNNING }?.match

    @Synchronized
    fun moveTarget(
        userId: Int,
        x: Int,
        y: Int,
    ) {
        runningMatch(userId)?.move(userId, x, y)
    }

    @Synchronized
    fun throwAtHuman(
        userId: Int,
        targetObjectId: Int,
        trajectory: Int,
    ) {
        runningMatch(userId)?.throwAtHuman(userId, targetObjectId, trajectory)
    }

    @Synchronized
    fun throwAtPosition(
        userId: Int,
        x: Int,
        y: Int,
        trajectory: Int,
    ) {
        runningMatch(userId)?.throwAtPosition(userId, x, y, trajectory)
    }

    @Synchronized
    fun makeSnowball(userId: Int) {
        runningMatch(userId)?.makeSnowball(userId)
    }

    /** Reason 0 behind, 1 checksum, -1 forced; answered with a FullGameStatus at most once a second. */
    @Synchronized
    fun requestFullStatus(
        userId: Int,
        reason: Int,
    ) {
        val game = byUser[userId]?.takeIf { it.state == State.RUNNING } ?: return
        if (game.requestFullStatus(userId, runtime.nowMillis())) {
            LOGGER.debug("Snowball game {}: user {} asked for a full status (reason {}).", game.id, userId, reason)
        }
    }

    /** Sent to every player including the sender, under room chat's mute, word filter, length and flood rules. */
    @Synchronized
    fun gameChat(
        userId: Int,
        text: String?,
    ) {
        val game = byUser[userId]?.takeIf { it.state.allowsChat } ?: return
        if (text.isNullOrBlank() || !game.allowChat(userId, runtime.nowMillis())) return
        val filtered = runtime.filterChat(userId, text.take(CHAT_MAX_LENGTH))
        if (filtered.isNullOrBlank()) return
        broadcast(game, Game2Messages.gameChatFromPlayer(userId, filtered))
    }

    /** Needs a game left like QuickJoin, read before the lobby lock. */
    fun playAgain(userId: Int) {
        val gamesLeft = freeGamesLeft(userId)
        synchronized(this) { rematch(userId, gamesLeft) }
    }

    private fun rematch(
        userId: Int,
        gamesLeft: Int,
    ) {
        val game = byUser[userId]?.takeIf { it.state == State.RESULTS && it.player(userId) != null } ?: return
        if (gamesLeft == 0) {
            send(userId, Game2Messages.joiningGameFailed(Game2Messages.JOINING_FAILED_NO_FREE_GAMES))
            return
        }
        if (game.rematchers.add(userId)) broadcast(game, Game2Messages.playerRematches(userId))
    }

    @Synchronized
    internal fun rematchWindowEnded(game: SnowballGame) {
        if (game.state != State.RESULTS) return
        val rematchers = game.rematchers.mapNotNull { game.player(it) }
        for (player in game.players) {
            send(player.userId, Game2Messages.gameRejoin(player.roomBeforeGame))
        }
        finishGame(game)
        if (rematchers.isNotEmpty()) openRematchLobby(game.mode, rematchers)
    }

    private fun openRematchLobby(
        mode: SnowballMode,
        rematchers: List<SnowballGamePlayer>,
    ) {
        val settings = runtime.settings()
        val lobby = if (settings.enabled) newGame(mode, settings) else null
        if (lobby == null) {
            for (player in rematchers) {
                send(player.userId, Game2Messages.startingGameFailed(Game2Messages.STARTING_FAILED_GAME_NOT_BUILT))
            }
            return
        }
        for (player in rematchers) {
            lobby.addPlayer(player.user, runtime.skill(player.userId), player.teamId).roomBeforeGame =
                player.roomBeforeGame
            byUser[player.userId] = lobby
        }
        lobby.balanceTeams() // team game, rematchers of one team only: the lobby data below already has them split
        lobby.players.forEachIndexed { index, player ->
            val data = lobby.lobbyData()
            send(player.userId, if (index == 0) Game2Messages.gameCreated(data) else Game2Messages.gameLongData(data))
        }
        if (lobby.isFull) {
            startOrQueue(lobby, settings)
        } else if (lobby.playerCount >= settings.minPlayers) {
            startCountdown(lobby, settings)
        }
    }

    /** A plain leave in a lobby, one with the leave block while loading or playing, one without from the results. */
    @Synchronized
    fun exitGame(userId: Int) {
        leave(userId, exit = true)
    }

    @Synchronized
    fun userLoggedOut(userId: Int) {
        floodWindows.remove(userId)
        leave(userId, exit = false)
    }

    private fun leave(
        userId: Int,
        exit: Boolean,
    ) {
        val game = byUser[userId] ?: return
        when (game.state) {
            State.LOBBY, State.WAITING_FOR_SLOT -> {
                leaveLobby(game, userId)
            }

            State.LOADING -> {
                LOGGER.info(
                    "Snowball game {}: user {} {} while the stage was loading.",
                    game.id,
                    userId,
                    if (exit) "left" else "disconnected",
                )
                game.removePlayer(userId)
                byUser.remove(userId)
                runtime.notifyFriends(userId, FriendNotification.FINISHED_GAME)
                if (exit) blockAfterLeave(userId)
                afterLoadingLeave(game)
            }

            State.STAGE_STARTING, State.RUNNING -> {
                leaveStage(game, userId, exit)
            }

            State.RESULTS -> {
                leaveResults(game, userId)
            }

            State.FINISHED -> {
                byUser.remove(userId)
            }
        }
    }

    private fun blockAfterLeave(userId: Int) {
        val blockSeconds = runtime.settings().leaveBlockSeconds
        if (blockSeconds > 0) {
            val now = runtime.nowMillis()
            // a player who never asks again would keep his ended block for good
            leaveBlocks.values.removeIf { it <= now }
            leaveBlocks[userId] = now + blockSeconds * 1000L
            send(userId, GameCenterMessages.userBlocked(blockSeconds))
        }
    }

    /** Leaving the stage keeps the player's results row and team points, gives no score row and moves him nowhere. */
    private fun leaveStage(
        game: SnowballGame,
        userId: Int,
        block: Boolean,
    ) {
        val player = game.removePlayer(userId) ?: return
        byUser.remove(userId)
        runtime.notifyFriends(userId, FriendNotification.FINISHED_GAME)
        game.fullStatusRequests.remove(userId)
        game.match?.leave(userId)
        broadcast(game, Game2Messages.playerExitedGameArena(userId, player.humanObjectId))
        if (block) blockAfterLeave(userId)
        if (game.playerCount == 0) {
            closeEmptyGame(game, "in the stage")
        } else if (game.state == State.STAGE_STARTING) {
            game.forfeitWinner().takeIf { it > 0 }?.let { endStage(game, it) }
        }
    }

    private fun leaveResults(
        game: SnowballGame,
        userId: Int,
    ) {
        game.removePlayer(userId)
        game.rematchers.remove(userId)
        byUser.remove(userId)
        if (game.playerCount == 0) closeEmptyGame(game, "from the results")
    }

    private fun leaveBlockSecondsLeft(userId: Int): Int {
        val until = leaveBlocks[userId] ?: return 0
        val left = until - runtime.nowMillis()
        if (left <= 0) {
            leaveBlocks.remove(userId)
            return 0
        }
        return ((left + 999) / 1000).toInt()
    }

    private fun closeEmptyGame(
        game: SnowballGame,
        where: String,
    ) {
        LOGGER.info("Snowball game {} closed: the last player left {}.", game.id, where)
        finishGame(game)
    }

    private fun cancelShortGame(game: SnowballGame) {
        LOGGER.warn(
            "Snowball game {} cancelled: {} players left, fewer than the {} it needs or no opponent.",
            game.id,
            game.playerCount,
            runtime.settings().minPlayers,
        )
        cancelGame(game)
    }

    private fun cancelGame(game: SnowballGame) {
        broadcast(game, GameCenterMessages.gameCancelled())
        for (player in game.players) {
            runtime.notifyFriends(player.userId, FriendNotification.FINISHED_GAME)
        }
        finishGame(game)
    }

    private fun finishGame(game: SnowballGame) {
        val heldSlot = game.state.holdsSlot
        game.state = State.FINISHED
        game.countdown.cancel()
        game.loadTimeout.cancel()
        game.stageTimer.cancel()
        byId.remove(game.id)
        slotQueue.remove(game)
        for (player in game.players) {
            if (byUser[player.userId] === game) byUser.remove(player.userId)
        }
        if (heldSlot) startQueuedGames()
    }

    private fun startQueuedGames() {
        val settings = runtime.settings()
        var started = false
        while (slotQueue.isNotEmpty() && gamesInPlay() < settings.maxConcurrentGames) {
            val next = slotQueue.removeFirst()
            if (next.state == State.WAITING_FOR_SLOT) {
                startGame(next, settings)
                started = true
            }
        }
        if (started) sendQueuePositions()
    }

    private fun send(
        userId: Int,
        packet: SnowballPacket<*>,
    ) {
        runtime.send(userId, packet)
    }

    private fun broadcast(
        game: SnowballGame,
        packet: SnowballPacket<*>,
        except: Int? = null,
    ) {
        for (player in game.players) {
            if (player.userId != except) runtime.send(player.userId, packet)
        }
    }

    private fun compose(packet: () -> SnowballPacket<*>): SnowballPacket<*>? =
        try {
            packet()
        } catch (exception: IllegalArgumentException) {
            LOGGER.error("Snowball: a message the client could not read was dropped.", exception)
            null
        }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SnowballLobby::class.java)

        internal const val STAGE_COUNTDOWN_MILLIS = 5_500L

        internal const val MAX_CATCH_UP_TURNS = 10

        internal const val FULL_STATUS_INTERVAL_MS = 1000L

        internal const val CHAT_INTERVAL_MS = 750L
        internal const val CHAT_MAX_LENGTH = 100

        internal const val WEEK_SETTLE_DELAY_MILLIS = 60_000L

        /** The friend bar shows it as the text gamecenter.<name>.name. */
        const val FRIEND_BAR_GAME = "snowball"

        internal const val PACKET_FLOOD_WINDOW_MS = 1000L
        internal const val PACKET_FLOOD_MAX_PER_WINDOW = 40
    }
}
