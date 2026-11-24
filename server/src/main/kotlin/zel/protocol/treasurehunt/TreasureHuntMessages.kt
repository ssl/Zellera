package zel.protocol.treasurehunt

import zel.net.clients.ClientConnection
import zel.protocol.generated.TreasureHuntFailOut
import zel.protocol.generated.TreasureHuntFailOutCodec
import zel.protocol.generated.TreasureHuntFirstWinnerOut
import zel.protocol.generated.TreasureHuntFirstWinnerOutCodec
import zel.protocol.generated.TreasureHuntUpdateOut
import zel.protocol.generated.TreasureHuntUpdateOutCodec

class TreasureHuntMessages {
    fun failed(
        client: ClientConnection,
        huntCode: String,
        requiredLevel: Int,
        requiredLevelPaying: Int,
    ) = client.send(
        TreasureHuntFailOutCodec,
        TreasureHuntFailOut(huntCode, requiredLevel, requiredLevelPaying),
    )

    fun update(
        client: ClientConnection,
        huntCode: String,
        stepsCompleted: Int,
        totalSteps: Int,
        completed: Boolean,
    ) = client.send(
        TreasureHuntUpdateOutCodec,
        TreasureHuntUpdateOut(huntCode, stepsCompleted, totalSteps, completed),
    )

    fun firstWinner(
        clients: Collection<ClientConnection>,
        huntCode: String,
        userId: Int,
        userName: String,
        figure: String,
        gender: String,
    ) {
        val winner = TreasureHuntFirstWinnerOut(huntCode, userId, userName, figure, gender)
        clients.forEach { it.send(TreasureHuntFirstWinnerOutCodec, winner) }
    }
}
