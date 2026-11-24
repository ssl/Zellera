package zel.protocol.generated

data class DailyTasksActiveListOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val long1: Long,
        val string1: String,
        val string2: String,
        val boolean1: Boolean,
        val string3: String,
        val string4: String,
        val int1: Int,
        val int2: Int,
        val byte1: Byte,
        val int3: Int,
        val list1: List<List1Item>,
    ) {
        data class List1Item(
            val short1: Short,
            val string1: String,
            val string2: String,
            val int1: Int,
        )
    }
}

object DailyTasksActiveListOutCodec : OutgoingCodec<DailyTasksActiveListOut> {
    override val name = "DailyTasksActiveListOut"

    override fun read(r: PacketReader): DailyTasksActiveListOut = readDailyTasksActiveListOut(r)

    override fun write(w: PacketWriter, m: DailyTasksActiveListOut) = writeDailyTasksActiveListOut(w, m)

    private fun readDailyTasksActiveListOutList1ItemList1Item(r: PacketReader): DailyTasksActiveListOut.List1Item.List1Item {
        val vShort1 = r.short()
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt1 = r.int()
        return DailyTasksActiveListOut.List1Item.List1Item(vShort1, vString1, vString2, vInt1)
    }

    private fun readDailyTasksActiveListOutList1Item(r: PacketReader): DailyTasksActiveListOut.List1Item {
        val vLong1 = r.long()
        val vString1 = r.string()
        val vString2 = r.string()
        val vBoolean1 = r.boolean()
        val vString3 = r.string()
        val vString4 = r.string()
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vByte1 = r.byte()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(10)) { readDailyTasksActiveListOutList1ItemList1Item(r) }
        return DailyTasksActiveListOut.List1Item(vLong1, vString1, vString2, vBoolean1, vString3, vString4, vInt1, vInt2, vByte1, vInt3, vList1)
    }

    private fun readDailyTasksActiveListOut(r: PacketReader): DailyTasksActiveListOut {
        val vList1 = List(r.countInt(34)) { readDailyTasksActiveListOutList1Item(r) }
        return DailyTasksActiveListOut(vList1)
    }

    private fun writeDailyTasksActiveListOutList1ItemList1Item(w: PacketWriter, m: DailyTasksActiveListOut.List1Item.List1Item) {
        w.short(m.short1)
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int1)
    }

    private fun writeDailyTasksActiveListOutList1Item(w: PacketWriter, m: DailyTasksActiveListOut.List1Item) {
        w.long(m.long1)
        w.string(m.string1)
        w.string(m.string2)
        w.boolean(m.boolean1)
        w.string(m.string3)
        w.string(m.string4)
        w.int(m.int1)
        w.int(m.int2)
        w.byte(m.byte1)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeDailyTasksActiveListOutList1ItemList1Item(w, it) }
    }

    private fun writeDailyTasksActiveListOut(w: PacketWriter, m: DailyTasksActiveListOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeDailyTasksActiveListOutList1Item(w, it) }
    }
}
