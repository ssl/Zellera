package zel.protocol.generated

data class GuildCreationInfoOut(
    val int1: Int,
    val list1: List<List1Item>,
    val list2: List<List2Item>,
) {
    data class List1Item(
        val int1: Int,
        val string1: String,
        val boolean1: Boolean,
    )

    data class List2Item(
        val int1: Int,
        val int2: Int,
        val int3: Int,
    )
}

object GuildCreationInfoOutCodec : OutgoingCodec<GuildCreationInfoOut> {
    override val name = "GuildCreationInfoOut"

    override fun read(r: PacketReader): GuildCreationInfoOut = readGuildCreationInfoOut(r)

    override fun write(w: PacketWriter, m: GuildCreationInfoOut) = writeGuildCreationInfoOut(w, m)

    private fun readGuildCreationInfoOutList1Item(r: PacketReader): GuildCreationInfoOut.List1Item {
        val vInt1 = r.int()
        val vString1 = r.string()
        val vBoolean1 = r.boolean()
        return GuildCreationInfoOut.List1Item(vInt1, vString1, vBoolean1)
    }

    private fun readGuildCreationInfoOutList2Item(r: PacketReader): GuildCreationInfoOut.List2Item {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return GuildCreationInfoOut.List2Item(vInt1, vInt2, vInt3)
    }

    private fun readGuildCreationInfoOut(r: PacketReader): GuildCreationInfoOut {
        val vInt1 = r.int()
        val vList1 = List(r.countInt(7)) { readGuildCreationInfoOutList1Item(r) }
        val vList2 = List(r.countInt(12)) { readGuildCreationInfoOutList2Item(r) }
        return GuildCreationInfoOut(vInt1, vList1, vList2)
    }

    private fun writeGuildCreationInfoOutList1Item(w: PacketWriter, m: GuildCreationInfoOut.List1Item) {
        w.int(m.int1)
        w.string(m.string1)
        w.boolean(m.boolean1)
    }

    private fun writeGuildCreationInfoOutList2Item(w: PacketWriter, m: GuildCreationInfoOut.List2Item) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }

    private fun writeGuildCreationInfoOut(w: PacketWriter, m: GuildCreationInfoOut) {
        w.int(m.int1)
        w.countInt(m.list1.size)
        m.list1.forEach { writeGuildCreationInfoOutList1Item(w, it) }
        w.countInt(m.list2.size)
        m.list2.forEach { writeGuildCreationInfoOutList2Item(w, it) }
    }
}
