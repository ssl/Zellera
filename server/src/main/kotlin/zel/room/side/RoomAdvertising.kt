package zel.room.side

import zel.db.Sql
import zel.db.allOrNothing
import zel.db.intOrNone
import zel.db.noneAsNull
import zel.db.orFallback
import zel.db.write
import zel.game.catalog.Catalog
import zel.game.catalog.CatalogWallet
import zel.game.catalog.layouts.PageForm
import zel.game.items.ProductKind
import zel.game.moderation.WordFilter
import zel.game.players.Player
import zel.net.clients.ClientConnection
import zel.protocol.generated.NotificationDialogOut
import zel.protocol.generated.NotificationDialogOutCodec
import zel.protocol.generated.RoomAdErrorOut
import zel.protocol.generated.RoomAdErrorOutCodec
import zel.protocol.generated.RoomAdPurchaseInfoOut
import zel.protocol.generated.RoomEventCancelOut
import zel.protocol.generated.RoomEventCancelOutCodec
import zel.protocol.generated.RoomEventOut
import zel.protocol.generated.RoomEventOutCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.users.UserCurrency
import zel.room.RoomRegistry
import zel.room.listing.RoomListings
import zel.room.tick.Audience
import zel.room.tick.Lane
import zel.util.SECONDS_PER_MINUTE
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class RoomAdvertising(
    private val dataSource: DataSource,
    private val registry: RoomRegistry,
    private val listings: RoomListings,
    private val catalog: () -> Catalog?,
    private val wordFilter: () -> WordFilter?,
    private val now: () -> Int,
    private val promotionBadge: () -> String = { "" },
    private val blockedText: () -> String = { "" },
) {
    private val sql = Sql(dataSource)

    data class Purchase(
        val pageId: Int,
        val offerId: Int,
        val roomId: Int,
        val title: String,
        val description: String,
        val category: Int,
    )

    private data class Stored(
        val title: String,
        val description: String,
        val start: Int,
        val end: Int,
        val category: Int,
    )

    fun purchaseInfo(player: Player): RoomAdPurchaseInfoOut =
        RoomAdPurchaseInfoOut(
            true,
            listings
                .ofOwner(player.profile.id)
                .map { RoomAdPurchaseInfoOut.List1Item(it.id, it.name, true) },
        )

    fun buy(
        client: ClientConnection,
        player: Player,
        purchase: Purchase,
    ) {
        val room = listings.room(purchase.roomId)?.takeIf { it.ownerId == player.profile.id } ?: return
        val offer =
            catalog()
                ?.page(purchase.pageId)
                ?.takeIf { it.form.role == PageForm.Role.ROOM_ADS }
                ?.takeIf { it.sellsTo(player.profile.rank.id, player.stats.club.isMember) }
                ?.offer(purchase.offerId)
                ?: return
        val title = eventText(purchase.title)
        val description = eventText(purchase.description)
        val blockedField = blocked(player, title, description)
        if (blockedField != null) return blockedDialog(client, blockedField)
        val price = offer.price
        if (!CatalogWallet.chargeQuietly(player, price.credits, price.pointsType, price.points)) return
        client.send(UserCurrency(player))
        val running = stored(room.id)
        val at = now()
        val event =
            if (running != null && running.end > at) {
                running.copy(end = running.end + EVENT_SECONDS)
            } else {
                Stored(
                    title,
                    description,
                    at,
                    at + EVENT_SECONDS,
                    purchase.category,
                )
            }
        if (!store(room.id, event)) {
            CatalogWallet.refund(player, price.credits, price.pointsType, price.points)
            return
        }
        CatalogReplies.purchaseOk(client, CatalogMessages.purchaseOk(null))
        announce(room.id, room.ownerId, room.ownerName, event)
        offer.products
            .filter { it.type == ProductKind.BADGE }
            .forEach { player.inventory.badges.award(it.name, asUnseen = false) }
        givePromotionBadge(player)
    }

    private fun givePromotionBadge(player: Player) {
        val code = promotionBadge()
        if (code.isNotEmpty()) player.inventory.badges.award(code, asUnseen = false)
    }

    /** A filtered field is saved filtered and the owner gets the blocked dialog. */
    fun edit(
        client: ClientConnection,
        player: Player,
        roomId: Int,
        title: String,
        description: String,
    ) {
        val room = listings.room(roomId)?.takeIf { it.ownerId == player.profile.id } ?: return
        val running = stored(roomId)?.takeIf { it.end > now() } ?: return
        val filter = wordFilter()
        val fields =
            listOf(TITLE_FIELD to eventText(title), DESCRIPTION_FIELD to eventText(description)).map { (field, text) ->
                Triple(field, text, filter?.filter(text, player) ?: text)
            }
        fields.firstOrNull { (_, text, filtered) -> filtered != text }?.let { (field, _, _) ->
            blockedDialog(client, field)
        }
        fields.filter { (_, text, filtered) -> filtered != text }.forEach { (field, _, filtered) ->
            CatalogReplies.send(client, RoomAdErrorOutCodec, RoomAdErrorOut(field, filtered))
        }
        val edited = running.copy(title = fields[0].third, description = fields[1].third)
        if (!store(roomId, edited)) return
        CatalogReplies.send(client, RoomEventOutCodec, eventMessage(roomId, room.ownerId, room.ownerName, edited))
        announce(roomId, room.ownerId, room.ownerName, edited)
    }

    fun cancel(
        player: Player,
        roomId: Int,
    ): Boolean {
        listings.room(roomId)?.takeIf { it.ownerId == player.profile.id } ?: return false
        val running = stored(roomId)?.takeIf { it.end > now() } ?: return false
        if (!store(roomId, running.copy(end = now()), promoted = false)) return false
        registry.withRoom(roomId) { it.out.send(Lane.Now, Audience.Room, RoomEventCancelOutCodec, RoomEventCancelOut) }
        return true
    }

    private fun blocked(
        player: Player,
        title: String,
        description: String,
    ): Int? {
        val filter = wordFilter() ?: return null
        return listOf(TITLE_FIELD to title, DESCRIPTION_FIELD to description)
            .firstOrNull { (_, text) -> filter.filter(text, player) != text }
            ?.first
    }

    private fun blockedDialog(
        client: ClientConnection,
        field: Int,
    ) {
        val context = if (field == TITLE_FIELD) TITLE_CONTEXT else DESCRIPTION_CONTEXT
        val params =
            listOf(
                NotificationDialogOut.List1Item(CONTEXT_PARAM, context),
                NotificationDialogOut.List1Item(MESSAGE_PARAM, blockedText()),
            )
        CatalogReplies.send(client, NotificationDialogOutCodec, NotificationDialogOut(BLOCKED_DIALOG, params))
    }

    private fun announce(
        roomId: Int,
        ownerId: Int,
        ownerName: String,
        event: Stored,
    ) {
        val message = eventMessage(roomId, ownerId, ownerName, event)
        registry.withRoom(roomId) { it.out.send(Lane.Now, Audience.Room, RoomEventOutCodec, message) }
    }

    private fun eventMessage(
        roomId: Int,
        ownerId: Int,
        ownerName: String,
        event: Stored,
    ): RoomEventOut =
        RoomEventOut(
            roomId,
            ownerId,
            ownerName,
            roomId,
            EVENT_TYPE,
            event.title,
            event.description,
            0,
            (event.end - now()) / SECONDS_PER_MINUTE,
            event.category,
        )

    private fun stored(roomId: Int): Stored? =
        sql.select(SELECT, roomId).first { row ->
            Stored(
                row.getString("title"),
                row.getString("description"),
                row.intOrNone("starts_at", NO_START),
                row.getInt("ends_at"),
                row.getInt("category"),
            )
        }

    /** False when neither was written. */
    private fun store(
        roomId: Int,
        event: Stored,
        promoted: Boolean = true,
    ): Boolean =
        log.orFallback("storing the promotion of room $roomId", false) {
            dataSource.connection.use { connection ->
                connection.allOrNothing {
                    connection.write(REPLACE) {
                        int(roomId)
                        string(event.title)
                        string(event.description)
                        int(event.end)
                        value(noneAsNull(event.start, NO_START))
                        int(event.category)
                    }
                    connection.write(FLAG) {
                        boolean(promoted)
                        int(roomId)
                    }
                    true
                }
            } ?: false
        }

    companion object {
        const val MAX_TEXT_LENGTH = 64
        private val log = LoggerFactory.getLogger(RoomAdvertising::class.java)

        fun eventText(text: String): String = text.trim().take(MAX_TEXT_LENGTH)

        private const val EVENT_SECONDS = 7200
        private const val TITLE_FIELD = 0
        private const val DESCRIPTION_FIELD = 1
        private const val BLOCKED_DIALOG = "content_moderation.message_blocked"
        private const val CONTEXT_PARAM = "context"
        private const val MESSAGE_PARAM = "message"
        private const val TITLE_CONTEXT = "EVENT_NAME"
        private const val DESCRIPTION_CONTEXT = "EVENT_DESCRIPTION"
        private const val NO_START = -1
        private const val EVENT_TYPE = 1
        private const val SELECT =
            "SELECT title, description, starts_at, ends_at, category FROM room_promotions WHERE room_id = ?"
        private const val REPLACE =
            "REPLACE INTO room_promotions (room_id, title, description, ends_at, starts_at, category)" +
                " VALUES (?, ?, ?, ?, ?, ?)"
        private const val FLAG = "UPDATE rooms SET is_promoted = ? WHERE id = ?"
    }
}
