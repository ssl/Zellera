package zel.protocol.generated

data class TalentTrackOut(
    val string1: String,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val list1: List<List1Item>,
        val list2: List<String>,
        val list3: List<List3Item>,
    ) {
        data class List1Item(
            val int1: Int,
            val int2: Int,
            val string1: String,
            val int3: Int,
            val int4: Int,
            val int5: Int,
        )

        data class List3Item(
            val string1: String,
            val int1: Int,
        )
    }
}

object TalentTrackOutCodec : OutgoingCodec<TalentTrackOut> {
    override val name = "TalentTrackOut"

    override fun read(r: PacketReader): TalentTrackOut = readTalentTrackOut(r)

    override fun write(w: PacketWriter, m: TalentTrackOut) = writeTalentTrackOut(w, m)

    private fun readTalentTrackOutList1ItemList1Item(r: PacketReader): TalentTrackOut.List1Item.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vString1 = r.string()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vInt5 = r.int()
        return TalentTrackOut.List1Item.List1Item(vInt1, vInt2, vString1, vInt3, vInt4, vInt5)
    }

    private fun readTalentTrackOutList1ItemList3Item(r: PacketReader): TalentTrackOut.List1Item.List3Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        return TalentTrackOut.List1Item.List3Item(vString1, vInt1)
    }

    private fun readTalentTrackOutList1Item(r: PacketReader): TalentTrackOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(22)) { readTalentTrackOutList1ItemList1Item(r) }
        val vList2 = List(r.countInt(2)) { r.string() }
        val vList3 = List(r.countInt(6)) { readTalentTrackOutList1ItemList3Item(r) }
        return TalentTrackOut.List1Item(vInt1, vInt2, vList1, vList2, vList3)
    }

    private fun readTalentTrackOut(r: PacketReader): TalentTrackOut {
        val vString1 = r.string()
        val vList1 = List(r.countInt(20)) { readTalentTrackOutList1Item(r) }
        return TalentTrackOut(vString1, vList1)
    }

    private fun writeTalentTrackOutList1ItemList1Item(w: PacketWriter, m: TalentTrackOut.List1Item.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.string(m.string1)
        w.int(m.int3)
        w.int(m.int4)
        w.int(m.int5)
    }

    private fun writeTalentTrackOutList1ItemList3Item(w: PacketWriter, m: TalentTrackOut.List1Item.List3Item) {
        w.string(m.string1)
        w.int(m.int1)
    }

    private fun writeTalentTrackOutList1Item(w: PacketWriter, m: TalentTrackOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { writeTalentTrackOutList1ItemList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.string(it) }
        w.countInt(m.list3.size)
        m.list3.forEach { writeTalentTrackOutList1ItemList3Item(w, it) }
    }

    private fun writeTalentTrackOut(w: PacketWriter, m: TalentTrackOut) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeTalentTrackOutList1Item(w, it) }
    }
}
