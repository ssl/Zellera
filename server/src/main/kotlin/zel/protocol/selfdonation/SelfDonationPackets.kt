package zel.protocol.selfdonation

import zel.game.items.Item
import zel.game.items.ProductKind
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.protocol.generated.FurniListInvalidateOut
import zel.protocol.generated.FurniListInvalidateOutCodec
import zel.protocol.generated.SelfDonateItemIn
import zel.protocol.generated.SelfDonateItemInCodec
import zel.protocol.generated.SelfDonationResultOut
import zel.protocol.generated.SelfDonationResultOutCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.server.GameServices

class SelfDonationPackets(
    private val services: GameServices,
) : PacketPackage {
    private val cooldown = ClientCooldown("selfdonation", GAP_MS)

    override val name = "selfdonation"
    override val routes =
        listOf(
            PacketRoute(SelfDonateItemInCodec) { session, m ->
                val player = session.client.player ?: return@PacketRoute
                if (!cooldown.acquire(session.client)) return@PacketRoute
                session.send(SelfDonationResultOutCodec, SelfDonationResultOut(donate(session, player, m)))
            },
        )

    private fun donate(
        session: PacketSession,
        player: Player,
        request: SelfDonateItemIn,
    ): Int {
        val config = services.config
        if (!config.getBoolean(ENABLED) || !player.hasPermission(Privilege.DEBUG_SELF_DONATION)) return NOT_ALLOWED
        if (request.amount !in 1..config.getInt(MAX_AMOUNT, DEFAULT_MAX_AMOUNT)) return FAILED
        val definition = definition(request) ?: return FAILED
        val items = services.game.items ?: return FAILED
        val extraData = if (request.isWallItem) request.legacyPosterId else ""
        repeat(request.amount) {
            val made = items.newFurni(player.profile.id, definition, 0, 0, extraData) ?: return FAILED
            player.inventory.furni.put(made)
            session.client.send(AddRoomItem(made))
        }
        session.client.send(FurniListInvalidateOutCodec, FurniListInvalidateOut)
        return SUCCESS
    }

    private fun definition(request: SelfDonateItemIn): Item? {
        if (request.typeId <= 0) return null
        val type = if (request.isWallItem) ProductKind.WALL_FURNI else ProductKind.FLOOR_FURNI
        val definitions = services.game.items?.getItems() ?: return null
        return synchronized(definitions) {
            definitions.values
                .filter { it.spriteId == request.typeId && it.type == type }
                .minByOrNull { it.id }
        }
    }

    private companion object {
        const val ENABLED = "self_donation.is_on"
        const val MAX_AMOUNT = "self_donation.max_amount"
        const val DEFAULT_MAX_AMOUNT = 500
        const val GAP_MS = 1000L

        const val SUCCESS = 0
        const val NOT_ALLOWED = 1
        const val FAILED = 2
    }
}
