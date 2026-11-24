package zel.protocol.generated

data class FurniListAddOrUpdateOut(
    val items: List<ItemsItem>,
) {
    data class ItemsItem(
        val stripId: Int,
        val type: String,
        val itemId: Int,
        val spriteId: Int,
        val category: Int,
        val stuffData: StuffData,
        val recyclable: Boolean,
        val tradable: Boolean,
        val groupable: Boolean,
        val sellable: Boolean,
        val secondsToExpiration: Int,
        val rentPeriodStarted: Boolean,
        val roomId: Int,
        val floorExtras: FloorExtras,
    ) {
        sealed interface FloorExtras {
            data class Then(
                val slotId: String,
                val extra: Int,
            ) : FloorExtras
            data object Else : FloorExtras
        }
    }
}

object FurniListAddOrUpdateOutCodec : OutgoingCodec<FurniListAddOrUpdateOut> {
    override val name = "FurniListAddOrUpdateOut"

    override fun read(r: PacketReader): FurniListAddOrUpdateOut = readFurniListAddOrUpdateOut(r)

    override fun write(w: PacketWriter, m: FurniListAddOrUpdateOut) = writeFurniListAddOrUpdateOut(w, m)

    private fun readFurniListAddOrUpdateOutItemsItemFloorExtrasThen(r: PacketReader): FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Then {
        val vSlotId = r.string()
        val vExtra = r.int()
        return FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Then(vSlotId, vExtra)
    }

    private fun readFurniListAddOrUpdateOutItemsItem(r: PacketReader): FurniListAddOrUpdateOut.ItemsItem {
        val vStripId = r.int()
        val vType = r.string()
        val vItemId = r.int()
        val vSpriteId = r.int()
        val vCategory = r.int()
        val vStuffData = StuffData.read(r)
        val vRecyclable = r.boolean()
        val vTradable = r.boolean()
        val vGroupable = r.boolean()
        val vSellable = r.boolean()
        val vSecondsToExpiration = r.int()
        val vRentPeriodStarted = r.boolean()
        val vRoomId = r.int()
        val vFloorExtras = if (vType == "S") readFurniListAddOrUpdateOutItemsItemFloorExtrasThen(r) else FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Else
        return FurniListAddOrUpdateOut.ItemsItem(vStripId, vType, vItemId, vSpriteId, vCategory, vStuffData, vRecyclable, vTradable, vGroupable, vSellable, vSecondsToExpiration, vRentPeriodStarted, vRoomId, vFloorExtras)
    }

    private fun readFurniListAddOrUpdateOut(r: PacketReader): FurniListAddOrUpdateOut {
        val vItems = List(r.countInt(35)) { readFurniListAddOrUpdateOutItemsItem(r) }
        return FurniListAddOrUpdateOut(vItems)
    }

    private fun writeFurniListAddOrUpdateOutItemsItemFloorExtrasThen(w: PacketWriter, m: FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Then) {
        w.string(m.slotId)
        w.int(m.extra)
    }

    private fun writeFurniListAddOrUpdateOutItemsItem(w: PacketWriter, m: FurniListAddOrUpdateOut.ItemsItem) {
        w.int(m.stripId)
        w.string(m.type)
        w.int(m.itemId)
        w.int(m.spriteId)
        w.int(m.category)
        StuffData.write(w, m.stuffData)
        w.boolean(m.recyclable)
        w.boolean(m.tradable)
        w.boolean(m.groupable)
        w.boolean(m.sellable)
        w.int(m.secondsToExpiration)
        w.boolean(m.rentPeriodStarted)
        w.int(m.roomId)
        when (val b = m.floorExtras) {
            is FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Then -> writeFurniListAddOrUpdateOutItemsItemFloorExtrasThen(w, b)
            is FurniListAddOrUpdateOut.ItemsItem.FloorExtras.Else -> {}
        }
    }

    private fun writeFurniListAddOrUpdateOut(w: PacketWriter, m: FurniListAddOrUpdateOut) {
        w.countInt(m.items.size)
        m.items.forEach { writeFurniListAddOrUpdateOutItemsItem(w, it) }
    }
}
