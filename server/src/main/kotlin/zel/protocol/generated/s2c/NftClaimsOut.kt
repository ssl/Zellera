package zel.protocol.generated

data class NftClaimsOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val long1: Long,
        val long2: Long,
        val long3: Long,
        val long4: Long,
        val string2: String,
        val string3: String,
        val string4: String,
        val short1: Short,
        val string5: String,
        val int4: Int,
        val string6: String,
        val list1: List<Int>,
        val string7: String,
        val string8: String,
        val string9: String,
        val string10: String,
    )
}

object NftClaimsOutCodec : OutgoingCodec<NftClaimsOut> {
    override val name = "NftClaimsOut"

    override fun read(r: PacketReader): NftClaimsOut = readNftClaimsOut(r)

    override fun write(w: PacketWriter, m: NftClaimsOut) = writeNftClaimsOut(w, m)

    private fun readNftClaimsOutList1Item(r: PacketReader): NftClaimsOut.List1Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vLong1 = r.long()
        val vLong2 = r.long()
        val vLong3 = r.long()
        val vLong4 = r.long()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        val vShort1 = r.short()
        val vString5 = r.string()
        val vInt4 = r.int()
        val vString6 = r.string()
        val vList1 = List(r.countInt(4)) { r.int() }
        val vString7 = r.string()
        val vString8 = r.string()
        val vString9 = r.string()
        val vString10 = r.string()
        return NftClaimsOut.List1Item(vString1, vInt1, vInt2, vInt3, vLong1, vLong2, vLong3, vLong4, vString2, vString3, vString4, vShort1, vString5, vInt4, vString6, vList1, vString7, vString8, vString9, vString10)
    }

    private fun readNftClaimsOut(r: PacketReader): NftClaimsOut {
        val vList1 = List(r.countInt(74)) { readNftClaimsOutList1Item(r) }
        return NftClaimsOut(vList1)
    }

    private fun writeNftClaimsOutList1Item(w: PacketWriter, m: NftClaimsOut.List1Item) {
        w.string(m.string1)
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.long(m.long1)
        w.long(m.long2)
        w.long(m.long3)
        w.long(m.long4)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
        w.short(m.short1)
        w.string(m.string5)
        w.int(m.int4)
        w.string(m.string6)
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.string(m.string7)
        w.string(m.string8)
        w.string(m.string9)
        w.string(m.string10)
    }

    private fun writeNftClaimsOut(w: PacketWriter, m: NftClaimsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeNftClaimsOutList1Item(w, it) }
    }
}
