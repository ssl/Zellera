package zel.protocol.outgoing.moderation

import zel.game.moderation.ModerationCatalog
import zel.game.moderation.SupportTicket
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.players.Players
import zel.game.players.connection
import zel.net.clients.ClientConnection
import zel.protocol.generated.CfhTopicsInitOutCodec
import zel.protocol.generated.IssueDeletedOut
import zel.protocol.generated.IssueDeletedOutCodec
import zel.protocol.generated.IssueInfoOutCodec
import zel.protocol.generated.ModeratorInitOutCodec
import zel.protocol.generated.OutgoingCodec

class ModerationOutbox(
    private val players: Players,
) {
    fun <T> send(
        client: ClientConnection,
        codec: OutgoingCodec<T>,
        message: T,
    ) = client.send(codec, message)

    /** To the players who work the ticket queue: the tool alone shows no tickets. */
    private fun <T> toQueue(
        codec: OutgoingCodec<T>,
        message: T,
    ) {
        for (player in players.all) {
            if (seesQueue(player)) player.client?.send(codec, message)
        }
    }

    fun issueInfoToQueue(
        issue: SupportTicket,
        now: Int,
    ) = toQueue(IssueInfoOutCodec, ModerationMessages.issueInfo(issue, now))

    fun issueDeletedToQueue(issueId: Int) = toQueue(IssueDeletedOutCodec, IssueDeletedOut(issueId))

    fun cfhTopics(
        client: ClientConnection,
        catalog: ModerationCatalog,
    ) {
        val categories =
            catalog.cfhCategories.map { category ->
                ModerationMessages.CfhCategoryView(
                    category.name,
                    category.topics.map {
                        ModerationMessages.CfhTopicView(
                            it.name,
                            it.id,
                            it.consequence,
                        )
                    },
                )
            }
        send(client, CfhTopicsInitOutCodec, ModerationMessages.cfhTopics(categories))
    }

    fun moderatorInit(
        player: Player,
        open: List<SupportTicket>,
        catalog: ModerationCatalog,
        now: Int,
    ) {
        val canSeeQueue = seesQueue(player)
        val rights =
            ModerationMessages.ModeratorRights(
                ticketQueue = canSeeQueue,
                userLogs = player.hasPermission(Privilege.MODERATION_PLAYER_CHATLOG),
                userAlert = player.hasPermission(Privilege.MODERATION_PLAYER_ALERT),
                userKick = player.hasPermission(Privilege.MODERATION_PLAYER_KICK),
                userBan = player.hasPermission(Privilege.MODERATION_PLAYER_BAN),
                roomInfo = player.hasPermission(Privilege.MODERATION_ROOM_INFO),
                roomLogs = player.hasPermission(Privilege.MODERATION_ROOM_CHATLOG),
            )
        val init =
            ModerationMessages.moderatorInit(
                open,
                canSeeQueue,
                catalog.userPresets,
                catalog.issueCategoryNames,
                rights,
                catalog.roomPresets,
                now,
            )
        send(player.connection, ModeratorInitOutCodec, init)
    }

    companion object {
        fun seesQueue(player: Player): Boolean =
            player.hasPermission(Privilege.MODERATION_TOOL) && player.hasPermission(Privilege.MODERATION_TICKETS)
    }
}
