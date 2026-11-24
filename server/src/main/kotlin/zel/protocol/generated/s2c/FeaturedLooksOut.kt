package zel.protocol.generated

data class FeaturedLooksOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
    )
}

object FeaturedLooksOutCodec : OutgoingCodec<FeaturedLooksOut> {
    override val name = "FeaturedLooksOut"

    override fun read(r: PacketReader): FeaturedLooksOut = readFeaturedLooksOut(r)

    override fun write(w: PacketWriter, m: FeaturedLooksOut) = writeFeaturedLooksOut(w, m)

    private fun readFeaturedLooksOutList1Item(r: PacketReader): FeaturedLooksOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        return FeaturedLooksOut.List1Item(vString1, vString2)
    }

    private fun readFeaturedLooksOut(r: PacketReader): FeaturedLooksOut {
        val vList1 = List(r.countInt(4)) { readFeaturedLooksOutList1Item(r) }
        return FeaturedLooksOut(vList1)
    }

    private fun writeFeaturedLooksOutList1Item(w: PacketWriter, m: FeaturedLooksOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeFeaturedLooksOut(w: PacketWriter, m: FeaturedLooksOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeFeaturedLooksOutList1Item(w, it) }
    }
}
