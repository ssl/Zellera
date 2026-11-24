package zel.game.catalog

import zel.game.achievements.AchievementEvents
import zel.game.achievements.GameEvents
import zel.game.achievements.count
import zel.game.bots.Bot
import zel.game.bots.BotLook
import zel.game.economy.EconomyLedger
import zel.game.economy.LedgerEntry
import zel.game.items.Item
import zel.game.items.ItemInteraction
import zel.game.items.OwnedFurni
import zel.game.items.ProductKind
import zel.game.pets.Pet
import zel.game.players.ChatStyles
import zel.game.players.Gender
import zel.game.players.Player
import zel.game.players.PlayerBadge
import zel.game.players.inventory.AvatarEffect
import zel.game.players.inventory.AvatarEffects
import zel.game.players.inventory.BadgeCase
import zel.game.quests.CountedAction
import zel.game.quests.PlayerActions
import zel.net.clients.ClientConnection
import zel.protocol.Outbound
import zel.protocol.generated.ActivityPointNotificationOut
import zel.protocol.generated.ActivityPointNotificationOutCodec
import zel.protocol.generated.PetReceivedOutCodec
import zel.protocol.outgoing.catalog.CatalogMessages
import zel.protocol.outgoing.catalog.CatalogReplies
import zel.protocol.outgoing.catalog.PurchaseError
import zel.protocol.outgoing.catalog.PurchaseNotAllowed
import zel.protocol.outgoing.generic.alerts.BubbleAlertMessage
import zel.protocol.outgoing.inventory.AddBot
import zel.protocol.outgoing.inventory.AddPet
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.AddRoomItem.UnseenCategory
import zel.protocol.outgoing.inventory.InventoryFurni
import zel.protocol.outgoing.inventory.InventoryUpdateItem
import zel.protocol.outgoing.users.AddUserBadge
import zel.protocol.outgoing.users.UserCredits
import zel.protocol.outgoing.users.UserCurrency
import java.sql.Connection
import java.sql.SQLException

/** The buyer hears of an order only after its commit. */
internal class PurchaseDelivery(
    private val services: CatalogServices,
) {
    private val ledger = CatalogPurchaseLedger(services.database)

    class Made {
        val furni: MutableList<OwnedFurni> = ArrayList()
        val badges: MutableList<PlayerBadge> = ArrayList()
        val bots: MutableList<Bot> = ArrayList()
        val pets: MutableList<Pet> = ArrayList()
        val effects: MutableList<AvatarEffect> = ArrayList()
        var chatStyle: Int? = null
    }

    /** Null after answering the buyer when something in the order cannot be sold to them. */
    @Throws(SQLException::class)
    fun deliver(
        order: PurchaseOrder,
        reservation: LimitedReservation?,
        chatStyle: Int? = null,
    ): Made? {
        if (!admissible(order, chatStyle)) return null
        val player = order.player
        val made =
            ledger.settle(player, LedgerEntry.newKey("shop/${player.profile.id}")) { connection ->
                val made = Made()
                chatStyle?.let {
                    ChatStyles.grant(connection, player.profile.id, it, services.now())
                    made.chatStyle = it
                }
                order.item.products.forEach { base ->
                    val copies = order.item.quantityOf(base.id) * order.amount
                    repeat(copies) { create(connection, order, base, reservation, made) }
                }
                PurchaseCharge(made, order.credits, order.points, order.pointsType)
            }
        publish(order, made)
        val record = record(order, made)
        services.threading.write { record.store(services.dataSource) }
        return made
    }

    private fun admissible(
        order: PurchaseOrder,
        chatStyle: Int?,
    ): Boolean {
        val player = order.player
        val client = player.client
        val products = order.item.products
        val refused =
            when {
                chatStyle != null && player.services.chatStyles.has(player.profile.id, chatStyle) -> {
                    client?.let { CatalogReplies.purchaseFailed(it, PurchaseError.CHAT_STYLE_OWNED) }
                    true
                }

                products.any { it.type == ProductKind.BADGE && player.inventory.badges.owns(it.name) } -> {
                    client?.let { CatalogReplies.purchaseFailed(it, PurchaseError.BADGE_OWNED) }
                    true
                }

                products.any { it.type == ProductKind.AVATAR_EFFECT && ownsPermanently(player, it) } -> {
                    client?.send(BubbleAlertMessage(PERMANENT_EFFECT_OWNED))
                    true
                }

                products.any { it.isPet && !picksSellablePalette(it, order.extraData) } -> {
                    client?.let { CatalogReplies.purchaseFailed(it, PurchaseError.GENERIC) }
                    true
                }

                products.any { it.isPet && isClubOnlyBreed(it, order.extraData) } && !player.stats.club.isMember -> {
                    client?.let { CatalogReplies.purchaseNotAllowed(it, PurchaseNotAllowed.CLUB_ONLY) }
                    true
                }

                products.any { it.interaction.isGroupFurni } && !mayBuyGroupFurni(order) -> {
                    client?.let { CatalogReplies.purchaseFailed(it, PurchaseError.GENERIC) }
                    true
                }

                else -> {
                    false
                }
            }
        return !refused
    }

    private fun picksSellablePalette(
        base: Item,
        extraData: String,
    ): Boolean {
        val catalog = services.pets.catalog
        val type = catalog.typeSoldBy(base.name) ?: return false
        return PetOrders.picksSellablePalette(extraData, catalog.palettesOf(type))
    }

    private fun isClubOnlyBreed(
        base: Item,
        extraData: String,
    ): Boolean {
        val catalog = services.pets.catalog
        val type = catalog.typeSoldBy(base.name) ?: return false
        val breed = extraData.split("\n").getOrNull(1)?.toIntOrNull() ?: return false
        return catalog.isClubOnlyBreed(type, breed)
    }

    private fun create(
        connection: Connection,
        order: PurchaseOrder,
        base: Item,
        reservation: LimitedReservation?,
        made: Made,
    ) {
        val player = order.player
        val playerId = player.profile.id
        when {
            base.type == ProductKind.BADGE -> {
                made.badges += BadgeCase.storeIn(player.services, connection, playerId, base.name)
            }

            base.type == ProductKind.AVATAR_EFFECT -> {
                val effectId = effectOf(base, player)
                // an effect id the furni lacks gives nothing; a permanent effect is held once however many copies
                val held = base.isPermanentEffect && made.effects.any { it.id == effectId }
                if (effectId > 0 && !held) {
                    val duration = if (base.isPermanentEffect) 0 else AvatarEffect.TEMPORARY_SECONDS
                    made.effects += AvatarEffects.storePurchase(connection, playerId, effectId, duration)
                }
            }

            base.isBot -> {
                made.bots += services.bots.create(connection, botLook(order, base), BOT_TYPE, playerId)
                    ?: throw SQLException("no bot for ${base.name}")
            }

            base.isPet -> {
                made.pets += pet(connection, order, base)
            }

            else -> {
                made.furni += furni(connection, order, base, reservation)
            }
        }
    }

    private fun furni(
        connection: Connection,
        order: PurchaseOrder,
        base: Item,
        reservation: LimitedReservation?,
    ): List<OwnedFurni> {
        val player = order.player
        val playerId = player.profile.id
        val extraData =
            services.catalog.engravings.purchasedState(player, order.item, base, order.extraData)
                ?: throw SQLException("no song for ${base.name} in offer ${order.item.id}")
        val stack = reservation?.stack ?: 0
        val number = reservation?.number ?: 0
        val make = {
            services.items.newFurni(connection, playerId, base, stack, number, extraData)
                ?: throw SQLException("no furni for ${base.name}")
        }
        val first = make()
        reservation?.stock?.markSold(connection, player, first)
        if (base.interaction.isGroupFurni) {
            orderedGroup(order)?.let { group ->
                services.game.guilds?.tagFurni(connection, first.id, group.id)
                first.guildId = group.id
            }
        }
        if (!base.interaction.isTeleport) return listOf(first)
        val second = make()
        services.items.pairTeleports(connection, first.id, second.id)
        return listOf(first, second)
    }

    private fun pet(
        connection: Connection,
        order: PurchaseOrder,
        base: Item,
    ): Pet {
        val parts = order.extraData.split("\n")
        require(parts.size >= PetOrders.PARTS) { "a pet purchase names the pet, its race and its colour" }
        return services.pets.maker.boughtOn(connection, base, parts[0], parts[1], parts[2], order.player.profile.id)
            ?: throw SQLException("no pet for ${base.name}")
    }

    private fun botLook(
        order: PurchaseOrder,
        base: Item,
    ): BotLook = BotLook(base.displayName).withPreset(order.item.presetState)

    private fun record(
        order: PurchaseOrder,
        made: Made,
    ) = ShopPurchaseRecord(
        services.now(),
        order.player.profile.id,
        order.item.id,
        made.furni.map { it.id },
        order.item.name,
        order.credits,
        order.points,
        order.pointsType,
        order.amount,
    )

    private fun publish(
        order: PurchaseOrder,
        made: Made,
    ) {
        val player = order.player
        val client = player.client
        val inventory = player.inventory
        made.pets.forEach { pet ->
            inventory.pets.put(pet)
            services.achievements.count(player, GameEvents.PET_ACQUIRED)
            services.pets.acquired(player.profile.id, pet)
            client?.let {
                CatalogReplies.send(it, PetReceivedOutCodec, CatalogMessages.petReceived(pet, false))
                it.send(AddPet(pet))
            }
        }
        unseen(client, UnseenCategory.PET, made.pets.map { it.id })
        unseen(client, UnseenCategory.ICON, made.effects.map { it.id })
        tellBalances(order)
        made.badges.forEach { badge ->
            inventory.badges.add(badge)
            client?.send(AddUserBadge(badge, player.services.badgeCaches.ownerCounts))
        }
        unseen(client, UnseenCategory.BADGE, made.badges.map { it.id })
        made.bots.forEach { bot ->
            inventory.bots.put(bot)
            client?.send(AddBot(bot))
        }
        unseen(client, UnseenCategory.BOT, made.bots.map { it.id })
        if (made.furni.isNotEmpty()) inventory.furni.putAll(made.furni)
        if (made.furni.any { isForum(it.definition) }) openForum(order)
        val confirm: () -> Unit = {
            client?.let { CatalogReplies.purchaseOk(it, CatalogMessages.purchaseOk(order.item)) }
        }
        if (made.furni.isEmpty()) confirm() else announceFurni(client, made.furni, confirm)
        if (made.furni.isNotEmpty()) PlayerActions.progress(player, CountedAction.BUY_FROM_CATALOGUE, 1)
        if (made.furni.any { it.definition.interaction.name == ItemInteraction.MUSIC_DISC }) {
            services.achievements.count(player, GameEvents.CATALOG_SONG_BOUGHT)
        }
        made.effects.forEach { inventory.effects.accept(it) }
        made.chatStyle?.let {
            player.services.chatStyles.tellGranted(player, it)
            AchievementEvents.identity().mirrorChatStyles(player)
        }
        if (order.item.isLimited) {
            player.stats.limitedPurchases.add(order.item.id, services.now())
            LimitedEditionAchievements.purchased(player, order.item)
        }
    }

    /** PurchaseOK precedes unseen marks only for furni without data; a wired chest is also listed before its mark. */
    private fun announceFurni(
        client: ClientConnection?,
        furni: List<OwnedFurni>,
        confirm: () -> Unit,
    ) {
        val chest = furni.any { it.definition.interaction.isStorage }
        val unseenFirst = chest || furni.any(InventoryFurni::hasStuffData)
        if (chest) client?.send(InventoryUpdateItem(furni))
        if (!unseenFirst) confirm()
        client?.send(AddRoomItem(furni))
        if (unseenFirst) confirm()
        client?.send(InventoryUpdateItem(furni))
    }

    private fun orderedGroup(order: PurchaseOrder) =
        order.extraData.toIntOrNull()?.let { services.game.guilds?.byId(it) }

    private fun mayBuyGroupFurni(order: PurchaseOrder): Boolean {
        val group = orderedGroup(order) ?: return false
        val buyer = order.player
        if (order.item.products.any { isForum(it) }) return group.ownerId == buyer.profile.id
        return services.game.guilds
            ?.members
            ?.find(group, buyer)
            ?.role
            ?.isMember == true
    }

    private fun openForum(order: PurchaseOrder) {
        val guilds = services.game.guilds ?: return
        val guild = orderedGroup(order) ?: return
        guild.openForum()
        services.threading.run(Runnable { guilds.save(guild) })
    }

    private fun unseen(
        client: ClientConnection?,
        category: UnseenCategory,
        ids: List<Int>,
    ) {
        if (ids.isNotEmpty()) client?.send(AddRoomItem(ids.toIntArray(), category))
    }

    private fun tellBalances(order: PurchaseOrder) {
        val player = order.player
        val client = player.client ?: return
        if (order.points > 0) {
            client.send(
                if (order.pointsType == EconomyLedger.POINTS) {
                    UserCurrency(player).compose()
                } else {
                    Outbound(
                        ActivityPointNotificationOutCodec,
                        ActivityPointNotificationOut(
                            player.profile.balance(order.pointsType),
                            -order.points,
                            order.pointsType,
                        ),
                    )
                },
            )
        }
        if (order.credits > 0) client.send(UserCredits(player))
    }

    private fun effectOf(
        base: Item,
        player: Player,
    ): Int = if (player.profile.gender == Gender.F && base.effectFemale > 0) base.effectFemale else base.effectMale

    private fun ownsPermanently(
        player: Player,
        base: Item,
    ): Boolean = player.inventory.effects.ownsPermanent(effectOf(base, player))

    private companion object {
        const val BOT_TYPE = "generic"
        const val PERMANENT_EFFECT_OWNED = "permanent_effect_exists"
        const val FORUM_PARAMS = "forum"

        fun isForum(item: Item) = item.interaction.name == ItemInteraction.GUILD_FURNI && item.params == FORUM_PARAMS
    }
}
