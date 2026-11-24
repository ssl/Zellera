package zel.protocol.generated

data class JukeboxSongDisksOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val int2: Int,
    )
}

object JukeboxSongDisksOutCodec : OutgoingCodec<JukeboxSongDisksOut> {
    override val name = "JukeboxSongDisksOut"

    override fun read(r: PacketReader): JukeboxSongDisksOut = readJukeboxSongDisksOut(r)

    override fun write(w: PacketWriter, m: JukeboxSongDisksOut) = writeJukeboxSongDisksOut(w, m)

    private fun readJukeboxSongDisksOutList1Item(r: PacketReader): JukeboxSongDisksOut.List1Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        return JukeboxSongDisksOut.List1Item(vInt1, vInt2)
    }

    private fun readJukeboxSongDisksOut(r: PacketReader): JukeboxSongDisksOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(8)) { readJukeboxSongDisksOutList1Item(r) }
        return JukeboxSongDisksOut(vInt1, vList1)
    }

    private fun writeJukeboxSongDisksOutList1Item(w: PacketWriter, m: JukeboxSongDisksOut.List1Item) {
        w.int(m.int1)
        w.int(m.int2)
    }

    private fun writeJukeboxSongDisksOut(w: PacketWriter, m: JukeboxSongDisksOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeJukeboxSongDisksOutList1Item(w, it) }
    }
}
