package zel.protocol.icons

import org.slf4j.LoggerFactory
import zel.game.achievements.AchievementTally
import zel.game.catalog.CatalogOffer
import zel.game.icons.IconAchievements
import zel.game.icons.IconRejected
import zel.game.icons.IconService
import zel.game.icons.IconShop
import zel.game.permissions.Privilege
import zel.game.players.Player
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.protocol.generated.BuyIconCollectionInCodec
import zel.protocol.generated.BuyIconInCodec
import zel.protocol.generated.ClaimIconInCodec
import zel.protocol.generated.FavoriteIconInCodec
import zel.protocol.generated.GetIconInfoInCodec
import zel.protocol.generated.GetIconShopDataInCodec
import zel.protocol.generated.IconInfoOutCodec
import zel.protocol.generated.IconShopDataOutCodec
import zel.protocol.generated.PlayerIconsOutCodec
import zel.protocol.generated.PurchaseErrorOut
import zel.protocol.generated.PurchaseErrorOutCodec
import zel.protocol.generated.TriggerIconInCodec
import zel.protocol.generated.UnfavoriteIconInCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.icons.iconInfo
import zel.protocol.outgoing.icons.iconShopData
import zel.protocol.outgoing.icons.playerIcons
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.typed.ClientCooldown
import zel.protocol.typed.PacketPackage
import zel.protocol.typed.PacketRoute
import zel.protocol.typed.PacketSession
import zel.room.port.RoomPort
import java.sql.SQLException

class IconPackets(
    private val rooms: () -> RoomPort? = { null },
) : PacketPackage {
    private val log = LoggerFactory.getLogger(IconPackets::class.java)
    private val triggerCooldown = ClientCooldown("icon.trigger", TRIGGER_GAP_MS)

    override val name = "icons"
    override val routes =
        listOf(
            PacketRoute(GetIconShopDataInCodec) { session, _ ->
                request(session, null) { service, player ->
                    // The owned icons are not sent again here; they arrive at login.
                    val snapshot = service.rosterOf(player)
                    session.send(IconShopDataOutCodec, iconShopData(snapshot))
                    if (snapshot.unseen.isNotEmpty()) {
                        session.client.send(
                            AddRoomItem(
                                snapshot.unseen.toIntArray(),
                                AddRoomItem.UnseenCategory.ICON,
                            ),
                        )
                    }
                }
            },
            PacketRoute(GetIconInfoInCodec) { session, m ->
                request(session, null) { service, player ->
                    session.send(IconInfoOutCodec, iconInfo(service.rosterOf(player).requireIcon(m.id)))
                }
            },
            PacketRoute(ClaimIconInCodec) { session, m ->
                request(session, IconService.Action.CLAIM) { service, player ->
                    service.perform(player, IconService.Action.CLAIM, m.id)
                    CatalogReplies.purchaseOk(
                        session.client,
                        CatalogMessages.purchaseOk(null as CatalogOffer?),
                    )
                }
            },
            PacketRoute(FavoriteIconInCodec) { session, m ->
                request(session, IconService.Action.FAVORITE) { service, player ->
                    service.perform(player, IconService.Action.FAVORITE, m.id)
                }
            },
            PacketRoute(UnfavoriteIconInCodec) { session, m ->
                request(session, IconService.Action.UNFAVORITE) { service, player ->
                    service.perform(player, IconService.Action.UNFAVORITE, m.id)
                }
            },
            PacketRoute(BuyIconInCodec) { session, m ->
                session.client.player?.let { IconShop.buy(it, false, m.id) }
            },
            PacketRoute(BuyIconCollectionInCodec) { session, m ->
                session.client.player?.let { IconShop.buy(it, true, m.id) }
            },
            PacketRoute(TriggerIconInCodec) { session, m -> trigger(session, m.iconId) },
        )

    private fun request(
        session: PacketSession,
        action: IconService.Action?,
        block: (IconService, Player) -> Unit,
    ) {
        val player = session.client.player ?: return
        try {
            block(player.iconService, player)
        } catch (e: IconRejected) {
            fail(session, action, e.code)
        } catch (e: SQLException) {
            log.error("Unable to process Icon request for user {}", player.profile.id, e)
            fail(session, action, SERVER_ERROR)
        }
    }

    private fun fail(
        session: PacketSession,
        action: IconService.Action?,
        code: Int,
    ) {
        if (action == IconService.Action.CLAIM) {
            CatalogReplies.send(session.client, PurchaseErrorOutCodec, PurchaseErrorOut(code))
        }
    }

    private fun trigger(
        session: PacketSession,
        iconId: Int,
    ) {
        val client = session.client
        if (!triggerCooldown.acquire(client)) return
        val player = client.player ?: return
        if (iconId <= 0 || iconId > MAX_ICON_ID) return
        val port = rooms() ?: return
        val icons = player.iconService
        val id = player.profile.id
        if (port.roomOf(id) == null || !player.stats.standing.mayTalk()) return
        if (!icons.use(id, IconService.allUnlocked(player), iconId)) return
        if (!port.showIcon(id, iconId, player.hasPermission(Privilege.CHAT_MUTE_IMMUNE))) return
        PlayerActions.progress(player, CountedAction.USE_ICON, 1)
        IconAchievements.used(player, AchievementTally.running())
        session.send(PlayerIconsOutCodec, playerIcons(icons.rosterOf(player)))
    }

    private companion object {
        const val SERVER_ERROR = 5
        const val MAX_ICON_ID = 1_000_000
        const val TRIGGER_GAP_MS = 1000L
    }
}
