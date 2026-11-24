package zel.protocol.generated

data class SlideObjectBundleOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val int4: Int,
    val list1: List<List1Item>,
    val int5: Int,
    val rest: ByteArray,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
    )
}

object SlideObjectBundleOutCodec : OutgoingCodec<SlideObjectBundleOut> {
    override val name = "SlideObjectBundleOut"

    override fun read(r: PacketReader): SlideObjectBundleOut = readSlideObjectBundleOut(r)

    override fun write(w: PacketWriter, m: SlideObjectBundleOut) = writeSlideObjectBundleOut(w, m)

    private fun readSlideObjectBundleOutList1Item(r: PacketReader): SlideObjectBundleOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return SlideObjectBundleOut.List1Item(vInt1, vString1, vString2)
    }

    private fun readSlideObjectBundleOut(r: PacketReader): SlideObjectBundleOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vInt4 = r.int()
        val vList1 = List(r.countInt(8)) { readSlideObjectBundleOutList1Item(r) }
        val vInt5 = r.int()
        val vRest = r.rest()
        return SlideObjectBundleOut(vInt1, vInt2, vInt3, vInt4, vList1, vInt5, vRest)
    }

    private fun writeSlideObjectBundleOutList1Item(w: PacketWriter, m: SlideObjectBundleOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeSlideObjectBundleOut(w: PacketWriter, m: SlideObjectBundleOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.int(m.int4)
        w.countInt(m.list1.size)
        m.list1.forEach { writeSlideObjectBundleOutList1Item(w, it) }
        w.int(m.int5)
        w.bytes(m.rest)
    }
}
