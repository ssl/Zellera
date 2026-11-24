package zel.protocol.snowball

import zel.game.games.snowball.SnowballLeaderboards
import zel.game.games.snowball.SnowballMode
import zel.game.games.snowball.arena.ArenaDefinition.FuseObject
import zel.game.games.snowball.sim.GameEvent
import zel.game.games.snowball.sim.GameObjectData
import zel.game.items.ItemDataUrls
import zel.protocol.generated.Game2ArenaEnteredOut
import zel.protocol.generated.Game2ArenaEnteredOutCodec
import zel.protocol.generated.Game2EnterArenaOut
import zel.protocol.generated.Game2EnterArenaOutCodec
import zel.protocol.generated.Game2FriendsLeaderboardOut
import zel.protocol.generated.Game2FriendsLeaderboardOutCodec
import zel.protocol.generated.Game2FullGameStatusOut
import zel.protocol.generated.Game2FullGameStatusOutCodec
import zel.protocol.generated.Game2GameChatFromPlayerOut
import zel.protocol.generated.Game2GameChatFromPlayerOutCodec
import zel.protocol.generated.Game2GameCreatedOut
import zel.protocol.generated.Game2GameCreatedOutCodec
import zel.protocol.generated.Game2GameEndingOut
import zel.protocol.generated.Game2GameEndingOutCodec
import zel.protocol.generated.Game2GameLongDataOut
import zel.protocol.generated.Game2GameLongDataOutCodec
import zel.protocol.generated.Game2GameRejoinOut
import zel.protocol.generated.Game2GameRejoinOutCodec
import zel.protocol.generated.Game2GameStartedOut
import zel.protocol.generated.Game2GameStartedOutCodec
import zel.protocol.generated.Game2GameStatusOut
import zel.protocol.generated.Game2GameStatusOutCodec
import zel.protocol.generated.Game2InArenaQueueOut
import zel.protocol.generated.Game2InArenaQueueOutCodec
import zel.protocol.generated.Game2JoiningGameFailedOut
import zel.protocol.generated.Game2JoiningGameFailedOutCodec
import zel.protocol.generated.Game2PlayerExitedGameArenaOut
import zel.protocol.generated.Game2PlayerExitedGameArenaOutCodec
import zel.protocol.generated.Game2PlayerJoinedGameOut
import zel.protocol.generated.Game2PlayerJoinedGameOutCodec
import zel.protocol.generated.Game2PlayerLeftGameOut
import zel.protocol.generated.Game2PlayerLeftGameOutCodec
import zel.protocol.generated.Game2PlayerRematchesOut
import zel.protocol.generated.Game2PlayerRematchesOutCodec
import zel.protocol.generated.Game2StageEndingOut
import zel.protocol.generated.Game2StageEndingOutCodec
import zel.protocol.generated.Game2StageLoadOut
import zel.protocol.generated.Game2StageLoadOutCodec
import zel.protocol.generated.Game2StageRunningOut
import zel.protocol.generated.Game2StageRunningOutCodec
import zel.protocol.generated.Game2StageStartingOut
import zel.protocol.generated.Game2StageStartingOutCodec
import zel.protocol.generated.Game2StageStillLoadingOut
import zel.protocol.generated.Game2StageStillLoadingOutCodec
import zel.protocol.generated.Game2StartCounterOut
import zel.protocol.generated.Game2StartCounterOutCodec
import zel.protocol.generated.Game2StartingGameFailedOut
import zel.protocol.generated.Game2StartingGameFailedOutCodec
import zel.protocol.generated.Game2StopCounterOut
import zel.protocol.generated.Game2StopCounterOutCodec
import zel.protocol.generated.Game2TotalGroupLeaderboardOut
import zel.protocol.generated.Game2TotalGroupLeaderboardOutCodec
import zel.protocol.generated.Game2TotalLeaderboardOut
import zel.protocol.generated.Game2TotalLeaderboardOutCodec
import zel.protocol.generated.Game2WeeklyFriendsLeaderboardOut
import zel.protocol.generated.Game2WeeklyFriendsLeaderboardOutCodec
import zel.protocol.generated.Game2WeeklyGroupLeaderboardOut
import zel.protocol.generated.Game2WeeklyGroupLeaderboardOutCodec
import zel.protocol.generated.Game2WeeklyLeaderboardOut
import zel.protocol.generated.Game2WeeklyLeaderboardOutCodec
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.StuffData

@Suppress("TooManyFunctions") // one factory per message
object Game2Messages {
    const val JOINING_FAILED_GENERIC = 1

    const val JOINING_FAILED_HAS_ACTIVE_INSTANCE = 6

    const val JOINING_FAILED_NO_FREE_GAMES = 8

    const val STARTING_FAILED_GAME_NOT_BUILT = 1

    const val STAGE_END_NOW = 0

    private const val STAGE_ROOM_TYPE = "snowball"
    private const val STAGE_COUNTDOWN_SECONDS = 5
    private const val NO_SNOWMAN_OUTCOME = 0
    private const val STUFF_LEGACY = 0
    private const val STUFF_MAP = 1

    fun arenaEntered(player: Game2PlayerData): SnowballPacket<*> =
        SnowballPacket(
            Game2ArenaEnteredOutCodec,
            Game2ArenaEnteredOut(player.referenceId, player.name, player.figure, player.gender, player.teamId),
        )

    @Suppress("LongParameterList") // one parameter per field of the message
    fun enterArena(
        gameType: Int,
        fieldType: Int,
        mode: SnowballMode,
        players: List<Game2PlayerData>,
        width: Int,
        height: Int,
        heightmap: String,
        fuseObjects: List<FuseObject>,
        backgroundBaseUrl: String?,
        site: String?,
    ): SnowballPacket<*> {
        val base = backgroundBaseUrl.orEmpty()
        val origin = site.orEmpty()
        return SnowballPacket(
            Game2EnterArenaOutCodec,
            Game2EnterArenaOut(
                gameType,
                fieldType,
                mode.numberOfTeams,
                players.map {
                    Game2EnterArenaOut.List1Item(it.referenceId, it.name, it.figure, it.gender, it.teamId)
                },
                width,
                height,
                heightmap,
                fuseObjects.map { fuseObject(it, base, origin) },
                mode.id,
            ),
        )
    }

    private fun fuseObject(
        fuse: FuseObject,
        backgroundBaseUrl: String,
        site: String,
    ): Game2EnterArenaOut.List2Item =
        Game2EnterArenaOut.List2Item(
            fuse.name,
            fuse.id,
            fuse.x,
            fuse.y,
            fuse.xDim,
            fuse.yDim,
            fuse.height,
            fuse.dir,
            fuse.altitude,
            fuse.canStandOn,
            fuseStuff(fuse, backgroundBaseUrl, site),
        )

    private fun fuseStuff(
        fuse: FuseObject,
        backgroundBaseUrl: String,
        site: String,
    ): StuffData {
        val background = fuse.background ?: return StuffData(STUFF_LEGACY, StuffData.Value.Legacy(fuse.state))
        val image = ItemDataUrls.image(backgroundBaseUrl + background.file, site)
        return StuffData(
            STUFF_MAP,
            StuffData.Value.MapData(
                listOf(
                    "state" to "0",
                    "offsetX" to background.offsetX.toString(),
                    "offsetZ" to background.offsetZ.toString(),
                    "offsetY" to background.offsetY.toString(),
                    "imageUrl" to image,
                ),
            ),
        )
    }

    @Suppress("LongParameterList") // one parameter per field of the message
    fun fullGameStatus(
        secondsLeft: Int,
        stageSeconds: Int,
        objects: List<GameObjectData>,
        mode: SnowballMode,
        turn: Int,
        checksum: Int,
        subturns: List<List<GameEvent>>,
    ): SnowballPacket<*> {
        Game2GameData.checkObjects(objects)
        Game2GameData.checkEvents(subturns)
        val body =
            PacketWriter()
                .int(0)
                .int(secondsLeft)
                .int(stageSeconds)
                .also { Game2GameData.writeObjects(it, objects) }
                .int(0)
                .int(mode.numberOfTeams)
                .also { Game2GameData.writeStatus(it, turn, checksum, subturns) }
                .int(mode.id)
                .toByteArray()
        return SnowballPacket(Game2FullGameStatusOutCodec, Game2FullGameStatusOut(body))
    }

    fun gameStatus(
        turn: Int,
        checksum: Int,
        subturns: List<List<GameEvent>>,
    ): SnowballPacket<*> {
        Game2GameData.checkEvents(subturns)
        val body = PacketWriter().also { Game2GameData.writeStatus(it, turn, checksum, subturns) }.toByteArray()
        return SnowballPacket(Game2GameStatusOutCodec, Game2GameStatusOut(body))
    }

    fun gameChatFromPlayer(
        userId: Int,
        text: String,
    ): SnowballPacket<*> = SnowballPacket(Game2GameChatFromPlayerOutCodec, Game2GameChatFromPlayerOut(userId, text))

    private val noPairs = Game2GameCreatedOut.Trailing1(emptyList(), 0)

    fun gameCreated(lobby: Game2LobbyData): SnowballPacket<*> =
        SnowballPacket(
            Game2GameCreatedOutCodec,
            with(lobby) {
                Game2GameCreatedOut(
                    gameId,
                    levelName,
                    gameType,
                    fieldType,
                    numberOfTeams,
                    maximumPlayers,
                    owningPlayerName,
                    levelEntryId,
                    players.map {
                        Game2GameCreatedOut.List1Item(
                            it.userId,
                            it.name,
                            it.figure,
                            it.gender,
                            it.teamId,
                            it.skillLevel,
                            it.totalScore,
                            it.scoreToNextLevel,
                        )
                    },
                    noPairs,
                    mode.id,
                )
            },
        )

    fun gameLongData(lobby: Game2LobbyData): SnowballPacket<*> =
        SnowballPacket(
            Game2GameLongDataOutCodec,
            with(lobby) {
                Game2GameLongDataOut(
                    gameId,
                    levelName,
                    gameType,
                    fieldType,
                    numberOfTeams,
                    maximumPlayers,
                    owningPlayerName,
                    levelEntryId,
                    players.map {
                        Game2GameLongDataOut.List1Item(
                            it.userId,
                            it.name,
                            it.figure,
                            it.gender,
                            it.teamId,
                            it.skillLevel,
                            it.totalScore,
                            it.scoreToNextLevel,
                        )
                    },
                    Game2GameLongDataOut.Trailing1(emptyList(), 0),
                    mode.id,
                )
            },
        )

    fun gameStarted(lobby: Game2LobbyData): SnowballPacket<*> =
        SnowballPacket(
            Game2GameStartedOutCodec,
            with(lobby) {
                Game2GameStartedOut(
                    gameId,
                    levelName,
                    gameType,
                    fieldType,
                    numberOfTeams,
                    maximumPlayers,
                    owningPlayerName,
                    levelEntryId,
                    players.map {
                        Game2GameStartedOut.List1Item(
                            it.userId,
                            it.name,
                            it.figure,
                            it.gender,
                            it.teamId,
                            it.skillLevel,
                            it.totalScore,
                            it.scoreToNextLevel,
                        )
                    },
                    Game2GameStartedOut.Trailing1(emptyList(), 0),
                    mode.id,
                )
            },
        )

    fun gameEnding(ending: Game2GameEnding): SnowballPacket<*> =
        SnowballPacket(
            Game2GameEndingOutCodec,
            with(ending) {
                Game2GameEndingOut(
                    timeToNextState,
                    mode.isDeathmatch,
                    resultType,
                    winner,
                    teams.map { team ->
                        Game2GameEndingOut.List1Item(
                            team.teamId,
                            team.score,
                            team.players.map { endingRow(it) },
                        )
                    },
                    mostKillsUserId,
                    mostHitsUserId,
                    mode.id,
                    NO_SNOWMAN_OUTCOME,
                )
            },
        )

    // The ten stats are a fixed field list.
    @Suppress("MagicNumber")
    private fun endingRow(row: Game2GameEnding.Player): Game2GameEndingOut.List1Item.List1Item {
        val s = row.stats
        return Game2GameEndingOut.List1Item.List1Item(
            row.name,
            row.userId,
            row.figure,
            row.gender,
            row.score,
            s[0],
            s[1],
            s[2],
            s[3],
            s[4],
            s[5],
            s[6],
            s[7],
            s[8],
            s[9],
        )
    }

    fun gameRejoin(roomBeforeGame: Int): SnowballPacket<*> =
        SnowballPacket(Game2GameRejoinOutCodec, Game2GameRejoinOut(roomBeforeGame))

    fun inArenaQueue(position: Int): SnowballPacket<*> =
        SnowballPacket(Game2InArenaQueueOutCodec, Game2InArenaQueueOut(position))

    fun joiningGameFailed(reason: Int): SnowballPacket<*> =
        SnowballPacket(Game2JoiningGameFailedOutCodec, Game2JoiningGameFailedOut(reason))

    fun playerExitedGameArena(
        userId: Int,
        humanObjectId: Int,
    ): SnowballPacket<*> =
        SnowballPacket(
            Game2PlayerExitedGameArenaOutCodec,
            Game2PlayerExitedGameArenaOut(userId, humanObjectId),
        )

    fun playerRematches(userId: Int): SnowballPacket<*> =
        SnowballPacket(Game2PlayerRematchesOutCodec, Game2PlayerRematchesOut(userId))

    fun stageEnding(timeToNextState: Int): SnowballPacket<*> =
        SnowballPacket(Game2StageEndingOutCodec, Game2StageEndingOut(timeToNextState))

    fun stageLoad(gameType: Int): SnowballPacket<*> =
        SnowballPacket(Game2StageLoadOutCodec, Game2StageLoadOut(gameType))

    fun stageRunning(secondsToStageEnd: Int): SnowballPacket<*> =
        SnowballPacket(Game2StageRunningOutCodec, Game2StageRunningOut(secondsToStageEnd))

    /** The room type and countdown are ignored by the client. */
    fun stageStarting(
        gameType: Int,
        objects: List<GameObjectData>,
    ): SnowballPacket<*> {
        Game2GameData.checkObjects(objects)
        val body =
            PacketWriter()
                .int(gameType)
                .string(STAGE_ROOM_TYPE)
                .int(STAGE_COUNTDOWN_SECONDS)
                .also { Game2GameData.writeObjects(it, objects) }
                .toByteArray()
        return SnowballPacket(Game2StageStartingOutCodec, Game2StageStartingOut(body))
    }

    fun stageStillLoading(
        percentage: Int,
        finishedUserIds: List<Int>,
    ): SnowballPacket<*> =
        SnowballPacket(
            Game2StageStillLoadingOutCodec,
            Game2StageStillLoadingOut(percentage, finishedUserIds.toList()),
        )

    fun startCounter(seconds: Int): SnowballPacket<*> =
        SnowballPacket(Game2StartCounterOutCodec, Game2StartCounterOut(seconds))

    fun startingGameFailed(reason: Int): SnowballPacket<*> =
        SnowballPacket(Game2StartingGameFailedOutCodec, Game2StartingGameFailedOut(reason))

    fun stopCounter(): SnowballPacket<*> = SnowballPacket(Game2StopCounterOutCodec, Game2StopCounterOut)

    fun userJoinedGame(
        player: Game2LobbyData.Player,
        wasTeamSwitched: Boolean,
    ): SnowballPacket<*> =
        SnowballPacket(
            Game2PlayerJoinedGameOutCodec,
            Game2PlayerJoinedGameOut(
                player.userId,
                player.name,
                player.figure,
                player.gender,
                player.teamId,
                player.skillLevel,
                player.totalScore,
                player.scoreToNextLevel,
                wasTeamSwitched,
            ),
        )

    fun userLeftGame(userId: Int): SnowballPacket<*> =
        SnowballPacket(Game2PlayerLeftGameOutCodec, Game2PlayerLeftGameOut(userId))

    fun totalLeaderboard(
        friends: Boolean,
        page: SnowballLeaderboards.Page,
    ): SnowballPacket<*> =
        if (friends) {
            SnowballPacket(
                Game2FriendsLeaderboardOutCodec,
                Game2FriendsLeaderboardOut(
                    page.entries.map {
                        Game2FriendsLeaderboardOut.List1Item(
                            it.userId,
                            it.score,
                            it.rank,
                            it.name,
                            it.figure,
                            it.gender,
                        )
                    },
                    page.totalListSize,
                    page.gameTypeId,
                ),
            )
        } else {
            SnowballPacket(
                Game2TotalLeaderboardOutCodec,
                Game2TotalLeaderboardOut(
                    page.entries.map {
                        Game2TotalLeaderboardOut.List1Item(
                            it.userId,
                            it.score,
                            it.rank,
                            it.name,
                            it.figure,
                            it.gender,
                        )
                    },
                    page.totalListSize,
                    page.gameTypeId,
                ),
            )
        }

    fun weeklyLeaderboard(
        friends: Boolean,
        page: SnowballLeaderboards.Page,
    ): SnowballPacket<*> =
        if (friends) {
            SnowballPacket(
                Game2WeeklyFriendsLeaderboardOutCodec,
                Game2WeeklyFriendsLeaderboardOut(
                    page.year,
                    page.week,
                    page.maxOffset,
                    page.currentOffset,
                    page.minutesUntilReset,
                    page.entries.map {
                        Game2WeeklyFriendsLeaderboardOut.List1Item(
                            it.userId,
                            it.score,
                            it.rank,
                            it.name,
                            it.figure,
                            it.gender,
                        )
                    },
                    page.totalListSize,
                    page.gameTypeId,
                ),
            )
        } else {
            SnowballPacket(
                Game2WeeklyLeaderboardOutCodec,
                Game2WeeklyLeaderboardOut(
                    page.year,
                    page.week,
                    page.maxOffset,
                    page.currentOffset,
                    page.minutesUntilReset,
                    page.entries.map {
                        Game2WeeklyLeaderboardOut.List1Item(
                            it.userId,
                            it.score,
                            it.rank,
                            it.name,
                            it.figure,
                            it.gender,
                        )
                    },
                    page.totalListSize,
                    page.gameTypeId,
                ),
            )
        }

    fun groupLeaderboard(
        weekly: Boolean,
        groupPage: SnowballLeaderboards.GroupPage,
    ): SnowballPacket<*> {
        val page = groupPage.page
        return if (weekly) {
            SnowballPacket(
                Game2WeeklyGroupLeaderboardOutCodec,
                Game2WeeklyGroupLeaderboardOut(
                    page.year,
                    page.week,
                    page.maxOffset,
                    page.currentOffset,
                    page.minutesUntilReset,
                    page.entries.map {
                        Game2WeeklyGroupLeaderboardOut.List1Item(
                            it.userId,
                            it.score,
                            it.rank,
                            it.name,
                            it.figure,
                            it.gender,
                        )
                    },
                    page.totalListSize,
                    page.gameTypeId,
                    groupPage.favouriteGroupId,
                ),
            )
        } else {
            SnowballPacket(
                Game2TotalGroupLeaderboardOutCodec,
                Game2TotalGroupLeaderboardOut(
                    page.entries.map {
                        Game2TotalGroupLeaderboardOut.List1Item(
                            it.userId,
                            it.score,
                            it.rank,
                            it.name,
                            it.figure,
                            it.gender,
                        )
                    },
                    page.totalListSize,
                    page.gameTypeId,
                    groupPage.favouriteGroupId,
                ),
            )
        }
    }
}
