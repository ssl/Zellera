package zel.protocol.gamecenter

import zel.core.rules.GameTokenOffer
import zel.game.economy.EconomyLedger
import zel.protocol.generated.Game2AccountGameStatusOut
import zel.protocol.generated.Game2AccountGameStatusOutCodec
import zel.protocol.generated.Game2GameCancelledOut
import zel.protocol.generated.Game2GameCancelledOutCodec
import zel.protocol.generated.Game2GameDirectoryStatusOut
import zel.protocol.generated.Game2GameDirectoryStatusOutCodec
import zel.protocol.generated.Game2PlayerBlockedOut
import zel.protocol.generated.Game2PlayerBlockedOutCodec
import zel.protocol.generated.SnowballGameTokensOut
import zel.protocol.generated.SnowballGameTokensOutCodec
import zel.protocol.snowball.SnowballPacket

object GameCenterMessages {
    const val STATUS_OK = 0

    const val STATUS_UNAVAILABLE = 2

    const val UNLIMITED_FREE_GAMES = -1

    fun accountGameStatus(
        gameTypeId: Int,
        freeGamesLeft: Int,
        gamesPlayed: Int,
    ): SnowballPacket<*> =
        SnowballPacket(
            Game2AccountGameStatusOutCodec,
            Game2AccountGameStatusOut(gameTypeId, freeGamesLeft, gamesPlayed),
        )

    fun gameCancelled(): SnowballPacket<*> = SnowballPacket(Game2GameCancelledOutCodec, Game2GameCancelledOut)

    fun gameDirectoryStatus(
        status: Int,
        blockLengthSeconds: Int,
        gamesPlayed: Int,
        freeGamesLeft: Int,
    ): SnowballPacket<*> =
        SnowballPacket(
            Game2GameDirectoryStatusOutCodec,
            Game2GameDirectoryStatusOut(
                status,
                blockLengthSeconds,
                gamesPlayed,
                freeGamesLeft,
                status == STATUS_OK,
                false,
            ),
        )

    fun userBlocked(secondsLeft: Int): SnowballPacket<*> =
        SnowballPacket(Game2PlayerBlockedOutCodec, Game2PlayerBlockedOut(secondsLeft))

    /** Offer ids count from 1. */
    fun snowballGameTokens(offers: List<GameTokenOffer>): SnowballPacket<*> =
        SnowballPacket(
            SnowballGameTokensOutCodec,
            SnowballGameTokensOut(
                offers.take(TOKEN_OFFER_NAMES.size).mapIndexed { index, offer ->
                    SnowballGameTokensOut.List1Item(
                        index + 1,
                        TOKEN_OFFER_NAMES[index],
                        offer.credits,
                        offer.premium,
                        EconomyLedger.PREMIUM,
                    )
                },
            ),
        )

    private val TOKEN_OFFER_NAMES = listOf("GET_SNOWBALL_TOKENS", "GET_SNOWBALL_TOKENS2", "GET_SNOWBALL_TOKENS3")
}
