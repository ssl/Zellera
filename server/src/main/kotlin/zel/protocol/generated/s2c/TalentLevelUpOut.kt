package zel.protocol.generated

data class TalentLevelUpOut(
    val string1: String,
    val int1: Int,
    val list1: List<String>,
    val list2: List<List2Item>,
) {
    data class List2Item(
        val string1: String,
        val int1: Int,
    )
}

object TalentLevelUpOutCodec : OutgoingCodec<TalentLevelUpOut> {
    override val name = "TalentLevelUpOut"

    override fun read(r: PacketReader): TalentLevelUpOut = readTalentLevelUpOut(r)

    override fun write(w: PacketWriter, m: TalentLevelUpOut) = writeTalentLevelUpOut(w, m)

    private fun readTalentLevelUpOutList2Item(r: PacketReader): TalentLevelUpOut.List2Item {
        val vString1 = r.string()
        val vInt1 = r.int()
        return TalentLevelUpOut.List2Item(vString1, vInt1)
    }

    private fun readTalentLevelUpOut(r: PacketReader): TalentLevelUpOut {
        val vString1 = r.string()
        val vInt1 = r.int()
        val vList1 = List(r.countInt(2)) { r.string() }
        val vList2 = List(r.countInt(6)) { readTalentLevelUpOutList2Item(r) }
        return TalentLevelUpOut(vString1, vInt1, vList1, vList2)
    }

    private fun writeTalentLevelUpOutList2Item(w: PacketWriter, m: TalentLevelUpOut.List2Item) {
        w.string(m.string1)
        w.int(m.int1)
    }

    private fun writeTalentLevelUpOut(w: PacketWriter, m: TalentLevelUpOut) {
        w.string(m.string1)
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { w.string(it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeTalentLevelUpOutList2Item(w, it) }
    }
}
