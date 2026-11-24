package zel.protocol.incoming.users

import zel.game.players.Player
import zel.game.players.inventory.UnseenItems
import zel.protocol.generated.GetBadgesInCodec
import zel.protocol.generated.GetPetInventoryInCodec
import zel.protocol.generated.PlayerEffectActivatedInCodec
import zel.protocol.generated.PlayerEffectSelectedInCodec
import zel.protocol.generated.RequestFurniInventoryInCodec
import zel.protocol.generated.RequestFurniInventoryWhenNotInRoomInCodec
import zel.protocol.generated.ResetUnseenItemIdsInCodec
import zel.protocol.generated.ResetUnseenItemsInCodec
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.InventoryBadges
import zel.protocol.outgoing.inventory.InventoryItems
import zel.protocol.outgoing.inventory.InventoryPets
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.player
import zel.server.GameServices

class InventoryPackets(
    private val services: GameServices,
) : PacketPackage {
    private val furniCooldown = ClientCooldown("inventory.items", FURNI_GAP_MS)
    private val landingFurniCooldown = ClientCooldown("inventory.items.landing", FURNI_GAP_MS)

    override val name = "inventory"

    override val routes =
        listOf(
            PacketRoute(RequestFurniInventoryInCodec) { session, _ ->
                if (furniCooldown.acquire(session.client)) sendFurni(session.player)
            },
            PacketRoute(RequestFurniInventoryWhenNotInRoomInCodec) { session, _ ->
                if (landingFurniCooldown.acquire(session.client)) sendFurni(session.player)
            },
            PacketRoute(GetBadgesInCodec) { session, _ -> session.client.send(InventoryBadges(session.player)) },
            PacketRoute(GetPetInventoryInCodec) { session, _ -> session.client.send(InventoryPets(session.player)) },
            PacketRoute(ResetUnseenItemsInCodec) { session, m -> markSeen(session.player, m.category, null) },
            PacketRoute(ResetUnseenItemIdsInCodec) { session, m -> markSeen(session.player, m.category, m.itemIds) },
            PacketRoute(PlayerEffectActivatedInCodec) { session, m -> activateEffect(session.player, m.effectId) },
            PacketRoute(PlayerEffectSelectedInCodec) { session, m -> selectEffect(session.player, m.effectId) },
        )

    private fun sendFurni(player: Player) {
        val client = player.client ?: return
        InventoryItems.fragments(player.inventory.furni.all, FRAGMENT_SIZE).forEach(client::send)
    }

    /** Chat icons keep their own marks. */
    private fun markSeen(
        player: Player,
        category: Int,
        ids: List<Int>?,
    ) {
        if (category <= 0) return
        val distinct = ids?.distinct()
        if (distinct != null && (distinct.isEmpty() || distinct.size > UnseenItems.MOST_IDS_PER_RESET)) return
        when {
            category == AddRoomItem.UnseenCategory.ICON.number -> {
                player.iconService.markSeen(player.profile.id, distinct.orEmpty())
            }

            distinct == null -> {
                player.inventory.unseen.clear(category)
            }

            else -> {
                player.inventory.unseen.seen(category, LinkedHashSet(distinct))
            }
        }
    }

    /** Effects from furni and games come from the room, never from this packet. */
    private fun activateEffect(
        player: Player,
        effectId: Int,
    ) {
        if (mayWear(player, effectId)) player.inventory.effects.wear(effectId)
    }

    private fun selectEffect(
        player: Player,
        effectId: Int,
    ) {
        val effects = player.inventory.effects
        when {
            effectId <= 0 -> effects.takeOff()
            effects.worn == effectId -> Unit
            mayWear(player, effectId) -> effects.wear(effectId)
        }
    }

    private fun mayWear(
        player: Player,
        effectId: Int,
    ): Boolean =
        player.inventory.effects.owns(effectId) &&
            !checkNotNull(services.game.permissions).blocksEffect(effectId, player.profile.rank.id)

    private companion object {
        const val FURNI_GAP_MS = 500L
        const val FRAGMENT_SIZE = 1000
    }
}
