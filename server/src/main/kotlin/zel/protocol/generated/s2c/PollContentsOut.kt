package zel.protocol.generated

data class PollContentsOut(
    val int1: Int,
    val string1: String,
    val string2: String,
    val list1: List<List1Item>,
    val boolean1: Boolean,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
        val string1: String,
        val int4: Int,
        val int5: Int,
        val list1: List<List1Item>,
        val list2: List<List2Item>,
    ) {
        data class List1Item(
            val string1: String,
            val string2: String,
            val int1: Int,
        )

        data class List2Item(
            val int1: Int,
            val int2: Int,
            val int3: Int,
            val string1: String,
            val int4: Int,
            val int5: Int,
            val list1: List<List1Item>,
        ) {
            data class List1Item(
                val string1: String,
                val string2: String,
                val int1: Int,
            )
        }
    }
}

object PollContentsOutCodec : OutgoingCodec<PollContentsOut> {
    override val name = "PollContentsOut"

    override fun read(r: PacketReader): PollContentsOut = readPollContentsOut(r)

    override fun write(w: PacketWriter, m: PollContentsOut) = writePollContentsOut(w, m)

    private fun readPollContentsOutList1ItemList1Item(r: PacketReader): PollContentsOut.List1Item.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt1 = r.int()
        return PollContentsOut.List1Item.List1Item(vString1, vString2, vInt1)
    }

    private fun readPollContentsOutList1ItemList2ItemList1Item(r: PacketReader): PollContentsOut.List1Item.List2Item.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vInt1 = r.int()
        return PollContentsOut.List1Item.List2Item.List1Item(vString1, vString2, vInt1)
    }

    private fun readPollContentsOutList1ItemList2Item(r: PacketReader): PollContentsOut.List1Item.List2Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(8)) { readPollContentsOutList1ItemList2ItemList1Item(r) }
        return PollContentsOut.List1Item.List2Item(vInt1, vInt2, vInt3, vString1, vInt4, vInt5, vList1)
    }

    private fun readPollContentsOutList1Item(r: PacketReader): PollContentsOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vString1 = r.string()
        val vInt4 = r.int()
        val vInt5 = r.int()
        val vList1 = List(r.countInt(8)) { readPollContentsOutList1ItemList1Item(r) }
        val vList2 = List(r.countInt(26)) { readPollContentsOutList1ItemList2Item(r) }
        return PollContentsOut.List1Item(vInt1, vInt2, vInt3, vString1, vInt4, vInt5, vList1, vList2)
    }

    private fun readPollContentsOut(r: PacketReader): PollContentsOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vList1 = List(r.countInt(30)) { readPollContentsOutList1Item(r) }
        val vBoolean1 = r.boolean()
        return PollContentsOut(vInt1, vString1, vString2, vList1, vBoolean1)
    }

    private fun writePollContentsOutList1ItemList1Item(w: PacketWriter, m: PollContentsOut.List1Item.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int1)
    }

    private fun writePollContentsOutList1ItemList2ItemList1Item(w: PacketWriter, m: PollContentsOut.List1Item.List2Item.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.int(m.int1)
    }

    private fun writePollContentsOutList1ItemList2Item(w: PacketWriter, m: PollContentsOut.List1Item.List2Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.int(m.int4)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writePollContentsOutList1ItemList2ItemList1Item(w, it) }
    }

    private fun writePollContentsOutList1Item(w: PacketWriter, m: PollContentsOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.string(m.string1)
        w.int(m.int4)
        w.int(m.int5)
        w.countInt(m.list1.size)
        m.list1.forEach { writePollContentsOutList1ItemList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writePollContentsOutList1ItemList2Item(w, it) }
    }

    private fun writePollContentsOut(w: PacketWriter, m: PollContentsOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.countInt(m.list1.size)
        m.list1.forEach { writePollContentsOutList1Item(w, it) }
        w.boolean(m.boolean1)
    }
}
