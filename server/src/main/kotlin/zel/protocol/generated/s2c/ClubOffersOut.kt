package zel.protocol.generated

data class ClubOffersOut(
    val list1: List<List1Item>,
    val int1: Int,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val boolean1: Boolean,
        val int2: Int,
        val int3: Int,
        val int4: Int,
        val boolean2: Boolean,
        val int5: Int,
        val int6: Int,
        val boolean3: Boolean,
        val int7: Int,
        val int8: Int,
        val int9: Int,
        val int10: Int,
    )
}

object ClubOffersOutCodec : OutgoingCodec<ClubOffersOut> {
    override val name = "ClubOffersOut"

    override fun read(r: PacketReader): ClubOffersOut = readClubOffersOut(r)

    override fun write(w: PacketWriter, m: ClubOffersOut) = writeClubOffersOut(w, m)

    private fun readClubOffersOutList1Item(r: PacketReader): ClubOffersOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vBoolean2 = r.boolean()
        val vInt5 = r.int()
        val vInt6 = r.int()
        val vBoolean3 = r.boolean()
        val vInt7 = r.int()
        val vInt8 = r.int()
        val vInt9 = r.int()
        val vInt10 = r.int()
        return ClubOffersOut.List1Item(vInt1, vString1, vBoolean1, vInt2, vInt3, vInt4, vBoolean2, vInt5, vInt6, vBoolean3, vInt7, vInt8, vInt9, vInt10)
    }

    private fun readClubOffersOut(r: PacketReader): ClubOffersOut {
        val vList1 = List(r.countInt(45)) { readClubOffersOutList1Item(r) }
        val vInt1 = r.int()
        return ClubOffersOut(vList1, vInt1)
    }

    private fun writeClubOffersOutList1Item(w: PacketWriter, m: ClubOffersOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.boolean(m.boolean2)
        w.int(m.int5)
        w.int(m.int6)
        w.boolean(m.boolean3)
        w.int(m.int7)
        w.int(m.int8)
        w.int(m.int9)
        w.int(m.int10)
    }

    private fun writeClubOffersOut(w: PacketWriter, m: ClubOffersOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeClubOffersOutList1Item(w, it) }
        w.int(m.int1)
    }
}
