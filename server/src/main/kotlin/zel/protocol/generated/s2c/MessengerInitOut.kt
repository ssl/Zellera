package zel.protocol.generated

data class MessengerInitOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object MessengerInitOutCodec : OutgoingCodec<MessengerInitOut> {
    override val name = "MessengerInitOut"

    override fun read(r: PacketReader): MessengerInitOut = readMessengerInitOut(r)

    override fun write(w: PacketWriter, m: MessengerInitOut) = writeMessengerInitOut(w, m)

    private fun readMessengerInitOutList1Item(r: PacketReader): MessengerInitOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return MessengerInitOut.List1Item(vInt1, vString1)
    }

    private fun readMessengerInitOut(r: PacketReader): MessengerInitOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        val vList1 = List(r.countInt(6)) { readMessengerInitOutList1Item(r) }
        return MessengerInitOut(vInt1, vInt2, vInt3, vList1)
    }

    private fun writeMessengerInitOutList1Item(w: PacketWriter, m: MessengerInitOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeMessengerInitOut(w: PacketWriter, m: MessengerInitOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
        w.countInt(m.list1.size)
        m.list1.forEach { writeMessengerInitOutList1Item(w, it) }
    }
}
