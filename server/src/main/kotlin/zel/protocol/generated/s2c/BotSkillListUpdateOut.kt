package zel.protocol.generated

data class BotSkillListUpdateOut(
    val int1: Int,
    val list1: List<List1Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
    )
}

object BotSkillListUpdateOutCodec : OutgoingCodec<BotSkillListUpdateOut> {
    override val name = "BotSkillListUpdateOut"

    override fun read(r: PacketReader): BotSkillListUpdateOut = readBotSkillListUpdateOut(r)

    override fun write(w: PacketWriter, m: BotSkillListUpdateOut) = writeBotSkillListUpdateOut(w, m)

    private fun readBotSkillListUpdateOutList1Item(r: PacketReader): BotSkillListUpdateOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        return BotSkillListUpdateOut.List1Item(vInt1, vString1)
    }

    private fun readBotSkillListUpdateOut(r: PacketReader): BotSkillListUpdateOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(6)) { readBotSkillListUpdateOutList1Item(r) }
        return BotSkillListUpdateOut(vInt1, vList1)
    }

    private fun writeBotSkillListUpdateOutList1Item(w: PacketWriter, m: BotSkillListUpdateOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
    }

    private fun writeBotSkillListUpdateOut(w: PacketWriter, m: BotSkillListUpdateOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeBotSkillListUpdateOutList1Item(w, it) }
    }
}
