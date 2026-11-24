package zel.protocol.snowball

import zel.game.games.snowball.SnowballLeaderboards
import zel.game.games.snowball.SnowballLobby
import zel.game.games.snowball.SnowballUser
import zel.game.players.Player
import zel.protocol.generated.Game2ExitGameInCodec
import zel.protocol.generated.Game2GameChatInCodec
import zel.protocol.generated.Game2GetFriendsLeaderboardInCodec
import zel.protocol.generated.Game2GetTotalGroupLeaderboardInCodec
import zel.protocol.generated.Game2GetTotalLeaderboardInCodec
import zel.protocol.generated.Game2GetWeeklyFriendsLeaderboardInCodec
import zel.protocol.generated.Game2GetWeeklyGroupLeaderboardInCodec
import zel.protocol.generated.Game2GetWeeklyLeaderboardInCodec
import zel.protocol.generated.Game2LeaveGameInCodec
import zel.protocol.generated.Game2LoadStageReadyInCodec
import zel.protocol.generated.Game2MakeSnowballInCodec
import zel.protocol.generated.Game2PlayAgainInCodec
import zel.protocol.generated.Game2QuickJoinGameInCodec
import zel.protocol.generated.Game2RequestFullStatusUpdateInCodec
import zel.protocol.generated.Game2SetPlayerMoveTargetInCodec
import zel.protocol.generated.Game2ThrowSnowballAtHumanInCodec
import zel.protocol.generated.Game2ThrowSnowballAtPositionInCodec
import zel.protocol.generated.IncomingCodec
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession

class SnowballPackets(
    private val manager: SnowballLobby,
) : PacketPackage {
    override val name = "snowball"

    override val routes: List<PacketRoute<*>> =
        listOf(
            player(Game2ExitGameInCodec) { userId, _ -> manager.exitGame(userId) },
            limited(Game2GameChatInCodec) { userId, m -> manager.gameChat(userId, m.text) },
            limited(Game2LeaveGameInCodec, spacing = LOBBY_SPACING_MS) { userId, _ ->
                manager.leaveGame(userId)
            },
            player(Game2LoadStageReadyInCodec) { userId, _ -> manager.loadStageReady(userId) },
            limited(Game2MakeSnowballInCodec) { userId, _ -> manager.makeSnowball(userId) },
            limited(Game2PlayAgainInCodec) { userId, _ -> manager.playAgain(userId) },
            limitedPlayer(Game2QuickJoinGameInCodec, spacing = LOBBY_SPACING_MS) { _, m, player ->
                manager.quickJoin(SnowballUser.of(player), quickJoinMode(m.selection?.teams, m.selection?.modeChoice))
            },
            limited(Game2RequestFullStatusUpdateInCodec) { userId, m ->
                manager.requestFullStatus(userId, m.reason)
            },
            limited(Game2SetPlayerMoveTargetInCodec) { userId, m -> manager.moveTarget(userId, m.x, m.y) },
            limited(Game2ThrowSnowballAtHumanInCodec) { userId, m ->
                manager.throwAtHuman(userId, m.targetObjectId, m.trajectory)
            },
            limited(Game2ThrowSnowballAtPositionInCodec) { userId, m ->
                manager.throwAtPosition(userId, m.x, m.y, m.trajectory)
            },
        ) + leaderboardRoutes()

    private fun leaderboardRoutes(): List<PacketRoute<*>> =
        listOf(
            board(Game2GetTotalLeaderboardInCodec) { s, m ->
                answer(s, m.gameType) { boards, user ->
                    Game2Messages.totalLeaderboard(false, boards.total(user, false, m.startRank, m.windowSize))
                }
            },
            board(Game2GetFriendsLeaderboardInCodec) { s, m ->
                answer(s, m.gameType) { boards, user ->
                    Game2Messages.totalLeaderboard(true, boards.total(user, true, m.startRank, m.windowSize))
                }
            },
            board(Game2GetWeeklyLeaderboardInCodec) { s, m ->
                answer(s, m.gameType) { boards, user ->
                    Game2Messages.weeklyLeaderboard(
                        false,
                        boards.weekly(user, false, m.weekOffset, m.startRank, m.windowSize),
                    )
                }
            },
            board(Game2GetWeeklyFriendsLeaderboardInCodec) { s, m ->
                answer(s, m.gameType) { boards, user ->
                    Game2Messages.weeklyLeaderboard(
                        true,
                        boards.weekly(user, true, m.weekOffset, m.startRank, m.windowSize),
                    )
                }
            },
            board(Game2GetTotalGroupLeaderboardInCodec) { s, m ->
                answer(s, m.gameType) { boards, user ->
                    Game2Messages.groupLeaderboard(false, boards.groups(user, false, 0, m.startRank, m.windowSize))
                }
            },
            board(Game2GetWeeklyGroupLeaderboardInCodec) { s, m ->
                answer(s, m.gameType) { boards, user ->
                    Game2Messages.groupLeaderboard(
                        true,
                        boards.groups(user, true, m.weekOffset, m.startRank, m.windowSize),
                    )
                }
            },
        )

    private fun <T> player(
        codec: IncomingCodec<T>,
        handle: (userId: Int, message: T) -> Unit,
    ): PacketRoute<T> =
        PacketRoute(codec) { session, message ->
            session.client.player?.let { handle(it.profile.id, message) }
        }

    private fun <T> limitedPlayer(
        codec: IncomingCodec<T>,
        spacing: Long = 0,
        handle: (userId: Int, message: T, player: Player) -> Unit,
    ): PacketRoute<T> {
        val cooldown = ClientCooldown("snowball.${codec.name}", spacing)
        return PacketRoute(codec) { session, message ->
            val player = session.client.player ?: return@PacketRoute
            if (spacing > 0 && !cooldown.acquire(session.client)) return@PacketRoute
            val userId = player.profile.id
            if (manager.allowPacket(userId)) handle(userId, message, player)
        }
    }

    private fun <T> limited(
        codec: IncomingCodec<T>,
        spacing: Long = 0,
        handle: (userId: Int, message: T) -> Unit,
    ): PacketRoute<T> = limitedPlayer(codec, spacing) { userId, message, _ -> handle(userId, message) }

    private fun <T> board(
        codec: IncomingCodec<T>,
        handle: (session: PacketSession, message: T) -> Unit,
    ): PacketRoute<T> = PacketRoute(codec) { session, message -> handle(session, message) }

    private fun answer(
        session: PacketSession,
        gameType: Int,
        build: (SnowballLeaderboards, Int) -> SnowballPacket<*>,
    ) {
        val userId =
            session.client.player
                ?.profile
                ?.id
        if (userId != null && gameType == SnowballLeaderboards.GAME_TYPE && manager.allowPacket(userId)) {
            manager.settleLastWeek()
            session.send(build(manager.leaderboards, userId))
        }
    }

    private companion object {
        const val LOBBY_SPACING_MS = 500L

        fun quickJoinMode(
            teams: Int?,
            mode: Int?,
        ): Int = mode ?: if ((teams ?: 2) == 1) 1 else 0
    }
}
