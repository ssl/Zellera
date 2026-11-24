package zel.protocol.trading

import zel.game.items.CreditFurni
import zel.game.items.OwnedFurni
import zel.game.items.ProductKind
import zel.protocol.generated.PacketWriter
import zel.protocol.generated.StuffData
import zel.protocol.generated.TradingItemListOut
import zel.protocol.generated.TradingItemListOutCodec
import zel.protocol.outgoing.inventory.InventoryFurni

internal object TradeItemsLayout {
    val codec = TradingItemListOutCodec

    fun offers(offers: List<Pair<Int, List<OwnedFurni>>>): TradingItemListOut {
        val rest = PacketWriter()
        offers.forEachIndexed { index, (userId, items) ->
            if (index > 0) rest.int(userId)
            writeSide(rest, items)
        }
        return TradingItemListOut(offers.first().first, rest.toByteArray())
    }

    private fun writeSide(
        w: PacketWriter,
        items: List<OwnedFurni>,
    ) {
        w.int(items.size)
        items.forEach { writeItem(w, it) }
        w.int(items.size)
        w.int(items.sumOf { creditsOf(it) })
    }

    private fun writeItem(
        w: PacketWriter,
        item: OwnedFurni,
    ) {
        val base = item.definition
        w.int(item.id)
        w.string(base.type.code)
        w.int(item.id)
        w.int(base.spriteId)
        w.int(InventoryFurni.entry(item).category)
        w.boolean(base.stacksInInventory && !item.isLimited)
        StuffData.write(w, base.services.inventoryStuff.of(item))
        w.int(0)
        w.int(0)
        w.int(0)
        if (base.type == ProductKind.FLOOR_FURNI) w.int(0)
    }

    private fun creditsOf(item: OwnedFurni): Int {
        val counted =
            item.definition.services.config
                .getBoolean("trading.credit_furni.is_counted")
        return if (counted) CreditFurni.credits(item.definition) else 0
    }
}
