package zel.protocol.incoming.guilds

import zel.core.rules.rules
import zel.game.guilds.Guild
import zel.game.guilds.GuildBadgeCode
import zel.game.guilds.GuildEditFailure
import zel.game.guilds.NewGuild
import zel.protocol.generated.ExtendedProfileChangedOut
import zel.protocol.generated.ExtendedProfileChangedOutCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchasedOffer
import zel.protocol.typed.PacketIds
import zel.room.port.RoomSummary

/** Checks run in the order the client expects, then the charge. */
internal class GuildFounding(
    private val services: GuildServices,
) {
    private val guilds = services.guilds
    private val messages = services.messages

    class Request(
        val name: String,
        val description: String,
        val roomId: Int,
        val colorOne: Int,
        val colorTwo: Int,
        val parts: List<Int>,
    )

    private sealed interface Stop {
        data class EditFailed(
            val reason: GuildEditFailure,
        ) : Stop

        data object PurchaseFailed : Stop

        data object NotTheOwner : Stop

        data object Ignored : Stop
    }

    fun found(
        caller: Caller,
        request: Request,
    ) {
        val name = services.wordFilter.filter(request.name, caller.player)
        val description = services.wordFilter.filter(request.description, caller.player)
        val room = if (PacketIds.isId(request.roomId)) services.live().summary(request.roomId) else null
        val badge = GuildBadgeCode.fromEditor(request.parts)
        val stop = stopOf(caller, request, name, description, room)
        when {
            stop != null -> {
                answer(caller, stop)
            }

            room == null || badge == null -> {
                answer(caller, Stop.PurchaseFailed)
            }

            else -> {
                charge(
                    caller,
                    room,
                    NewGuild(
                        name,
                        description,
                        NewGuild.Home(room.id, room.name),
                        NewGuild.Crest(badge, request.colorOne, request.colorTwo),
                    ),
                )
            }
        }
    }

    private fun stopOf(
        caller: Caller,
        request: Request,
        name: String,
        description: String,
        room: RoomSummary?,
    ): Stop? =
        when {
            !GuildInput.isValidName(name) -> {
                Stop.EditFailed(GuildEditFailure.BAD_NAME)
            }

            !GuildInput.isValidDescription(description) -> {
                Stop.Ignored
            }

            rules.groupsRequireClub && !caller.player.stats.club.isMember -> {
                Stop.EditFailed(
                    GuildEditFailure.CLUB_REQUIRED,
                )
            }

            room == null -> {
                Stop.PurchaseFailed
            }

            room.groupId != 0 -> {
                Stop.EditFailed(GuildEditFailure.ROOM_TAKEN)
            }

            room.ownerId != caller.userId -> {
                Stop.NotTheOwner
            }

            !guilds.parts.hasSymbolColour(
                request.colorOne,
            ) ||
                !guilds.parts.hasBackgroundColour(
                    request.colorTwo,
                )
            -> {
                Stop.EditFailed(GuildEditFailure.BAD_NAME)
            }

            guilds.members.atMembershipLimit(caller.player) -> {
                Stop.PurchaseFailed
            }

            else -> {
                null
            }
        }

    private fun answer(
        caller: Caller,
        stop: Stop,
    ) {
        when (stop) {
            is Stop.EditFailed -> {
                messages.sendEditFailed(caller.client, stop.reason)
            }

            Stop.PurchaseFailed -> {
                CatalogReplies.purchaseFailed(caller.client, PurchaseError.GENERIC)
            }

            Stop.NotTheOwner -> {
                caller.client.warnings.refused("not the owner")
            }

            Stop.Ignored -> {}
        }
    }

    private fun charge(
        caller: Caller,
        room: RoomSummary,
        details: NewGuild,
    ) {
        val price = services.config.getInt("groups.founding.price.credits")
        val player = caller.player
        if (price > 0 && (player.profile.credits < price || !player.wallet.takeCredits(price))) {
            answer(caller, Stop.PurchaseFailed)
            return
        }
        val guild = guilds.create(player, details)
        if (guild == null) {
            if (price > 0) player.wallet.changeCredits(price)
            answer(caller, Stop.PurchaseFailed)
            return
        }
        announce(caller, room, guild, price)
    }

    private fun announce(
        caller: Caller,
        room: RoomSummary,
        guild: Guild,
        price: Int,
    ) {
        val client = caller.client
        guilds.remember(guild)
        if (services.config.getBoolean("groups.badges.are_drawn")) services.badgeImager().generate(guild)
        CatalogReplies.purchaseOk(client, CatalogMessages.purchaseOkOffer(PurchasedOffer.guild(price)))
        client.send(ExtendedProfileChangedOutCodec, ExtendedProfileChangedOut(caller.userId))
        messages.sendCreated(client, guild)
        services.live().assignGroup(room.id, guild.id)
        messages.sendMemberships(client, guilds.parts, groupsOf(caller.player, guilds), caller.player)
    }
}
