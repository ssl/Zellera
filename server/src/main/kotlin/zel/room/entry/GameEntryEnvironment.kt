package zel.room.entry

import zel.game.achievements.AchievementEvents
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.players.Player
import zel.game.players.level.AccountLevels
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.net.clients.ClientConnections
import zel.protocol.generated.CloseConnectionOut
import zel.protocol.generated.CloseConnectionOutCodec
import zel.protocol.generated.OutgoingCodec
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.generic.alerts.BubbleAlert
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage

class GameEntryEnvironment(
    private val clients: ClientConnections,
    private val persistence: (key: Any, job: Runnable) -> Unit,
    private val clockSeconds: () -> Int,
    private val accountLevels: () -> AccountLevels? = { null },
) : EntryEnvironment {
    override fun player(id: Int): Player? =
        clients
            .owner(id)
            ?.takeIf { it.channel.isOpen }
            ?.player

    override fun persistInOrder(
        key: Any,
        job: Runnable,
    ) = persistence(key, job)

    override fun nowSeconds(): Int = clockSeconds()

    override fun landing(player: Player) {
        player.client?.send(CloseConnectionOutCodec, CloseConnectionOut)
    }

    override fun <T> send(
        player: Player,
        codec: OutgoingCodec<T>,
        message: T,
    ) {
        player.client?.send(codec, message)
    }

    override fun buildPassStatus(owner: Player) {
        owner.client?.let { CatalogReplies.buildPassStatus(it, owner) }
    }

    override fun buildPassVisitDenied(
        owner: Player,
        visitor: String,
    ) {
        owner.client?.send(
            BubbleAlertMessage(BubbleAlert.BUILD_PASS_OWNER_KEPT_OUT.key, mapOf("USERNAME" to visitor)),
        )
    }

    override fun visitEnded(
        ownerId: Int,
        visitorLevel: Int,
        minutes: Int,
    ) {
        accountLevels()?.visitEnded(ownerId, visitorLevel, minutes)
    }

    override fun countEntry(
        player: Player,
        firstVisit: Boolean,
    ) {
        if (firstVisit) {
            player.services.game.achievements
                ?.count(player, GameEvents.ROOM_ENTERED)
        }
        PlayerActions.progress(player, CountedAction.ENTER_OTHER_USERS_ROOM, 1)
    }

    override fun entered(
        player: Player,
        roomId: Int,
    ) {
        AchievementEvents.roomTime().arrived(player, roomId)
        player.services.game.pollOffers
            ?.roomEntered(player, roomId)
    }

    override fun left(player: Player) {
        AchievementEvents.roomTime().departed(player)
    }
}
