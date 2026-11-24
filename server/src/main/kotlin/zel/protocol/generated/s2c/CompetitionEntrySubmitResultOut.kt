package zel.protocol.generated

data class CompetitionEntrySubmitResultOut(
    val int1: Int,
    val string1: String,
    val int2: Int,
    val list1: List<String>,
    val list2: List<String>,
)

object CompetitionEntrySubmitResultOutCodec : OutgoingCodec<CompetitionEntrySubmitResultOut> {
    override val name = "CompetitionEntrySubmitResultOut"

    override fun read(r: PacketReader): CompetitionEntrySubmitResultOut = readCompetitionEntrySubmitResultOut(r)

    override fun write(w: PacketWriter, m: CompetitionEntrySubmitResultOut) = writeCompetitionEntrySubmitResultOut(w, m)

    private fun readCompetitionEntrySubmitResultOut(r: PacketReader): CompetitionEntrySubmitResultOut {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vInt2 = r.int()
        val vList1 = List(r.countInt(2)) { r.string() }
        val vList2 = List(r.countInt(2)) { r.string() }
        return CompetitionEntrySubmitResultOut(vInt1, vString1, vInt2, vList1, vList2)
    }

    private fun writeCompetitionEntrySubmitResultOut(w: PacketWriter, m: CompetitionEntrySubmitResultOut) {
        w.int(m.int1)
        w.string(m.string1)
        w.int(m.int2)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { w.string(it) }
    }
}
