package zel.protocol.generated

data class PlayerNftWardrobeOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val string1: String,
        val string2: String,
        val string3: String,
        val string4: String,
        val string5: String,
    )
}

object PlayerNftWardrobeOutCodec : OutgoingCodec<PlayerNftWardrobeOut> {
    override val name = "PlayerNftWardrobeOut"

    override fun read(r: PacketReader): PlayerNftWardrobeOut = readPlayerNftWardrobeOut(r)

    override fun write(w: PacketWriter, m: PlayerNftWardrobeOut) = writePlayerNftWardrobeOut(w, m)

    private fun readPlayerNftWardrobeOutList1Item(r: PacketReader): PlayerNftWardrobeOut.List1Item {
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vString4 = r.string()
        val vString5 = r.string()
        return PlayerNftWardrobeOut.List1Item(vString1, vString2, vString3, vString4, vString5)
    }

    private fun readPlayerNftWardrobeOut(r: PacketReader): PlayerNftWardrobeOut {
        val vList1 = List(r.countInt(10)) { readPlayerNftWardrobeOutList1Item(r) }
        return PlayerNftWardrobeOut(vList1)
    }

    private fun writePlayerNftWardrobeOutList1Item(w: PacketWriter, m: PlayerNftWardrobeOut.List1Item) {
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.string(m.string4)
        w.string(m.string5)
    }

    private fun writePlayerNftWardrobeOut(w: PacketWriter, m: PlayerNftWardrobeOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writePlayerNftWardrobeOutList1Item(w, it) }
    }
}
