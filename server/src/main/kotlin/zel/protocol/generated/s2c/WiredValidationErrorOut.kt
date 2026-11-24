package zel.protocol.generated

data class WiredValidationErrorOut(
    val string1: String,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
    )
}

object WiredValidationErrorOutCodec : OutgoingCodec<WiredValidationErrorOut> {
    override val name = "WiredValidationErrorOut"

    override fun read(r: PacketReader): WiredValidationErrorOut = readWiredValidationErrorOut(r)

    override fun write(w: PacketWriter, m: WiredValidationErrorOut) = writeWiredValidationErrorOut(w, m)

    private fun readWiredValidationErrorOutList1Item(r: PacketReader): WiredValidationErrorOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        return WiredValidationErrorOut.List1Item(vString1, vString2)
    }

    private fun readWiredValidationErrorOut(r: PacketReader): WiredValidationErrorOut {
        val vString1 = r.string()
        val vList1 = List(r.countInt(4)) { readWiredValidationErrorOutList1Item(r) }
        return WiredValidationErrorOut(vString1, vList1)
    }

    private fun writeWiredValidationErrorOutList1Item(w: PacketWriter, m: WiredValidationErrorOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writeWiredValidationErrorOut(w: PacketWriter, m: WiredValidationErrorOut) {
        w.string(m.string1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeWiredValidationErrorOutList1Item(w, it) }
    }
}
