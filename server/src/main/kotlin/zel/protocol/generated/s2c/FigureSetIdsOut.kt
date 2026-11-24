package zel.protocol.generated

data class FigureSetIdsOut(
    val list1: List<Int>,
    val list2: List<String>,
)

object FigureSetIdsOutCodec : OutgoingCodec<FigureSetIdsOut> {
    override val name = "FigureSetIdsOut"

    override fun read(r: PacketReader): FigureSetIdsOut = readFigureSetIdsOut(r)

    override fun write(w: PacketWriter, m: FigureSetIdsOut) = writeFigureSetIdsOut(w, m)

    private fun readFigureSetIdsOut(r: PacketReader): FigureSetIdsOut {
        val vList1 = List(r.countInt(4)) { r.int() }
        val vList2 = List(r.countInt(2)) { r.string() }
        return FigureSetIdsOut(vList1, vList2)
    }

    private fun writeFigureSetIdsOut(w: PacketWriter, m: FigureSetIdsOut) {
        w.countInt(m.list1.size)
        m.list1.forEach { w.int(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.string(it) }
    }
}
