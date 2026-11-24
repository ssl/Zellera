package zel.protocol.gamecenter

import zel.game.games.snowball.SnowballLeaderboards
import zel.game.games.snowball.SnowballLobby
import zel.game.games.snowball.SnowballTokens
import zel.protocol.generated.Game2CheckGameDirectoryStatusInCodec
import zel.protocol.generated.Game2GetAccountGameStatusInCodec
import zel.protocol.generated.GetSnowballGameTokensOfferInCodec
import zel.protocol.generated.PurchaseSnowballGameTokensOfferInCodec
import zel.protocol.snowball.send
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute

class GameCenterPackets(
    manager: SnowballLobby,
    tokens: SnowballTokens,
) : PacketPackage {
    override val name = "gamecenter"

    override val routes: List<PacketRoute<*>> =
        listOf(
            PacketRoute(Game2CheckGameDirectoryStatusInCodec) { session, _ ->
                session.client.player?.let { manager.checkDirectoryStatus(it.profile.id) }
            },
            PacketRoute(Game2GetAccountGameStatusInCodec) { session, m ->
                session.client.player?.let { manager.accountGameStatus(it.profile.id, m.gameTypeId) }
            },
            PacketRoute(GetSnowballGameTokensOfferInCodec) { session, _ ->
                session.send(GameCenterMessages.snowballGameTokens(tokens.offers()))
            },
            // the hub's games-left line follows the account status
            PacketRoute(PurchaseSnowballGameTokensOfferInCodec) { session, m ->
                session.client.player?.let {
                    if (tokens.buy(it, m.int1)) manager.accountGameStatus(it.profile.id, SnowballLeaderboards.GAME_TYPE)
                }
            },
        )
}
