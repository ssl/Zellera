package zel.wired.chest

import zel.protocol.generated.ItemsChestContentsChunkOut
import zel.protocol.generated.ItemsChestContentsUpdatedOut
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.StuffData
import zel.protocol.generated.WiredTradeInitiateOut
import zel.protocol.generated.WiredTradeItemsUpdateOut
import zel.protocol.generated.WiredTransactionSuccessOut
import zel.util.GameZone
import java.time.LocalDate

object TableWire {
    private const val REQUIRE_COINS = 0
    private const val REQUIRE_FURNI = 1
    private const val REQUIRE_CONTRACT = 4
    private const val FLOOR = "S"
    private const val WALL = "I"
    private const val FRAGMENT = 500
    private const val PLAIN_CATEGORY = 1

    fun depositInitiate(chest: Chest): WiredTradeInitiateOut =
        WiredTradeInitiateOut(
            if (chest.isCoins) REQUIRE_COINS else REQUIRE_FURNI,
            "",
            Contract.LAYOUT_GENERIC,
            tail(immediate = false, timeout = 0),
        )

    fun donationInitiate(
        deal: Deal.Negotiated,
        coinsOnly: Boolean,
    ): WiredTradeInitiateOut =
        WiredTradeInitiateOut(
            if (coinsOnly) REQUIRE_COINS else REQUIRE_FURNI,
            deal.contract.payment.receiveText,
            deal.contract.payment.layout,
            tail(immediate = true, timeout = deal.timeoutSeconds),
        )

    fun contractInitiate(deal: Deal.Negotiated): WiredTradeInitiateOut {
        val w = PacketWriter()
        ContractWire.sides(w, deal.contract, givesList = true, getsList = deal.contract.get.isNotEmpty())
        w.int(deal.mode)
        if (deal.mode != 0) w.int(deal.multiplier)
        w.bytes(tail(immediate = true, timeout = deal.timeoutSeconds))
        return WiredTradeInitiateOut(
            REQUIRE_CONTRACT,
            deal.contract.payment.receiveText,
            if (deal.contract.kind == ContractKind.PAYMENT) deal.contract.payment.layout else "",
            w.toByteArray(),
        )
    }

    private fun tail(
        immediate: Boolean,
        timeout: Int,
    ): ByteArray =
        PacketWriter()
            .boolean(immediate)
            .boolean(false)
            .int(timeout)
            .toByteArray()

    fun itemsUpdate(
        table: Table,
        canAccept: Boolean,
        sets: Int,
        today: LocalDate = GameZone.today(),
    ): WiredTradeItemsUpdateOut {
        val w = PacketWriter()
        w.int(table.offered.size)
        table.offered.values.forEach { offered(w, it, today) }
        w.int(table.offered.size).int(0)
        val side = (table.deal as? Deal.Negotiated)?.chestSide.orEmpty()
        w.int(0).int(side.size)
        side.forEach { stored(w, it, today) }
        w.int(side.size).int(0)
        w.boolean(canAccept).int(sets)
        return WiredTradeItemsUpdateOut(table.playerId, w.toByteArray())
    }

    private fun offered(
        w: PacketWriter,
        item: OfferedFurni,
        today: LocalDate,
    ) {
        w
            .int(item.id)
            .string(if (item.wall) WALL else FLOOR)
            .int(item.id)
            .int(item.spriteId)
            .int(item.category)
        w.boolean(false)
        StuffData.write(w, item.stuff)
        date(w, today)
        if (!item.wall) w.int(item.extra)
    }

    private fun stored(
        w: PacketWriter,
        item: StoredFurni,
        today: LocalDate,
    ) {
        w
            .int(item.slotId)
            .string(if (item.isWall) WALL else FLOOR)
            .int(item.slotId)
            .int(item.spriteId)
        w.int(PLAIN_CATEGORY).boolean(true)
        StuffData.write(w, plain(item))
        date(w, today)
        if (!item.isWall) w.int(item.extra)
    }

    private fun date(
        w: PacketWriter,
        day: LocalDate,
    ) {
        w.int(day.dayOfMonth).int(day.monthValue).int(day.year)
    }

    private fun plain(item: StoredFurni) = StuffData(0, StuffData.Value.Legacy(item.state))

    fun chunks(chest: Chest): List<ItemsChestContentsChunkOut> {
        val pages = chest.furni.chunked(FRAGMENT).ifEmpty { listOf(emptyList()) }
        return pages.mapIndexed { index, page ->
            val w = PacketWriter().int(page.size)
            page.forEach { entry(w, it) }
            ItemsChestContentsChunkOut(chest.id, pages.size, index, w.toByteArray())
        }
    }

    fun contentsUpdated(
        chest: Chest,
        removed: List<Int>,
        added: List<StoredFurni>,
    ): ItemsChestContentsUpdatedOut {
        val w = PacketWriter().int(added.size)
        added.forEach { entry(w, it) }
        return ItemsChestContentsUpdatedOut(chest.id, removed, w.toByteArray())
    }

    private fun entry(
        w: PacketWriter,
        item: StoredFurni,
    ) {
        w
            .int(item.slotId)
            .int(0)
            .long(item.transactionId)
            .boolean(item.isWall)
            .int(item.spriteId)
        w.string(item.posterCode).boolean(item.groupable).int(0)
        StuffData.write(w, plain(item))
        if (!item.isWall) w.int(item.extra)
    }

    fun success(kind: Int): WiredTransactionSuccessOut = WiredTransactionSuccessOut(kind, ByteArray(0))

    fun rewardSuccess(
        rewards: List<Term>,
        text: String,
        popup: Boolean,
    ): WiredTransactionSuccessOut {
        val w = PacketWriter()
        ContractWire.rule(w, rewards)
        w.string(text).boolean(popup)
        return WiredTransactionSuccessOut(SUCCESS_REWARD, w.toByteArray())
    }

    const val SUCCESS_DEPOSIT = 0
    const val SUCCESS_WITHDRAW = 1
    const val SUCCESS_REWARD = 2
    const val SUCCESS_PAYMENT = 3
    const val SUCCESS_TRADE = 4
}
