package zel.game.rewardtracks

import org.slf4j.LoggerFactory
import zel.game.bots.BotLook
import zel.game.bots.Bots
import zel.game.catalog.Catalog
import zel.game.icons.IconRejected
import zel.game.icons.IconService
import zel.game.items.Item
import zel.game.items.Items
import zel.game.items.ProductKind
import zel.game.pets.PetsModule
import zel.game.players.Player
import zel.game.quests.QuestReward
import zel.game.quests.QuestRewards
import zel.protocol.outgoing.inventory.AddBot
import zel.protocol.outgoing.inventory.AddRoomItem
import zel.protocol.outgoing.inventory.AddRoomItem.UnseenCategory
import zel.protocol.outgoing.inventory.InventoryUpdateItem

/** A row that cannot be read into a [Payout] pays nothing and logs why. */
class PrizeHandout(
    private val items: Items,
    private val pets: PetsModule,
    private val bots: Bots,
    private val catalog: Catalog,
) {
    private val questRewards = QuestRewards(items)

    @Suppress("MagicNumber") // the codes of the prize table
    enum class PrizeKind(
        val code: Int,
    ) {
        WALL_FURNI(0),
        FLOOR_FURNI(1),
        BADGE(4),
        BOT(6),
        ACTIVITY_POINTS(8),
        PET(10),
        ICON(12),
        ;

        companion object {
            fun fromCode(code: Int): PrizeKind? = entries.firstOrNull { it.code == code }
        }
    }

    sealed interface Payout {
        /** Listed in the inventory [listings] times; a pet package is listed twice. */
        data class Furni(
            val item: Item,
            val copies: Int,
            val listings: Int = 1,
        ) : Payout

        data class Badge(
            val code: String,
        ) : Payout

        data class Points(
            val pointsType: Int,
            val amount: Int,
        ) : Payout

        data class Bot(
            val kind: String,
            val look: BotLook,
        ) : Payout

        data class Icon(
            val iconId: Int,
        ) : Payout

        data class Unpayable(
            val why: String,
        ) : Payout
    }

    /** False only for a pet prize without a package in `pet_packages`: that claim fails and stays claimable. */
    fun canPay(prize: RewardTrackDefinition.Prize): Boolean =
        PrizeKind.fromCode(prize.productType) != PrizeKind.PET || petPackage(prize) != null

    fun handOut(
        player: Player,
        prize: RewardTrackDefinition.Prize,
    ) {
        when (val payout = read(prize)) {
            is Payout.Furni -> giveFurni(player, payout)
            is Payout.Badge -> questRewards.badge(player, payout.code)
            is Payout.Points -> givePoints(player, payout)
            is Payout.Bot -> giveBot(player, payout)
            is Payout.Icon -> giveIcon(player, payout)
            is Payout.Unpayable -> log.warn("Prize {} pays nothing: {}", prize.id, payout.why)
        }
    }

    internal fun read(prize: RewardTrackDefinition.Prize): Payout =
        when (PrizeKind.fromCode(prize.productType)) {
            PrizeKind.FLOOR_FURNI -> furniBySprite(prize, ProductKind.FLOOR_FURNI)
            PrizeKind.WALL_FURNI -> furniBySprite(prize, ProductKind.WALL_FURNI)
            PrizeKind.BADGE -> Payout.Badge(prize.productCode)
            PrizeKind.ACTIVITY_POINTS -> numberedCode(prize) { Payout.Points(it, prize.amount) }
            PrizeKind.BOT -> Payout.Bot(botKind(prize.productCode), botLook(botPreset(), prize.extra))
            PrizeKind.ICON -> numberedCode(prize, Payout::Icon)
            PrizeKind.PET -> petPackagePayout(prize)
            null -> Payout.Unpayable("product type ${prize.productType} is unknown")
        }

    private fun numberedCode(
        prize: RewardTrackDefinition.Prize,
        payout: (Int) -> Payout,
    ): Payout =
        prize.productCode.toIntOrNull()?.let(payout)
            ?: Payout.Unpayable("product code '${prize.productCode}' is not a number")

    private fun furniBySprite(
        prize: RewardTrackDefinition.Prize,
        type: ProductKind,
    ): Payout {
        val sprite = prize.productCode.toIntOrNull()
        val item = items.getItems().values.firstOrNull { it.type == type && it.spriteId == sprite }
        return item?.let { Payout.Furni(it, maxOf(1, prize.amount)) }
            ?: Payout.Unpayable("no ${type.name.lowercase()} furni has sprite ${prize.productCode}")
    }

    /** Matched on the figure in `extra` (`type palette colour`). */
    private fun petPackage(prize: RewardTrackDefinition.Prize): Item? {
        val figure = prize.extra.trim().split(' ')
        val type = figure.getOrNull(0)?.toIntOrNull() ?: return null
        val palette = figure.getOrNull(1)?.toIntOrNull() ?: return null
        val colour = figure.getOrNull(2) ?: return null
        return pets.catalog.packageFor(type, palette, colour)?.let { items.getItem(it.itemName) }
    }

    private fun petPackagePayout(prize: RewardTrackDefinition.Prize): Payout =
        petPackage(prize)?.let { Payout.Furni(it, maxOf(1, prize.amount), listings = 2) }
            ?: Payout.Unpayable("pet '${prize.extra}' has no package in pet_packages")

    private fun giveFurni(
        player: Player,
        payout: Payout.Furni,
    ) {
        val ownerId = player.profile.id
        val made = (1..payout.copies).mapNotNull { items.newFurni(ownerId, payout.item, 0, 0, "") }
        if (made.isEmpty()) return
        player.inventory.furni.putAll(made)
        player.client?.let { client ->
            client.send(AddRoomItem(made))
            repeat(payout.listings) { client.send(InventoryUpdateItem(made)) }
        }
    }

    private fun givePoints(
        player: Player,
        payout: Payout.Points,
    ) = questRewards.grant(player, QuestReward.Currency(payout.pointsType, payout.amount), LEDGER_REASON)

    private fun giveBot(
        player: Player,
        payout: Payout.Bot,
    ) {
        val bot = bots.create(payout.look, payout.kind, player.profile.id) ?: return
        player.inventory.bots.put(bot)
        player.client?.let {
            it.send(AddBot(bot))
            it.send(AddRoomItem(bot.id, UnseenCategory.BOT))
        }
    }

    private fun giveIcon(
        player: Player,
        payout: Payout.Icon,
    ) {
        val service = player.iconService
        try {
            val outcome = service.grant(player.profile.id, IconService.allUnlocked(player), payout.iconId)
            IconService.announceStates(player, outcome)
            service.countCollected(player, outcome)
        } catch (refused: IconRejected) {
            log.warn("Icon prize {} refused with code {}", payout.iconId, refused.code)
        }
    }

    /** The catalog's oldest bot offer names the standard bot. */
    private fun botPreset(): String =
        catalog.contents.pages.values
            .asSequence()
            .flatMap { page -> page.offers.values.asSequence() }
            .filter { offer -> offer.products.any(Item::isBot) }
            .minByOrNull { it.id }
            ?.presetState
            .orEmpty()

    companion object {
        private val log = LoggerFactory.getLogger(PrizeHandout::class.java)
        private const val LEDGER_REASON = "reward_track"
        private const val STANDARD_BOT_NAME = "Robbie"

        fun botKind(productCode: String): String = productCode.lowercase()

        fun botLook(
            preset: String,
            figure: String,
        ): BotLook {
            val look = BotLook(STANDARD_BOT_NAME).withPreset(preset)
            return if (figure.isBlank()) look else look.copy(figure = figure)
        }
    }
}
