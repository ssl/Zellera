package zel.protocol.generated

data class TraxSongInfoOut(
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val string2: String,
        val string3: String,
        val int2: Int,
        val string4: String,
    )
}

object TraxSongInfoOutCodec : OutgoingCodec<TraxSongInfoOut> {
    override val name = "TraxSongInfoOut"

    override fun read(r: PacketReader): TraxSongInfoOut = readTraxSongInfoOut(r)

    override fun write(w: PacketWriter, m: TraxSongInfoOut) = writeTraxSongInfoOut(w, m)

    private fun readTraxSongInfoOutList1Item(r: PacketReader): TraxSongInfoOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vString2 = r.string()
        val vString3 = r.string()
        val vInt2 = r.int()
        val vString4 = r.string()
        return TraxSongInfoOut.List1Item(vInt1, vString1, vString2, vString3, vInt2, vString4)
    }

    private fun readTraxSongInfoOut(r: PacketReader): TraxSongInfoOut {
        val vList1 = List(r.countInt(16)) { readTraxSongInfoOutList1Item(r) }
        return TraxSongInfoOut(vList1)
    }

    private fun writeTraxSongInfoOutList1Item(w: PacketWriter, m: TraxSongInfoOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.string(m.string2)
        w.string(m.string3)
        w.int(m.int2)
        w.string(m.string4)
    }

    private fun writeTraxSongInfoOut(w: PacketWriter, m: TraxSongInfoOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { writeTraxSongInfoOutList1Item(w, it) }
    }
}
