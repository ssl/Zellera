package zel.protocol.generated

data class MyCfhReportStatusOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val long1: Long,
        val long2: Long,
        val string1: String,
        val int1: Int,
        val string2: String,
        val long3: Long,
        val boolean1: Boolean,
        val boolean2: Boolean,
        val byte1: Byte,
        val long4: Long,
        val long5: Long,
    )
}

object MyCfhReportStatusOutCodec : OutgoingCodec<MyCfhReportStatusOut> {
    override val name = "MyCfhReportStatusOut"

    override fun read(r: PacketReader): MyCfhReportStatusOut = readMyCfhReportStatusOut(r)

    override fun write(w: PacketWriter, m: MyCfhReportStatusOut) = writeMyCfhReportStatusOut(w, m)

    private fun readMyCfhReportStatusOutList1Item(r: PacketReader): MyCfhReportStatusOut.List1Item {
        val vLong1 = r.long()
        val vLong2 = r.long()
        val vString1 = r.string()
        val vInt1 = r.int()
        val vString2 = r.string()
        val vLong3 = r.long()
        val vBoolean1 = r.boolean()
        val vBoolean2 = r.boolean()
        val vByte1 = r.byte()
        val vLong4 = r.long()
        val vLong5 = r.long()
        return MyCfhReportStatusOut.List1Item(vLong1, vLong2, vString1, vInt1, vString2, vLong3, vBoolean1, vBoolean2, vByte1, vLong4, vLong5)
    }

    private fun readMyCfhReportStatusOut(r: PacketReader): MyCfhReportStatusOut {
        val vList1 = List(r.countInt(51)) { readMyCfhReportStatusOutList1Item(r) }
        return MyCfhReportStatusOut(vList1)
    }

    private fun writeMyCfhReportStatusOutList1Item(w: PacketWriter, m: MyCfhReportStatusOut.List1Item) {
        w.long(m.long1)
        w.long(m.long2)
        w.string(m.string1)
        w.int(m.int1)
        w.string(m.string2)
        w.long(m.long3)
        w.boolean(m.boolean1)
        w.boolean(m.boolean2)
        w.byte(m.byte1)
        w.long(m.long4)
        w.long(m.long5)
    }

    private fun writeMyCfhReportStatusOut(w: PacketWriter, m: MyCfhReportStatusOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeMyCfhReportStatusOutList1Item(w, it) }
    }
}
