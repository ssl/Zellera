package zel.protocol.generated

data class PlayerClassificationOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
    )
}

object PlayerClassificationOutCodec : OutgoingCodec<PlayerClassificationOut> {
    override val name = "PlayerClassificationOut"

    override fun read(r: PacketReader): PlayerClassificationOut = readPlayerClassificationOut(r)

    override fun write(w: PacketWriter, m: PlayerClassificationOut) = writePlayerClassificationOut(w, m)

    private fun readPlayerClassificationOutList1Item(r: PacketReader): PlayerClassificationOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        return PlayerClassificationOut.List1Item(vInt1, vString1, vString2)
    }

    private fun readPlayerClassificationOut(r: PacketReader): PlayerClassificationOut {
        val vList1 = List(r.countInt(8)) { readPlayerClassificationOutList1Item(r) }
        return PlayerClassificationOut(vList1)
    }

    private fun writePlayerClassificationOutList1Item(w: PacketWriter, m: PlayerClassificationOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
    }

    private fun writePlayerClassificationOut(w: PacketWriter, m: PlayerClassificationOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writePlayerClassificationOutList1Item(w, it) }
    }
}
