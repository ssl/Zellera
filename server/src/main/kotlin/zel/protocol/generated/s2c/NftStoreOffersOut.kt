package zel.protocol.generated

data class NftStoreOffersOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val int1: Int,
        val boolean1: Boolean,
        val boolean2: Boolean,
        val int2: Int,
        val int3: Int,
        val short1: Short,
        val string2: String,
        val int4: Int,
        val string3: String,
        val list1: List<Int>,
        val string4: String,
        val string5: String,
    )
}

object NftStoreOffersOutCodec : OutgoingCodec<NftStoreOffersOut> {
    override val name = "NftStoreOffersOut"

    override fun read(r: PacketReader): NftStoreOffersOut = readNftStoreOffersOut(r)

    override fun write(w: PacketWriter, m: NftStoreOffersOut) = writeNftStoreOffersOut(w, m)

    private fun readNftStoreOffersOutList1Item(r: PacketReader): NftStoreOffersOut.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vShort1 = r.short()
        val vString2 = r.string()
        val vInt4 = r.int()
        val vString3 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vString4 = r.string()
        val vString5 = r.string()
        return NftStoreOffersOut.List1Item(vString1, vInt1, vBoolean1, vBoolean2, vInt2, vInt3, vShort1, vString2, vInt4, vString3, vList1, vString4, vString5)
    }

    private fun readNftStoreOffersOut(r: PacketReader): NftStoreOffersOut {
        val vList1 = List(r.countInt(34)) { readNftStoreOffersOutList1Item(r) }
        return NftStoreOffersOut(vList1)
    }

    private fun writeNftStoreOffersOutList1Item(w: PacketWriter, m: NftStoreOffersOut.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.int(m.int2)
        w.int(m.int3)
        w.short(m.short1)
        w.string(m.string2)
        w.int(m.int4)
        w.string(m.string3)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.string(m.string4)
        w.string(m.string5)
    }

    private fun writeNftStoreOffersOut(w: PacketWriter, m: NftStoreOffersOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeNftStoreOffersOutList1Item(w, it) }
    }
}
