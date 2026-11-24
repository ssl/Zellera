package zel.protocol.generated

data class NewPlayerExperienceGiftOfferOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val string1: String,
            val list1: List<List1Item>,
        ) {
            data class List1Item(
                val string1: String,
                val string2: String,
            )
        }
    }
}

object NewPlayerExperienceGiftOfferOutCodec : OutgoingCodec<NewPlayerExperienceGiftOfferOut> {
    override val name = "NewPlayerExperienceGiftOfferOut"

    override fun read(r: PacketReader): NewPlayerExperienceGiftOfferOut = readNewPlayerExperienceGiftOfferOut(r)

    override fun write(w: PacketWriter, m: NewPlayerExperienceGiftOfferOut) = writeNewPlayerExperienceGiftOfferOut(w, m)

    private fun readNewPlayerExperienceGiftOfferOutList1ItemList1ItemList1Item(r: PacketReader): NewPlayerExperienceGiftOfferOut.List1Item.List1Item.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        return NewPlayerExperienceGiftOfferOut.List1Item.List1Item.List1Item(vString1, vString2)
    }

    private fun readNewPlayerExperienceGiftOfferOutList1ItemList1Item(r: PacketReader): NewPlayerExperienceGiftOfferOut.List1Item.List1Item {
        val vString1 = r.string()
        val vList1 = List(r.countInt(4)) { readNewPlayerExperienceGiftOfferOutList1ItemList1ItemList1Item(r) }
        return NewPlayerExperienceGiftOfferOut.List1Item.List1Item(vString1, vList1)
    }

    private fun readNewPlayerExperienceGiftOfferOutList1Item(r: PacketReader): NewPlayerExperienceGiftOfferOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(6)) { readNewPlayerExperienceGiftOfferOutList1ItemList1Item(r) }
        return NewPlayerExperienceGiftOfferOut.List1Item(vInt1, vInt2, vList1)
    }

    private fun readNewPlayerExperienceGiftOfferOut(r: PacketReader): NewPlayerExperienceGiftOfferOut {
        val vList1 = List(r.countInt(12)) { readNewPlayerExperienceGiftOfferOutList1Item(r) }
        return NewPlayerExperienceGiftOfferOut(vList1)
    }

    private fun writeNewPlayerExperienceGiftOfferOutList1ItemList1ItemList1Item(w: PacketWriter, m: NewPlayerExperienceGiftOfferOut.List1Item.List1Item.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeNewPlayerExperienceGiftOfferOutList1ItemList1Item(w: PacketWriter, m: NewPlayerExperienceGiftOfferOut.List1Item.List1Item) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeNewPlayerExperienceGiftOfferOutList1ItemList1ItemList1Item(w, it) }
    }

    private fun writeNewPlayerExperienceGiftOfferOutList1Item(w: PacketWriter, m: NewPlayerExperienceGiftOfferOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeNewPlayerExperienceGiftOfferOutList1ItemList1Item(w, it) }
    }

    private fun writeNewPlayerExperienceGiftOfferOut(w: PacketWriter, m: NewPlayerExperienceGiftOfferOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeNewPlayerExperienceGiftOfferOutList1Item(w, it) }
    }
}
