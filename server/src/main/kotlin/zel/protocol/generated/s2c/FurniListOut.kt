package zel.protocol.generated

data class FurniListOut(
    val totalFragments: Int,
    val fragmentNo: Int,
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

object FurniListOutCodec : OutgoingCodec<FurniListOut> {
    override val name = "FurniListOut"

    override fun read(r: PacketReader): FurniListOut = readFurniListOut(r)

    override fun write(w: PacketWriter, m: FurniListOut) = writeFurniListOut(w, m)

    private fun readFurniListOutItemsItemFloorExtrasThen(r: PacketReader): FurniListOut.ItemsItem.FloorExtras.Then {
        val vSlotId = r.string()
        val vExtra = r.int()
        return FurniListOut.ItemsItem.FloorExtras.Then(vSlotId, vExtra)
    }

    private fun readFurniListOutItemsItem(r: PacketReader): FurniListOut.ItemsItem {
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
        val vFloorExtras = if (vType == "S") readFurniListOutItemsItemFloorExtrasThen(r) else FurniListOut.ItemsItem.FloorExtras.Else
        return FurniListOut.ItemsItem(vStripId, vType, vItemId, vSpriteId, vCategory, vStuffData, vRecyclable, vTradable, vGroupable, vSellable, vSecondsToExpiration, vRentPeriodStarted, vRoomId, vFloorExtras)
    }

    private fun readFurniListOut(r: PacketReader): FurniListOut {
        val vTotalFragments = r.int()
        val vFragmentNo = r.int()
        val vItems = List(r.countInt(35)) { readFurniListOutItemsItem(r) }
        return FurniListOut(vTotalFragments, vFragmentNo, vItems)
    }

    private fun writeFurniListOutItemsItemFloorExtrasThen(w: PacketWriter, m: FurniListOut.ItemsItem.FloorExtras.Then) {
        w.string(m.slotId)
        w.int(m.extra)
    }

    private fun writeFurniListOutItemsItem(w: PacketWriter, m: FurniListOut.ItemsItem) {
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
            is FurniListOut.ItemsItem.FloorExtras.Then -> writeFurniListOutItemsItemFloorExtrasThen(w, b)
            is FurniListOut.ItemsItem.FloorExtras.Else -> {}
        }
    }

    private fun writeFurniListOut(w: PacketWriter, m: FurniListOut) {
        w.int(m.totalFragments)
        w.int(m.fragmentNo)
        w.countInt(m.items.size)
        m.items.forEach { writeFurniListOutItemsItem(w, it) }
    }
}
